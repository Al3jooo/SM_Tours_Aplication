-- «Administrar cuentas» (ADR-008, DEC-26): proyección del estado de las cuentas de ms-auth, para que ms-booking rechace
-- con 401 SESION_INVALIDADA un token con «ver» menor que la versión conocida (DEC-29). Cubre todos los roles.
-- Referencia lógica, sin FK física: id_usuario → usuario.id_usuario de auth_db. La llena el endpoint interno
-- PUT /internal/v1/cuentas/{id}, con un upsert que solo avanza si la versión es mayor (idempotente, tolera desorden).
-- Una cuenta sin fila se acepta: modo permisivo hasta el exp del token. Reservar, consultar, cancelar y la
-- disponibilidad no cambian de lógica ni consultan esta tabla; solo el validador del JWT lo hace.
CREATE TABLE cuenta (
    id_usuario     uuid        NOT NULL,
    rol            varchar(20) NOT NULL,
    -- Estado derivado que publica ms-auth: en un Prestador reúne estado_validacion y estado_cuenta.
    estado         varchar(20) NOT NULL,
    version        bigint      NOT NULL,
    actualizado_en timestamptz NOT NULL,
    CONSTRAINT pk_cuenta PRIMARY KEY (id_usuario),
    CONSTRAINT ck_cuenta_rol CHECK (rol IN ('Turista', 'Prestador', 'Gestor', 'Administrador')),
    CONSTRAINT ck_cuenta_estado CHECK (estado IN ('Pendiente', 'Activo', 'Rechazado', 'Inactivo', 'Desactivado')),
    CONSTRAINT ck_cuenta_version CHECK (version >= 1)
);
