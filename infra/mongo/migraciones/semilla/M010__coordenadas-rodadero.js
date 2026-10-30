// M010 - SEMILLA (solo demostración, MONGO_SEMILLA=true): el pin de «Cocina Caribe El Rodadero» caía en el mar
// (N25, O21; DEC-194). 03-semilla-catalogo.js es la línea base congelada y M009 ya está aplicada: ninguno cambia.
//
// DATOS FICTICIOS: la coordenada es aproximada, tierra adentro de la Carrera 1 de El Rodadero (Santa Marta), elegida
// sobre el mapa de OpenStreetMap; como el resto de la semilla, no es la ubicación de un negocio real.
// Orden GeoJSON: [longitud, latitud].
//
// Idempotente: solo cambia el documento si conserva la coordenada original de la semilla.

(function () {
  const catalogo = db.getSiblingDB("catalog_db");
  const original = [-74.229, 11.205];
  const enTierra = [-74.2255, 11.2055];

  const resultado = catalogo.servicios.updateOne(
    { nombreComercial: "Cocina Caribe El Rodadero", "coordenadas.coordinates": original },
    { $set: { coordenadas: { type: "Point", coordinates: enTierra } } });
  print("    M010: coordenadas de Cocina Caribe El Rodadero: " + resultado.modifiedCount);
})();
