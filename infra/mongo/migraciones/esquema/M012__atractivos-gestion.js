// M012 - Atractivos y agenda, sesión A1 (iteración E; RF06, RF07, RF24, RF27, RF33, RF58; DEC-207).
//
// atractivos (validador v2): version y fechaRegistro obligatorias (el Gestor edita con una escritura condicional sobre
// la versión, como DEC-186); horarioAtencion (RF58, cierra HC4); categoria como enum (DEC-214); vocabulario de
// accesibilidad de RF27; límites de longitud y de 5 imágenes. Primero se rellena lo que se puede derivar sin inventar
// datos (version 0 y fechaRegistro desde el _id) y después se instala el validador.
//
// No normaliza datos: si un atractivo no cumple el v2 (p. ej. una categoría fuera de la lista), M012 se detiene con la
// lista de _id y no instala el validador. El dato se corrige a mano y se vuelve a aplicar.
//
// INMUTABLE: usa su propia copia del validador (M012__atractivos.v2.schema.json), nunca docs/. Idempotente: el relleno
// solo toca los documentos a los que les falta el campo.

(function () {
  const fs = require("fs");
  const path = require("path");
  const catalogo = db.getSiblingDB("catalog_db");
  const atractivos = JSON.parse(fs.readFileSync(path.join(__dirname, "M012__atractivos.v2.schema.json"), "utf8"));

  // 1. Relleno: los atractivos anteriores empiezan en la versión 0 y con la fecha de su _id.
  const conVersion = catalogo.atractivos.updateMany(
    { version: { $exists: false } }, { $set: { version: NumberInt(0) } });
  const conFecha = catalogo.atractivos.updateMany(
    { fechaRegistro: { $exists: false } }, [{ $set: { fechaRegistro: { $toDate: "$_id" } } }]);

  // 2. Antes de fijar el v2, ningún documento puede quedar inválido: después no se podría actualizar.
  const invalidos = catalogo.atractivos.find({ $nor: [atractivos] }, { _id: 1 }).toArray();
  if (invalidos.length > 0) {
    throw new Error("M012: atractivos que no cumplen el validador v2: "
      + invalidos.map((a) => a._id.toHexString()).join(", "));
  }

  const resultado = catalogo.runCommand({
    collMod: "atractivos", validator: atractivos, validationLevel: "strict", validationAction: "error"
  });
  if (resultado.ok !== 1) {
    throw new Error("M012: collMod de atractivos falló: " + JSON.stringify(resultado));
  }

  print("    M012: validador v2 (atractivos); version en " + conVersion.modifiedCount + " y fechaRegistro en "
    + conFecha.modifiedCount + " atractivos");
})();
