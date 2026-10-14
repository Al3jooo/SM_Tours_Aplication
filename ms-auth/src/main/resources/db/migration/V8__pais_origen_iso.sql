-- Decisión D5 (docs/trazabilidad.md): RF01 «país de origen» se guarda como código ISO 3166-1 alfa-2 (p. ej. «CO»).
-- VARCHAR(2) y no CHAR(2): CHAR rellena con espacios. La lista de códigos válidos vive en el dominio (CodigoPais);
-- aquí solo se asegura el formato. Con la semilla, V7 ya convirtió los nombres en códigos.
ALTER TABLE turista ALTER COLUMN pais_origen TYPE VARCHAR(2);
ALTER TABLE turista
    ADD CONSTRAINT ck_turista_pais_origen CHECK (pais_origen ~ '^[A-Z]{2}$');
