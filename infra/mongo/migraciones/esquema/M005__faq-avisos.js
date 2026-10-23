// M005 - Colecciones «faq» (RF26, RF47; del modelo del Entregable 2) y «avisos» (RF40; ampliación del modelo), en
// catalog_db (DEC-57, DEC-58). Validadores e índices:
// - faq_titulo: el listado público va por título en orden alfabético, sin distinguir mayúsculas ni tildes (colación
//   es, nivel 1; DEC-63). La consulta usa la misma colación, o MongoDB no usa el índice para ordenar;
// - avisos_activos: el banner, solo los activos, del más reciente al más antiguo;
// - avisos_fecha: el listado del Administrador, todos, en el mismo orden.
//
// INMUTABLE: usa sus propias copias de los validadores (M005__faq.v1.schema.json y M005__avisos.v1.schema.json),
// nunca docs/. Idempotente: crea cada colección si no existe y, si ya existe, solo vuelve a fijar el validador;
// createIndex con el mismo nombre y las mismas claves no hace nada.

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
    if (resultado.ok !== 1) {
      throw new Error("M005: " + (existe ? "collMod" : "create") + " de " + coleccion + " falló: " + JSON.stringify(resultado));
    }
    print("    M005: colección " + coleccion + " " + (existe ? "ya existía; validador fijado" : "creada con su validador"));
  }

  fijarValidador("faq", "M005__faq.v1.schema.json");
  fijarValidador("avisos", "M005__avisos.v1.schema.json");

  catalogo.faq.createIndex({ titulo: 1, _id: 1 }, { name: "faq_titulo", collation: { locale: "es", strength: 1 } });
  catalogo.avisos.createIndex({ activo: 1, fechaPublicacion: -1, _id: -1 }, { name: "avisos_activos" });
  catalogo.avisos.createIndex({ fechaPublicacion: -1, _id: -1 }, { name: "avisos_fecha" });
  print("    M005: índices faq_titulo, avisos_activos y avisos_fecha");
})();
