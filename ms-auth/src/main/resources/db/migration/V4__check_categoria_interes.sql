-- RF01 y RF53 (decisión D1, docs/trazabilidad.md): vocabulario cerrado de categorías de interés turístico.
-- Igual que el CHECK de prestador.estado_validacion: el dominio (CategoriaInteres) y la base aceptan los mismos valores.
ALTER TABLE turista_categoria_interes
    ADD CONSTRAINT ck_turista_categoria_interes_categoria
        CHECK (categoria IN ('Naturaleza', 'Cultura', 'Aventura'));
