-- RF02 y WF-03 no piden razón social (decisión D2, docs/trazabilidad.md): pasa a ser opcional.
ALTER TABLE prestador ALTER COLUMN razon_social DROP NOT NULL;
