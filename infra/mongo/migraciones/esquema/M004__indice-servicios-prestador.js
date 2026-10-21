// M004 - Índice por prestador en servicios (ADR-008). Cuando una cuenta cambia de estado, ms-catalog actualiza
// servicios.prestadorActivo de todos los servicios de ese prestador (RF22): sin índice recorrería la colección entera.
//
// Idempotente: createIndex con el mismo nombre y las mismas claves no hace nada.

(function () {
  const catalogo = db.getSiblingDB("catalog_db");
  catalogo.servicios.createIndex({ prestadorId: 1 }, { name: "prestador" });
  print("    M004: índice prestador en servicios");
})();
