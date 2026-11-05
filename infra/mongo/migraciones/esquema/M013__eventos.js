// M013 - Colección «eventos» (RF25; iteración E, sesión A2; DEC-215, DEC-235 a DEC-242), en catalog_db.
// Esquema del Documento Entregable 2 (§2.4), más estricto: ubicacion y descripcion obligatorias, fechaRegistro
// (ampliación), límites de longitud, additionalProperties: false y fechaInicio <= fechaFin ($expr).
// Índices:
//   eventos_agenda: la agenda pública, por fechaInicio ascendente con el rango de fechaFin (DEC-215);
//   eventos_registro: el panel del Gestor, del más reciente al más antiguo (DEC-238).
// INMUTABLE: usa su propia copia del validador (M013__eventos.v1.schema.json), nunca docs/. Idempotente: crea la
// colección si no existe y, si ya existe, solo vuelve a fijar el validador; createIndex con el mismo nombre y las mismas
// claves no hace nada. Sin semilla.
(function () {
  const fs = require("fs");
  const path = require("path");
  const catalogo = db.getSiblingDB("catalog_db");
  const validador = JSON.parse(fs.readFileSync(path.join(__dirname, "M013__eventos.v1.schema.json"), "utf8"));
  const existe = catalogo.getCollectionInfos({ name: "eventos" }).length > 0;
  const resultado = existe
    ? catalogo.runCommand({ collMod: "eventos", validator: validador, validationLevel: "strict", validationAction: "error" })
    : catalogo.runCommand({ create: "eventos", validator: validador, validationLevel: "strict", validationAction: "error" });
  if (resultado.ok !== 1) {
    throw new Error("M013: " + (existe ? "collMod" : "create") + " de eventos falló: " + JSON.stringify(resultado));
  }
  print("    M013: colección eventos " + (existe ? "ya existía; validador fijado" : "creada con su validador"));
  catalogo.eventos.createIndex({ fechaInicio: 1, _id: 1, fechaFin: 1 }, { name: "eventos_agenda" });
  catalogo.eventos.createIndex({ fechaRegistro: -1, _id: -1 }, { name: "eventos_registro" });
  print("    M013: índices eventos_agenda y eventos_registro");
})();
