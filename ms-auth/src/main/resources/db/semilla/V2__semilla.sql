-- Datos de prueba de auth_db. Se cargan solo con el perfil dev (spring.flyway.locations incluye db/semilla).
-- La numeración es compartida con db/migration: la próxima migración de esquema será V3.
--
-- Los 4 usuarios comparten una contraseña de prueba. Hashes BCrypt costo 10 (RNF05), generados con
-- BCryptPasswordEncoder(10) de spring-security-crypto y verificados con matches().

INSERT INTO rol (id_rol, nombre) VALUES
    (1, 'Turista'),
    (2, 'Prestador'),
    (3, 'Gestor'),
    (4, 'Administrador');

INSERT INTO usuario (id_usuario, id_rol, correo, contrasena_hash, fecha_registro,
                     intentos_fallidos, estado_cuenta, bloqueado_hasta) VALUES
    ('00000000-0000-4000-8000-000000000001', 1, 'turista@samaria360.test',
     '$2a$10$PPaplSsKK6CEavx41XytzOay3ih6dyFzwC4zr/TVjfchvcdXhNW0C', CURRENT_DATE, 0, 'Activo', NULL),
    ('00000000-0000-4000-8000-000000000002', 2, 'prestador@samaria360.test',
     '$2a$10$GUAyg8dMPH1UDSdLVgU7Du85Woc.Ig.5aHCYRqy8eVs5q6brv9eMS', CURRENT_DATE, 0, 'Activo', NULL),
    ('00000000-0000-4000-8000-000000000003', 3, 'gestor@samaria360.test',
     '$2a$10$DT0fle.VHZGmFgH/fjFo9uZsJ6HWUfNg.EPcztOskAa8dm4HZGT3e', CURRENT_DATE, 0, 'Activo', NULL),
    ('00000000-0000-4000-8000-000000000004', 4, 'admin@samaria360.test',
     '$2a$10$PGEd8o4TvXaBoSwpR8.RROxBGJsWNAZOliSHg.HNv8kuqLErFQpiG', CURRENT_DATE, 0, 'Activo', NULL);

INSERT INTO turista (id_usuario, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido,
                     pais_origen, acepto_politicas) VALUES
    ('00000000-0000-4000-8000-000000000001', 'Ana', NULL, 'Pérez', NULL, 'Colombia', TRUE);

-- RF01: tres categorías de interés turístico.
INSERT INTO turista_categoria_interes (id_usuario, categoria) VALUES
    ('00000000-0000-4000-8000-000000000001', 'Naturaleza'),
    ('00000000-0000-4000-8000-000000000001', 'Cultura'),
    ('00000000-0000-4000-8000-000000000001', 'Aventura');

-- Prestador pendiente de validación (RF02): puede iniciar sesión porque su estado_cuenta es Activo;
-- RF09 solo le impide registrar servicios.
INSERT INTO prestador (id_usuario, razon_social, rnt, telefono, nombre_representante_legal,
                       correo_corporativo, estado_validacion) VALUES
    ('00000000-0000-4000-8000-000000000002', 'Tours Sierra Nevada S.A.S.', 'RNT-00000', '+57 3000000000',
     'Carlos Gómez', 'contacto@tours-sierra.test', 'Pendiente');

INSERT INTO gestor (id_usuario) VALUES ('00000000-0000-4000-8000-000000000003');

INSERT INTO administrador (id_usuario) VALUES ('00000000-0000-4000-8000-000000000004');
