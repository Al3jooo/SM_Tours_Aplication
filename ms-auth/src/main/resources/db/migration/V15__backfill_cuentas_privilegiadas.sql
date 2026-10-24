-- «Cambiar rol de cuenta» (RF45, DEC-82, DEC-108): cierre de L3. ms-catalog y ms-booking responden 401 a un token de
-- Gestor o Administrador cuya cuenta no tiene fila en su proyección «cuentas» (DEC-110), así que cada cuenta de esos
-- roles necesita un evento CuentaActualizada. Se encola uno por cuenta, con sus dos entregas, como lo haría el outbox.
--
-- La versión sube en 1: los consumidores exigen version >= 1 y las cuentas parten de 0 (V12). Así la versión proyectada
-- siempre existe en auth_db. Efecto aceptado: un token de Gestor o Administrador anterior a esta migración recibe 401
-- SESION_INVALIDADA una vez (vence a los 15 minutos de todos modos).
--
-- Se selecciona por el nombre del rol, no por datos de la semilla: sin personal no inserta nada. Flyway la aplica una
-- sola vez y el consumidor es idempotente por versión.
WITH privilegiadas AS (
    UPDATE usuario u
    SET version_cuenta = u.version_cuenta + 1
    FROM rol r
    WHERE r.id_rol = u.id_rol AND r.nombre IN ('Gestor', 'Administrador')
    RETURNING u.id_usuario, r.nombre AS rol, u.estado_cuenta::text AS estado, u.version_cuenta
), eventos AS (
    INSERT INTO outbox_evento (tipo, id_usuario, carga, creado_en)
    SELECT 'CuentaActualizada', p.id_usuario,
           jsonb_build_object('idUsuario', p.id_usuario::text, 'rol', p.rol, 'estadoCuenta', p.estado,
                              'version', p.version_cuenta),
           now()
    FROM privilegiadas p
    RETURNING id_evento
)
INSERT INTO outbox_entrega (id_evento, destino, proximo_intento)
SELECT e.id_evento, d.destino, now()
FROM eventos e
CROSS JOIN (VALUES ('ms-catalog'), ('ms-booking')) AS d (destino);
