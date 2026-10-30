// M011 - Oferta del prestador (iteración D; RF32, RF43; DEC-186).
//
// servicios (validador v4): version obligatoria (entero >= 0). Es el control de concurrencia de la edición del
// Prestador (precio y datos comerciales) y de la decisión del Administrador (RF21): cada una compara la versión leída
// y la sube en la misma escritura. Primero se rellena version: 0 en todos los servicios existentes y después se instala
// el validador; si no, ninguna escritura posterior (RF21, RF22) podría tocar un documento sin el campo.
//
// INMUTABLE: usa su propia copia del validador (M011__servicios.v4.schema.json), nunca docs/. Idempotente: el relleno
// solo toca los documentos a los que les falta el campo.

(function () {
  const fs = require("fs");
  const path = require("path");
  const catalogo = db.getSiblingDB("catalog_db");
  const servicios = JSON.parse(fs.readFileSync(path.join(__dirname, "M011__servicios.v4.schema.json"), "utf8"));

  // 1. Relleno: los servicios anteriores empiezan en la versión 0.
  const conVersion = catalogo.servicios.updateMany(
    { version: { $exists: false } }, { $set: { version: NumberInt(0) } });

  // 2. Antes de fijar el v4, ningún documento puede quedar inválido: después no se podría actualizar.
  const invalidos = catalogo.servicios.find({ $nor: [servicios] }, { _id: 1 }).toArray();
  if (invalidos.length > 0) {
    throw new Error("M011: servicios que no cumplen el validador v4: "
      + invalidos.map((s) => s._id.toHexString()).join(", "));
  }

  const resultado = catalogo.runCommand({
    collMod: "servicios", validator: servicios, validationLevel: "strict", validationAction: "error"
  });
  if (resultado.ok !== 1) {
    throw new Error("M011: collMod de servicios falló: " + JSON.stringify(resultado));
  }

  print("    M011: validador v4 (servicios); version en " + conVersion.modifiedCount + " servicios");
})();
