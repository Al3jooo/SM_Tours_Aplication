// Samaria360 - catalog_db (ms-catalog). Se ejecuta una sola vez, con el volumen de MongoDB vacío
// (docker-entrypoint-initdb.d), en orden alfabético: 01 validadores, 02 índices, 03 semilla.
//
// LÍNEA BASE CONGELADA: 01, 02 y 03 no se vuelven a editar. Crean el formato v1 del catálogo (el del Documento
// Entregable 2) y después el servicio mongo-migraciones aplica las migraciones de infra/mongo/migraciones, igual en
// un volumen nuevo que en uno existente. Cualquier cambio de esquema o de datos va en una migración nueva, con su
// propia copia del validador.
//
// Los validadores v1 están en servicios.v1.schema.json y atractivos.v1.schema.json, junto a este script (el
// entrypoint de MongoDB solo ejecuta .sh y .js, así que ignora los .json). La prueba ScriptsInicializacionMongoTest
// verifica que la base quede con esos validadores.
//
// Solo se crean las colecciones del caso de uso Buscar oferta turística; las demás colecciones del
// Entregable 2 se crearán cuando un caso de uso las necesite.

const fs = require("fs");
const path = require("path");

const catalogo = db.getSiblingDB("catalog_db");

function validador(archivo) {
  return JSON.parse(fs.readFileSync(path.join(__dirname, archivo), "utf8"));
}

catalogo.createCollection("servicios", {
  validator: validador("servicios.v1.schema.json"),
  validationLevel: "strict",
  validationAction: "error"
});

catalogo.createCollection("atractivos", {
  validator: validador("atractivos.v1.schema.json"),
  validationLevel: "strict",
  validationAction: "error"
});
