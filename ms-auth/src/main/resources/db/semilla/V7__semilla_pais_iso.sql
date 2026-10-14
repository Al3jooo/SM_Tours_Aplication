-- Datos de prueba (solo perfiles dev y test). Decisión D5 (docs/trazabilidad.md): turista.pais_origen guarda el código
-- ISO 3166-1 alfa-2. V2 y V3 solo usan «Colombia». Va antes de V8, que agrega el CHECK de formato: si quedara un nombre,
-- V8 fallaría al arrancar.
UPDATE turista SET pais_origen = 'CO' WHERE pais_origen = 'Colombia';
