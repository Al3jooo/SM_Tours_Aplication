-- «Administrar cuentas» (RF28, DEC-42): revisión del prestador por el Administrador. El rechazo exige una justificación
-- (RF28) de hasta 500 caracteres; revisado_en y revisado_por dejan quién revisó y cuándo (misma base: FK física).
ALTER TABLE prestador
    ADD COLUMN motivo_rechazo varchar(500) NULL,
    ADD COLUMN revisado_en    timestamptz  NULL,
    ADD COLUMN revisado_por   uuid         NULL,
    ADD CONSTRAINT fk_prestador_revisado_por FOREIGN KEY (revisado_por) REFERENCES administrador (id_usuario);

-- Antes del CHECK: un prestador Rechazado anterior a RF28 (p. ej. de pruebas manuales en una base de desarrollo) no
-- tiene justificación y haría fallar la migración. En una base sin filas Rechazadas esta sentencia no cambia nada.
UPDATE prestador
SET motivo_rechazo = 'Rechazo anterior a RF28, sin justificación registrada'
WHERE estado_validacion = 'Rechazado' AND motivo_rechazo IS NULL;

ALTER TABLE prestador
    ADD CONSTRAINT ck_prestador_rechazo_con_motivo
        CHECK (estado_validacion <> 'Rechazado' OR motivo_rechazo IS NOT NULL);
