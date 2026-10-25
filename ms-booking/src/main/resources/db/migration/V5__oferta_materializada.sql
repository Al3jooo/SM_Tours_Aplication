-- Iteración C, sesión A2 (DEC-135, DEC-154): hasta qué fecha materializó ms-booking la oferta de cada servicio en
-- inventario_cupo. Distingue un día que no opera (fecha <= materializada_hasta y sin filas) de uno que falta
-- materializar (fecha > materializada_hasta). Solo avanza (UPSERT con GREATEST); nunca se borra ni retrocede.
-- Referencia lógica, sin FK física: id_servicio_mongo → _id de servicios en catalog_db (mismo tipo que en
-- inventario_cupo). Ni reservar ni cancelar la bloquean: se escribe fuera de sus transacciones (DEC-158).
CREATE TABLE oferta_materializada (
    id_servicio_mongo   varchar(24) NOT NULL,
    materializada_hasta date        NOT NULL,
    fecha_actualizacion timestamptz NOT NULL,
    CONSTRAINT pk_oferta_materializada PRIMARY KEY (id_servicio_mongo)
);
