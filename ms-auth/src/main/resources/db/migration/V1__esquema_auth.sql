-- Esquema de auth_db según el modelo lógico del SAD (docs/sad/diagramas/modelo-logico.dbml).
-- Solo tablas de ms-auth: Reserva, Inventario_Cupo y Reserva_Cupo_Dia pertenecen a reservas_db.

CREATE TYPE estado_cuenta AS ENUM ('Activo', 'Inactivo', 'Desactivado');

CREATE TABLE rol (
    id_rol INTEGER     NOT NULL,
    nombre VARCHAR(50) NOT NULL,
    CONSTRAINT pk_rol PRIMARY KEY (id_rol),
    CONSTRAINT uq_rol_nombre UNIQUE (nombre)
);

CREATE TABLE usuario (
    id_usuario        UUID          NOT NULL,
    id_rol            INTEGER       NOT NULL,
    correo            VARCHAR(100)  NOT NULL,
    contrasena_hash   VARCHAR(255)  NOT NULL,
    fecha_registro    DATE          NOT NULL,
    intentos_fallidos INTEGER       NOT NULL,
    estado_cuenta     estado_cuenta NOT NULL,
    bloqueado_hasta   TIMESTAMPTZ   NULL,
    CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),
    CONSTRAINT uq_usuario_correo UNIQUE (correo),
    CONSTRAINT fk_usuario_rol FOREIGN KEY (id_rol) REFERENCES rol (id_rol)
);

-- Subtipos de Usuario: relación 1:1 (PK = FK)

CREATE TABLE turista (
    id_usuario       UUID        NOT NULL,
    primer_nombre    VARCHAR(50) NOT NULL,
    segundo_nombre   VARCHAR(50) NULL,
    primer_apellido  VARCHAR(50) NOT NULL,
    segundo_apellido VARCHAR(50) NULL,
    pais_origen      VARCHAR(50) NOT NULL,
    acepto_politicas BOOLEAN     NOT NULL,
    CONSTRAINT pk_turista PRIMARY KEY (id_usuario),
    CONSTRAINT fk_turista_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);

CREATE TABLE turista_categoria_interes (
    id_usuario UUID        NOT NULL,
    categoria  VARCHAR(50) NOT NULL,
    CONSTRAINT pk_turista_categoria_interes PRIMARY KEY (id_usuario, categoria),
    CONSTRAINT fk_turista_categoria_interes_turista FOREIGN KEY (id_usuario) REFERENCES turista (id_usuario)
);

CREATE TABLE prestador (
    id_usuario                 UUID         NOT NULL,
    razon_social               VARCHAR(150) NOT NULL,
    rnt                        VARCHAR(50)  NOT NULL,
    telefono                   VARCHAR(20)  NOT NULL,
    nombre_representante_legal VARCHAR(150) NOT NULL,
    correo_corporativo         VARCHAR(100) NOT NULL,
    -- 'Pendiente' = "Pendiente de Validación" (RF02); 'Activo' y 'Rechazado' (RF28).
    -- Independiente de usuario.estado_cuenta: el login no lo consulta (RF09 lo usa para registrar servicios).
    estado_validacion          VARCHAR(50)  NOT NULL,
    CONSTRAINT pk_prestador PRIMARY KEY (id_usuario),
    CONSTRAINT uq_prestador_rnt UNIQUE (rnt),
    CONSTRAINT ck_prestador_estado_validacion
        CHECK (estado_validacion IN ('Pendiente', 'Activo', 'Rechazado')),
    CONSTRAINT fk_prestador_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);

CREATE TABLE gestor (
    id_usuario UUID NOT NULL,
    CONSTRAINT pk_gestor PRIMARY KEY (id_usuario),
    CONSTRAINT fk_gestor_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);

CREATE TABLE administrador (
    id_usuario UUID NOT NULL,
    CONSTRAINT pk_administrador PRIMARY KEY (id_usuario),
    CONSTRAINT fk_administrador_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);

-- Recuperación de contraseña (RF34): solo se guarda el SHA-256 del token, nunca el token en claro.
CREATE TABLE token_recuperacion (
    id_token_recuperacion BIGINT GENERATED ALWAYS AS IDENTITY,
    id_usuario            UUID        NOT NULL,
    token_hash            VARCHAR(64) NOT NULL,
    expira_en             TIMESTAMPTZ NOT NULL,
    usado                 BOOLEAN     NOT NULL,
    CONSTRAINT pk_token_recuperacion PRIMARY KEY (id_token_recuperacion),
    CONSTRAINT uq_token_recuperacion_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_token_recuperacion_usuario FOREIGN KEY (id_usuario) REFERENCES usuario (id_usuario)
);

-- Diseño físico: PostgreSQL no indexa automáticamente las FK.
CREATE INDEX idx_token_recuperacion_usuario ON token_recuperacion (id_usuario);
