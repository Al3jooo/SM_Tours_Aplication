// M002 - SEMILLA (solo demostración, MONGO_SEMILLA=true): una segunda unidad, «Suite familiar», en Hotel Casa
// Tayrona, para probar el aislamiento de cupos por unidad (CU Reservar servicio). Es la única fuente de esa unidad:
// 03-semilla-catalogo.js es la línea base congelada y no cambia.
//
// Idempotente: no agrega la unidad si ya existe. precioBase se recalcula como el menor precioNoche (sigue en
// 150.000), así que la búsqueda pública no cambia.

(function () {
  const catalogo = db.getSiblingDB("catalog_db");
  const hotel = catalogo.servicios.findOne({ nombreComercial: "Hotel Casa Tayrona", categoria: "Alojamiento" });
  if (hotel === null) {
    print("    M002: no existe Hotel Casa Tayrona; nada que hacer");
    return;
  }
  if (!Array.isArray(hotel.detalleCategoria.unidades)) {
    throw new Error("M002: Hotel Casa Tayrona no tiene unidades; falta M001");
  }

  const suite = {
    idUnidad: new ObjectId(),
    nombre: "Suite familiar",
    tipoHabitacion: "Suite",
    numHabitaciones: NumberInt(3),
    capacidadMaxHuespedes: NumberInt(4),
    precioNoche: NumberDecimal("260000")
  };
  const cambio = catalogo.servicios.updateOne(
    { _id: hotel._id, "detalleCategoria.unidades.nombre": { $ne: suite.nombre } },
    [
      { $set: { "detalleCategoria.unidades": { $concatArrays: ["$detalleCategoria.unidades", [{ $literal: suite }]] } } },
      { $set: { precioBase: { $min: "$detalleCategoria.unidades.precioNoche" } } }
    ]);
  print("    M002: Suite familiar " + (cambio.modifiedCount === 1 ? "agregada" : "ya existía"));
})();
