// M003 - Proyección «cuentas» (ADR-008, DEC-26): el estado y la versión de las cuentas de ms-auth, para que ms-catalog
// rechace con 401 SESION_INVALIDADA un token con «ver» menor que la versión conocida (DEC-29) y derive
// servicios.prestadorActivo (RF22). Cubre todos los roles.
//
// INMUTABLE: usa su propia copia del validador (M003__cuentas.v1.schema.json), nunca docs/.
// Idempotente: crea la colección si no existe y, si ya existe, solo vuelve a fijar el validador.

(function () {
  const fs = require("fs");
  const path = require("path");
  const catalogo = db.getSiblingDB("catalog_db");
  const validador = JSON.parse(fs.readFileSync(path.join(__dirname, "M003__cuentas.v1.schema.json"), "utf8"));

  const existe = catalogo.getCollectionInfos({ name: "cuentas" }).length > 0;
  const resultado = existe
    ? catalogo.runCommand({ collMod: "cuentas", validator: validador, validationLevel: "strict", validationAction: "error" })
    : catalogo.runCommand({ create: "cuentas", validator: validador, validationLevel: "strict", validationAction: "error" });
  if (resultado.ok !== 1) {
    throw new Error("M003: " + (existe ? "collMod" : "create") + " falló: " + JSON.stringify(resultado));
  }
  print("    M003: colección cuentas " + (existe ? "ya existía; validador fijado" : "creada con su validador"));
})();
