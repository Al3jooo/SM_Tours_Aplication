// M007 - Colecciones «favoritos» (RF54; DEC-80, DEC-86) e «historial_busqueda» (RF60; DEC-56, DEC-88), en catalog_db.
// Campos del Entregable 2 (§2.4) más dos ampliaciones propias: favoritos.fechaCreacion e historial_busqueda.clave.
// Índices:
//   favoritos_turista_entidad (único): un turista guarda cada servicio o atractivo una sola vez (DEC-80);
//   favoritos_turista_fecha: «Mis favoritos», del más reciente al más antiguo;
//   historial_turista_fecha: las 10 búsquedas más recientes y la poda;
//   historial_turista_clave (único): la misma combinación de criterios no crea otro registro (deduplicación atómica).
// INMUTABLE: usa sus propias copias de los validadores (M007__favoritos.v1.schema.json y
// M007__historial_busqueda.v1.schema.json), nunca docs/. Idempotente: crea cada colección si no existe y, si ya existe,
// solo vuelve a fijar el validador; createIndex con el mismo nombre y las mismas claves no hace nada.
(function () {
  const fs = require("fs");
  const path = require("path");
  const catalogo = db.getSiblingDB("catalog_db");
  function fijarValidador(coleccion, archivo) {
    const validador = JSON.parse(fs.readFileSync(path.join(__dirname, archivo), "utf8"));
    const existe = catalogo.getCollectionInfos({ name: coleccion }).length > 0;
    const resultado = existe
      ? catalogo.runCommand({ collMod: coleccion, validator: validador, validationLevel: "strict", validationAction: "error" })
      : catalogo.runCommand({ create: coleccion, validator: validador, validationLevel: "strict", validationAction: "error" });
    if (resultado.ok !== 1) { throw new Error("M007: " + (existe ? "collMod" : "create") + " de " + coleccion + " falló: " + JSON.stringify(resultado)); }
    print("    M007: colección " + coleccion + " " + (existe ? "ya existía; validador fijado" : "creada con su validador"));
  }
  fijarValidador("favoritos", "M007__favoritos.v1.schema.json");
  fijarValidador("historial_busqueda", "M007__historial_busqueda.v1.schema.json");
  catalogo.favoritos.createIndex({ turistaId: 1, tipoEntidad: 1, entidadId: 1 }, { name: "favoritos_turista_entidad", unique: true });
  catalogo.favoritos.createIndex({ turistaId: 1, fechaCreacion: -1, _id: -1 }, { name: "favoritos_turista_fecha" });
  catalogo.historial_busqueda.createIndex({ turistaId: 1, fecha: -1, _id: -1 }, { name: "historial_turista_fecha" });
  catalogo.historial_busqueda.createIndex({ turistaId: 1, clave: 1 }, { name: "historial_turista_clave", unique: true });
  print("    M007: índices favoritos_turista_entidad, favoritos_turista_fecha, historial_turista_fecha e historial_turista_clave");
})();
