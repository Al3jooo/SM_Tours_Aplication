// M009 - SEMILLA (solo demostración, MONGO_SEMILLA=true): dirección y RNT de los servicios de la semilla (HC2,
// DEC-149), para que WF-06 los muestre. 03-semilla-catalogo.js es la línea base congelada y no cambia.
//
// DATOS FICTICIOS: las direcciones son aproximadas e inventadas, como el resto de la semilla. Los RNT no se inventan:
// son los de la semilla de ms-auth (V2 y V3), por prestadorId. La oferta ya la rellenó M008.
//
// Idempotente: solo escribe los campos que faltan.

(function () {
  const catalogo = db.getSiblingDB("catalog_db");
  if (catalogo.migraciones.findOne({ _id: "M008__registro-servicios" }) === null) {
    throw new Error("M009: falta M008 (validador v3 de servicios)");
  }

  // RNT de la semilla de ms-auth: prestador@ (V2) y prestador2@ (V3).
  const rnts = {
    "00000000-0000-4000-8000-000000000002": "RNT-00000",
    "00000000-0000-4000-8000-000000000005": "RNT-00001"
  };
  const direcciones = {
    "Hotel Casa Tayrona": "Calle 19 # 3-45, Centro Histórico, Santa Marta",
    "Cabañas El Roble": "Vereda El Campano, vía Minca, Santa Marta",
    "Hostal Bahía Taganga": "Carrera 1 # 18-20, Taganga, Santa Marta",
    "Ecocabañas Cañaveral": "Sector Cañaveral, Parque Nacional Natural Tayrona",
    "Restaurante Ouzo Santa Marta": "Carrera 3 # 19-29, Parque de los Novios, Santa Marta",
    "Mercado Gastronómico La Plaza": "Calle 11 # 5-60, Centro, Santa Marta",
    "Mirador Gourmet Taganga": "Calle 14 # 1-10, Taganga, Santa Marta",
    "Cocina Caribe El Rodadero": "Carrera 1 # 7-15, El Rodadero, Santa Marta",
    "Tour de Café en Minca": "Parque principal de Minca, Santa Marta",
    "Tour de café Samaria Alta": "Finca Samaria Alta, vía Minca - El Campano, Santa Marta",
    "Caminata a Pueblito Tayrona": "Sector Calabazo, Parque Nacional Natural Tayrona",
    "Snorkel en Bahía Concha": "Bahía Concha, Parque Nacional Natural Tayrona",
    "Alquiler de kayaks Taganga": "Playa principal de Taganga, Santa Marta",
    "Bicicletas Centro Histórico": "Carrera 2 # 16-40, Camellón Rodrigo de Bastidas, Santa Marta"
  };

  let conDireccion = 0;
  for (const [nombre, direccion] of Object.entries(direcciones)) {
    conDireccion += catalogo.servicios.updateOne(
      { nombreComercial: nombre, direccion: { $exists: false } }, { $set: { direccion: direccion } }).modifiedCount;
  }
  let conRnt = 0;
  for (const [prestador, rnt] of Object.entries(rnts)) {
    conRnt += catalogo.servicios.updateMany(
      { prestadorId: prestador, rnt: { $exists: false } }, { $set: { rnt: rnt } }).modifiedCount;
  }
  print("    M009: direccion en " + conDireccion + " servicios; rnt en " + conRnt);
})();
