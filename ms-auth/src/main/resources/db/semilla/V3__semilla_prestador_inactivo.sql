-- Datos de prueba de auth_db (solo perfiles dev y test). La numeración es compartida con db/migration:
-- la próxima migración de esquema será V4.
--
-- Segundo prestador, con la cuenta Inactiva (RF22): permite probar el 403 CUENTA_INACTIVA sin preparar datos a mano,
-- y respalda el servicio «Snorkel en Bahía Concha» de la semilla de ms-catalog (prestadorActivo: false).
-- Su validación está Activa: lo que lo bloquea es estado_cuenta, no estado_validacion.
-- Contraseña de prueba: la misma de V2 (BCrypt costo 10, generado con BCryptPasswordEncoder(10) y verificado con matches()).

INSERT INTO usuario (id_usuario, id_rol, correo, contrasena_hash, fecha_registro,
                     intentos_fallidos, estado_cuenta, bloqueado_hasta) VALUES
    ('00000000-0000-4000-8000-000000000005', 2, 'prestador2@samaria360.test',
     '$2a$10$.6TxnjDt8.L3KAPI2gwe0e88KBv7C4Whpds0wusUisiJV3O0MDPhq', CURRENT_DATE, 0, 'Inactivo', NULL);

INSERT INTO prestador (id_usuario, razon_social, rnt, telefono, nombre_representante_legal,
                       correo_corporativo, estado_validacion) VALUES
    ('00000000-0000-4000-8000-000000000005', 'Snorkel Bahía Concha S.A.S.', 'RNT-00001', '+57 3000000001',
     'Laura Martínez', 'contacto@snorkelbahiaconcha.test', 'Activo');
