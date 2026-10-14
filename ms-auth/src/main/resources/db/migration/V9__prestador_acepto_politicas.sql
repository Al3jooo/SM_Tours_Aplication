-- Decisión D6 (docs/trazabilidad.md): WF-03 pide aceptar la Política de Privacidad (requisito derivado de E1 §3.2.7.4).
-- Las filas existentes quedan en false, que es lo cierto: nadie aceptó nada en un formulario. Sin DEFAULT: cada
-- inserción nueva debe enviar el valor. No interviene en el inicio de sesión.
ALTER TABLE prestador ADD COLUMN acepto_politicas BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE prestador ALTER COLUMN acepto_politicas DROP DEFAULT;
