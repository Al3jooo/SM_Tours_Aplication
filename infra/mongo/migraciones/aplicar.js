// Samaria360 - catalog_db: aplica las migraciones de MongoDB en orden de número, una sola vez cada una.
//
// - esquema/: cambios de esquema y de datos de referencia. Se aplican siempre.
// - semilla/: datos de demostración. Solo con MONGO_SEMILLA=true (Docker Compose, demo).
// La numeración (M001, M002…) es compartida entre las dos carpetas, como en Flyway.
//
// Cada migración aplicada queda en la colección «migraciones» ({_id: nombre, aplicadaEn}) y no se vuelve a
// ejecutar. Además, cada migración es idempotente por sí misma. Nunca se edita una migración ya aplicada: un cambio
// va en una migración nueva, con su propia copia del validador si toca el esquema.
//
// Cualquier error termina mongosh con código distinto de 0: el servicio mongo-migraciones falla y ms-catalog no
// arranca (depends_on: service_completed_successfully).

const fs = require("fs");
const path = require("path");

const catalogo = db.getSiblingDB("catalog_db");
const conSemilla = process.env.MONGO_SEMILLA === "true";
const carpetas = conSemilla ? ["esquema", "semilla"] : ["esquema"];

const migraciones = carpetas
  .flatMap((carpeta) => fs.readdirSync(path.join(__dirname, carpeta))
    .filter((archivo) => /^M\d{3}__[a-z0-9-]+\.js$/.test(archivo))
    .map((archivo) => ({ nombre: archivo.slice(0, -3), ruta: path.join(__dirname, carpeta, archivo) })))
  .sort((a, b) => a.nombre.localeCompare(b.nombre));

const numeros = migraciones.map((migracion) => migracion.nombre.slice(0, 4));
const repetido = numeros.find((numero, i) => numeros.indexOf(numero) !== i);
if (repetido) {
  throw new Error("Número de migración repetido: " + repetido);
}

print("Migraciones de catalog_db (semilla: " + (conSemilla ? "sí" : "no") + ")");
for (const migracion of migraciones) {
  if (catalogo.migraciones.findOne({ _id: migracion.nombre }) !== null) {
    print("  ya aplicada: " + migracion.nombre);
    continue;
  }
  print("  aplicando:   " + migracion.nombre);
  load(migracion.ruta);
  catalogo.migraciones.insertOne({ _id: migracion.nombre, aplicadaEn: new Date() });
}
print("Migraciones al día.");
