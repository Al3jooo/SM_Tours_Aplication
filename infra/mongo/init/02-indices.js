// Samaria360 - catalog_db: índices del caso de uso Buscar oferta turística.
// Los índices no forman parte del validador $jsonSchema (ver docs/sad/modelo-nosql/README.md).

const catalogo = db.getSiblingDB("catalog_db");

// RF23 (mapa) y RF38 (distancia): consultas geoespaciales sobre GeoJSON [longitud, latitud].
catalogo.servicios.createIndex({ coordenadas: "2dsphere" }, { name: "coordenadas_2dsphere" });
catalogo.atractivos.createIndex({ coordenadas: "2dsphere" }, { name: "coordenadas_2dsphere" });

// RF16: la búsqueda pública solo devuelve servicios Publicados de prestadores activos (RF22), filtrables por categoría.
catalogo.servicios.createIndex(
  { estadoPublicacion: 1, prestadorActivo: 1, categoria: 1 },
  { name: "busqueda_publica" }
);
