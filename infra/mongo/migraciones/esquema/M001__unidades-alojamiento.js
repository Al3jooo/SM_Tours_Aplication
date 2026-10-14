// M001 - Alojamientos con varias unidades (decisión D1 del CU Reservar servicio; WF-10, RF10).
//
// Formato v1: detalleCategoria = {tipoHabitacion, numHabitaciones, capacidadMaxHuespedes, serviciosIncluidos}.
// Formato v2: detalleCategoria = {unidades: [{idUnidad, nombre, tipoHabitacion, numHabitaciones,
//             capacidadMaxHuespedes, precioNoche}], serviciosIncluidos}; precioBase = menor precioNoche.
//
// INMUTABLE: usa su propia copia del validador (M001__servicios.v2.schema.json), nunca docs/.
// Idempotente: solo convierte los alojamientos que todavía no tienen unidades.

(function () {
  const fs = require("fs");
  const path = require("path");
  const catalogo = db.getSiblingDB("catalog_db");
  const sinUnidades = { categoria: "Alojamiento", "detalleCategoria.unidades": { $exists: false } };

  // 1. Sin precioBase no hay precioNoche: se aborta antes de tocar nada (no se inventan precios).
  const sinPrecio = catalogo.servicios.find({ ...sinUnidades, precioBase: null }, { _id: 1 }).toArray();
  if (sinPrecio.length > 0) {
    throw new Error("M001: alojamientos sin precioBase, no se pueden convertir: "
      + sinPrecio.map((servicio) => servicio._id.toHexString()).join(", "));
  }

  // 2. Primero el validador v2: el v1 tiene additionalProperties: false y rechazaría «unidades». Con
  //    validationLevel strict, cada documento que se actualiza después se valida contra el v2.
  const validador = JSON.parse(fs.readFileSync(path.join(__dirname, "M001__servicios.v2.schema.json"), "utf8"));
  const resultado = catalogo.runCommand({
    collMod: "servicios",
    validator: validador,
    validationLevel: "strict",
    validationAction: "error"
  });
  if (resultado.ok !== 1) {
    throw new Error("M001: collMod falló: " + JSON.stringify(resultado));
  }

  // 3. Cada alojamiento v1 pasa a una sola unidad con sus mismos datos; precioBase no cambia (ya es el mínimo).
  let convertidos = 0;
  catalogo.servicios.find(sinUnidades).forEach((servicio) => {
    const detalle = servicio.detalleCategoria;
    const cambio = catalogo.servicios.updateOne(
      { _id: servicio._id, "detalleCategoria.unidades": { $exists: false } },
      {
        $set: {
          "detalleCategoria.unidades": [{
            idUnidad: new ObjectId(),
            nombre: detalle.tipoHabitacion,
            tipoHabitacion: detalle.tipoHabitacion,
            numHabitaciones: detalle.numHabitaciones,
            capacidadMaxHuespedes: detalle.capacidadMaxHuespedes,
            precioNoche: servicio.precioBase
          }]
        },
        $unset: {
          "detalleCategoria.tipoHabitacion": "",
          "detalleCategoria.numHabitaciones": "",
          "detalleCategoria.capacidadMaxHuespedes": ""
        }
      });
    convertidos += cambio.modifiedCount;
  });
  print("    M001: validador v2 aplicado; alojamientos convertidos: " + convertidos);
})();
