-- DEC-26 (ADR-008): outbox transaccional. El evento CuentaActualizada se inserta en la misma transacción que el cambio
-- de estado; el relevo lo entrega por HTTP a cada destino, que lleva su propio registro de entrega.
CREATE TABLE outbox_evento (
    id_evento  bigint GENERATED ALWAYS AS IDENTITY,
    tipo       varchar(50) NOT NULL,
    id_usuario uuid        NOT NULL,
    carga      jsonb       NOT NULL,
    creado_en  timestamptz NOT NULL,
    CONSTRAINT pk_outbox_evento PRIMARY KEY (id_evento),
    CONSTRAINT ck_outbox_evento_tipo CHECK (tipo IN ('CuentaActualizada')),
    CONSTRAINT fk_outbox_evento_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);

CREATE TABLE outbox_entrega (
    id_evento       bigint       NOT NULL,
    destino         varchar(20)  NOT NULL,
    entregado_en    timestamptz  NULL,
    intentos        integer      NOT NULL DEFAULT 0,
    proximo_intento timestamptz  NOT NULL,
    ultimo_error    varchar(500) NULL,
    CONSTRAINT pk_outbox_entrega PRIMARY KEY (id_evento, destino),
    CONSTRAINT ck_outbox_entrega_destino CHECK (destino IN ('ms-catalog', 'ms-booking')),
    CONSTRAINT ck_outbox_entrega_intentos CHECK (intentos >= 0),
    CONSTRAINT fk_outbox_entrega_evento FOREIGN KEY (id_evento) REFERENCES outbox_evento (id_evento)
);

-- El relevo solo busca entregas pendientes de un destino.
CREATE INDEX ix_outbox_entrega_pendiente ON outbox_entrega (destino, proximo_intento) WHERE entregado_en IS NULL;
-- Y, por usuario, el último evento pendiente.
CREATE INDEX ix_outbox_evento_usuario ON outbox_evento (id_usuario, id_evento DESC);
