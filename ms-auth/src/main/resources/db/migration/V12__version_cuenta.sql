-- DEC-29 (ADR-008): versión de la cuenta. Sube en 1 con cada cambio de estado_cuenta, de estado_validacion o de rol.
-- El JWT lleva el valor en el claim «ver»; ms-catalog y ms-booking rechazan un token con una versión menor que la
-- conocida. Las cuentas existentes parten de 0, que es también lo que vale un token sin el claim.
ALTER TABLE usuario
    ADD COLUMN version_cuenta bigint NOT NULL DEFAULT 0,
    ADD CONSTRAINT ck_usuario_version_cuenta CHECK (version_cuenta >= 0);
