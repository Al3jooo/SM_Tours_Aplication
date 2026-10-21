-- Datos de prueba (solo perfiles dev y test) para la demostración de «Administrar cuentas» (WF-12; RF28, RF22, RF62).
-- La numeración es compartida con db/migration: la próxima migración de esquema será V15.
--
-- Dos prestadores Pendientes de Validación y un turista más. Comparten la contraseña de prueba de V2: se reutiliza el
-- mismo hash BCrypt (costo 10), sin escribir la contraseña aquí.

INSERT INTO usuario (id_usuario, id_rol, correo, contrasena_hash, fecha_registro,
                     intentos_fallidos, estado_cuenta, bloqueado_hasta)
SELECT v.id, v.id_rol, v.correo, base.contrasena_hash, v.fecha, 0, 'Activo', NULL
FROM (VALUES
    ('00000000-0000-4000-8000-000000000006'::uuid, 2, 'prestador3@samaria360.test', CURRENT_DATE - 2),
    ('00000000-0000-4000-8000-000000000007'::uuid, 2, 'prestador4@samaria360.test', CURRENT_DATE - 1),
    ('00000000-0000-4000-8000-000000000008'::uuid, 1, 'turista2@samaria360.test',   CURRENT_DATE)
) AS v(id, id_rol, correo, fecha)
CROSS JOIN (SELECT contrasena_hash FROM usuario
            WHERE id_usuario = '00000000-0000-4000-8000-000000000001') AS base;

INSERT INTO prestador (id_usuario, razon_social, rnt, telefono, nombre_representante_legal,
                       correo_corporativo, estado_validacion, acepto_politicas) VALUES
    ('00000000-0000-4000-8000-000000000006', 'Kayak Taganga S.A.S.', 'RNT-00002', '+57 3000000002',
     'Marta Rojas', 'contacto@kayaktaganga.test', 'Pendiente', TRUE),
    ('00000000-0000-4000-8000-000000000007', NULL, 'RNT-00003', '+57 3000000003',
     'Pedro Salcedo', 'contacto@hostalrodadero.test', 'Pendiente', TRUE);

INSERT INTO turista (id_usuario, primer_nombre, segundo_nombre, primer_apellido, segundo_apellido,
                     pais_origen, acepto_politicas) VALUES
    ('00000000-0000-4000-8000-000000000008', 'Marta', NULL, 'Lee', NULL, 'US', TRUE);

-- Un turista desactivado por RF62 se guarda como «Desactivado» (DEC-44). Si una base de desarrollo conserva un turista
-- «Inactivo» de antes de esta iteración, pasa a «Desactivado». V2 no se edita.
UPDATE usuario SET estado_cuenta = 'Desactivado' WHERE id_rol = 1 AND estado_cuenta = 'Inactivo';
