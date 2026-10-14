-- Datos de prueba (solo perfiles dev y test). No se editan V2 ni V3: ya están aplicadas y cambiarlas rompería el
-- checksum de Flyway.
--
-- Decisión D3 (docs/trazabilidad.md): un prestador Pendiente o Rechazado ya no puede iniciar sesión. prestador@ tiene
-- servicios publicados en la semilla de ms-catalog, y según RF09 y RF21 eso solo es posible con la validación Activa.
UPDATE prestador
SET estado_validacion = 'Activo'
WHERE id_usuario = '00000000-0000-4000-8000-000000000002';
