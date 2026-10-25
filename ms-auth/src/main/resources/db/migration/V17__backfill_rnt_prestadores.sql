-- «Registrar y aprobar servicio» (DEC-126, DEC-146, DEC-147): el evento CuentaActualizada de un prestador lleva su RNT, y
-- ms-catalog lo guarda en su proyección «cuentas» para copiarlo en los servicios que el prestador registre (HC2). Además,
-- registrar un servicio exige la fila del prestador en esa proyección con estado Activo (DEC-125). Hasta ahora ningún
-- prestador anterior tenía un evento con RNT (los de la semilla no tenían ninguno): se encola uno por prestador, en
-- cualquier estado, con sus dos entregas, como lo haría el outbox.
--
-- La versión sube en 1, como en V15: los consumidores ignoran una versión igual o menor a la proyectada, así que sin
-- subirla un prestador ya proyectado nunca recibiría su RNT. Efecto aceptado: un token de prestador anterior a esta
-- migración recibe 401 SESION_INVALIDADA una vez (vence a los 15 minutos de todos modos).
--
-- El estado es el derivado de EstadoAdministrativo.dePrestador (DEC-43), el mismo CASE de CuentasAdminJdbcAdapter:
-- Pendiente > Rechazado > Activo (con la cuenta Activa) o Inactivo. Se selecciona por el nombre del rol: sin prestadores
-- no inserta nada. Flyway la aplica una sola vez y el consumidor es idempotente por versión.
WITH prestadores AS (
    UPDATE usuario u
    SET version_cuenta = u.version_cuenta + 1
    FROM rol r, prestador p
    WHERE r.id_rol = u.id_rol AND r.nombre = 'Prestador' AND p.id_usuario = u.id_usuario
    RETURNING u.id_usuario, u.version_cuenta, p.rnt,
              CASE WHEN p.estado_validacion = 'Pendiente' THEN 'Pendiente'
                   WHEN p.estado_validacion = 'Rechazado' THEN 'Rechazado'
                   WHEN u.estado_cuenta = 'Activo' THEN 'Activo'
                   ELSE 'Inactivo' END AS estado
), eventos AS (
    INSERT INTO outbox_evento (tipo, id_usuario, carga, creado_en)
    SELECT 'CuentaActualizada', p.id_usuario,
           jsonb_build_object('idUsuario', p.id_usuario::text, 'rol', 'Prestador', 'estadoCuenta', p.estado,
                              'version', p.version_cuenta, 'rnt', p.rnt),
           now()
    FROM prestadores p
    RETURNING id_evento
)
INSERT INTO outbox_entrega (id_evento, destino, proximo_intento)
SELECT e.id_evento, d.destino, now()
FROM eventos e
CROSS JOIN (VALUES ('ms-catalog'), ('ms-booking')) AS d (destino);
