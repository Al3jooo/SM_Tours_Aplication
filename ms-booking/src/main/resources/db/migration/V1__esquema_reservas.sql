-- Samaria360 - reservas_db (ms-booking). Esquema del CU «Reservar servicio» (RF11, RF18, RF20, RF32, RF37, RF44).
-- Referencias lógicas, sin FK física (otras bases): id_turista e id_prestador → auth_db; id_servicio_mongo e
-- id_unidad → catalog_db. Las FK de reserva_cupo_dia sí son físicas: misma base.

-- Cupos por día (D2): una noche de una unidad de alojamiento (D1) o un horario de un tour. Es la fuente de verdad de
-- lo que queda: solo la cambian reservar y cancelar, con UPDATE condicionales (D3).
CREATE TABLE inventario_cupo (
    id_inventario     bigint GENERATED ALWAYS AS IDENTITY,
    id_servicio_mongo varchar(24) NOT NULL,
    categoria         varchar(20) NOT NULL,
    id_unidad         varchar(24),
    fecha             date        NOT NULL,
    hora_inicio       time,
    cupos_totales     integer     NOT NULL,
    cupos_disponibles integer     NOT NULL,
    CONSTRAINT pk_inventario_cupo PRIMARY KEY (id_inventario),
    CONSTRAINT ck_inventario_cupo_categoria CHECK (categoria IN ('Tour', 'Alojamiento')),
    -- Alojamiento: por unidad y noche, sin hora. Tour: por horario, sin unidad.
    CONSTRAINT ck_inventario_cupo_forma CHECK (
        (categoria = 'Alojamiento' AND id_unidad IS NOT NULL AND hora_inicio IS NULL)
        OR (categoria = 'Tour' AND id_unidad IS NULL AND hora_inicio IS NOT NULL)),
    CONSTRAINT ck_inventario_cupo_totales CHECK (cupos_totales > 0),
    -- Nunca negativos (no se vende de más) ni por encima del total (no se libera dos veces).
    CONSTRAINT ck_inventario_cupo_disponibles CHECK (cupos_disponibles BETWEEN 0 AND cupos_totales),
    -- Una fila por servicio, unidad, fecha y hora; los nulos cuentan como iguales (PostgreSQL 15+).
    CONSTRAINT uq_inventario_cupo_dia UNIQUE NULLS NOT DISTINCT (id_servicio_mongo, id_unidad, fecha, hora_inicio)
);

-- Reserva confirmada automáticamente (RF20). Guarda lo que el turista vio al reservar (RF32): precio, total, nombre
-- del servicio y de la unidad, aunque el catálogo cambie después.
CREATE TABLE reserva (
    id_reserva          uuid          NOT NULL,
    id_turista          uuid          NOT NULL,
    id_prestador        uuid          NOT NULL,
    id_servicio_mongo   varchar(24)   NOT NULL,
    nombre_servicio     varchar(150)  NOT NULL,
    categoria           varchar(20)   NOT NULL,
    id_unidad           varchar(24),
    nombre_unidad       varchar(100),
    estado              varchar(20)   NOT NULL,
    cantidad_solicitada integer       NOT NULL,
    personas            integer       NOT NULL,
    precio_unitario     numeric(10,2) NOT NULL,
    unidad_cobro        varchar(20)   NOT NULL,
    monto_total         numeric(12,2) NOT NULL,
    moneda              char(3)       NOT NULL,
    fecha_entrada       date          NOT NULL,
    fecha_salida        date,
    hora_inicio         time,
    fecha_inicio        timestamptz   NOT NULL,
    fecha_fin           timestamptz   NOT NULL,
    fecha_creacion      timestamptz   NOT NULL,
    fecha_cancelacion   timestamptz,
    clave_idempotencia  uuid          NOT NULL,
    hash_solicitud      char(64)      NOT NULL,
    CONSTRAINT pk_reserva PRIMARY KEY (id_reserva),
    CONSTRAINT ck_reserva_estado CHECK (estado IN ('Confirmada', 'Cancelada', 'Completada')),
    CONSTRAINT ck_reserva_categoria CHECK (categoria IN ('Tour', 'Alojamiento')),
    CONSTRAINT ck_reserva_unidad_cobro CHECK (unidad_cobro IN ('Noche', 'Persona')),
    CONSTRAINT ck_reserva_cantidades CHECK (cantidad_solicitada > 0 AND personas > 0),
    CONSTRAINT ck_reserva_montos CHECK (precio_unitario > 0 AND monto_total > 0),
    CONSTRAINT ck_reserva_fechas CHECK (fecha_fin > fecha_inicio),
    CONSTRAINT ck_reserva_cancelacion CHECK ((estado = 'Cancelada') = (fecha_cancelacion IS NOT NULL)),
    -- Alojamiento: una unidad, una salida y cobro por noche. Tour: un horario y cobro por persona.
    CONSTRAINT ck_reserva_forma CHECK (
        (categoria = 'Alojamiento' AND id_unidad IS NOT NULL AND nombre_unidad IS NOT NULL
            AND fecha_salida IS NOT NULL AND hora_inicio IS NULL AND unidad_cobro = 'Noche')
        OR (categoria = 'Tour' AND id_unidad IS NULL AND nombre_unidad IS NULL
            AND fecha_salida IS NULL AND hora_inicio IS NOT NULL AND unidad_cobro = 'Persona')),
    -- Idempotency-Key por turista (D6): la segunda solicitud con la misma clave choca aquí.
    CONSTRAINT uq_reserva_turista_clave UNIQUE (id_turista, clave_idempotencia)
);

-- «Mis reservas», de la más reciente a la más antigua.
CREATE INDEX ix_reserva_turista_creacion ON reserva (id_turista, fecha_creacion DESC);
-- Reservas de un prestador por fecha (RF42, más adelante).
CREATE INDEX ix_reserva_prestador_entrada ON reserva (id_prestador, fecha_entrada);

-- Lo que cada reserva descontó de cada fila de inventario: se devuelve igual al cancelar.
CREATE TABLE reserva_cupo_dia (
    id_reserva          uuid    NOT NULL,
    id_inventario       bigint  NOT NULL,
    cantidad_descontada integer NOT NULL,
    CONSTRAINT pk_reserva_cupo_dia PRIMARY KEY (id_reserva, id_inventario),
    CONSTRAINT fk_reserva_cupo_dia_reserva FOREIGN KEY (id_reserva) REFERENCES reserva (id_reserva),
    CONSTRAINT fk_reserva_cupo_dia_inventario FOREIGN KEY (id_inventario) REFERENCES inventario_cupo (id_inventario),
    CONSTRAINT ck_reserva_cupo_dia_cantidad CHECK (cantidad_descontada > 0)
);

CREATE INDEX ix_reserva_cupo_dia_inventario ON reserva_cupo_dia (id_inventario);
