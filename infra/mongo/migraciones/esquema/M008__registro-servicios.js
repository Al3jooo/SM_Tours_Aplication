// M008 - Registrar y aprobar servicio (iteración C; RF09, RF10, RF37, RF21; DEC-126 a DEC-136, DEC-148).
//
// servicios (validador v3): fechaRegistro obligatoria; precioBase obligatorio en las cuatro categorías (DEC-127);
// oferta obligatoria en Tour (días y salidas) y Alojamiento (días), prohibida en las demás (DEC-136); direccion, rnt,
// motivoRechazo, revisadoPor y fechaRevision opcionales en el validador (obligatorios en el dominio para los registros
// nuevos); motivoRechazo existe si y solo si el servicio está Rechazado ($expr); aforos y unidades ≥ 1; vocabulario
// cerrado de accesibilidad (RF27). cuentas (validador v2): rnt opcional (solo Prestador, DEC-146).
//
// Lo que se puede derivar sin inventar datos se rellena aquí: fechaRegistro sale del _id, y la oferta de los servicios
// existentes es la que ya ofrece el generador de inventario D5 de ms-booking (todos los días; tours a las 08:00 y 14:00).
// direccion y rnt no se derivan: la semilla los recibe en M009 y, sin semilla, quedan ausentes (el validador lo admite;
// la línea base de init/ existe aunque MONGO_SEMILLA=false, y exigirlos rompería el updateMulti de RF22).
//
// INMUTABLE: usa sus propias copias de los validadores (M008__servicios.v3.schema.json y M008__cuentas.v2.schema.json),
// nunca docs/. Idempotente: el relleno solo toca los documentos a los que les falta el campo.

(function () {
  const fs = require("fs");
  const path = require("path");
  const catalogo = db.getSiblingDB("catalog_db");
  const leer = (archivo) => JSON.parse(fs.readFileSync(path.join(__dirname, archivo), "utf8"));
  const servicios = leer("M008__servicios.v3.schema.json");
  const cuentas = leer("M008__cuentas.v2.schema.json");

  // 1. Sin precioBase no se puede exigir (RF37): se aborta antes de tocar nada (no se inventan precios).
  const sinPrecio = catalogo.servicios.find({ precioBase: null }, { _id: 1 }).toArray();
  if (sinPrecio.length > 0) {
    throw new Error("M008: servicios sin precioBase: " + sinPrecio.map((s) => s._id.toHexString()).join(", "));
  }

  // 2. Relleno de lo derivable. El validador v2 no tiene additionalProperties en la raíz: acepta los campos nuevos.
  const todosLosDias = ["Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo"];
  const conFecha = catalogo.servicios.updateMany(
    { fechaRegistro: { $exists: false } }, [{ $set: { fechaRegistro: { $toDate: "$_id" } } }]);
  const alojamientos = catalogo.servicios.updateMany(
    { categoria: "Alojamiento", oferta: { $exists: false } }, { $set: { oferta: { diasOperacion: todosLosDias } } });
  const tours = catalogo.servicios.updateMany(
    { categoria: "Tour", oferta: { $exists: false } },
    { $set: { oferta: { diasOperacion: todosLosDias, salidas: ["08:00", "14:00"] } } });

  // 3. Antes de fijar el v3, ningún documento puede quedar inválido: después no se podría actualizar.
  const invalidos = catalogo.servicios.find({ $nor: [servicios] }, { _id: 1 }).toArray();
  if (invalidos.length > 0) {
    throw new Error("M008: servicios que no cumplen el validador v3: "
      + invalidos.map((s) => s._id.toHexString()).join(", "));
  }

  for (const [coleccion, validador] of [["servicios", servicios], ["cuentas", cuentas]]) {
    const resultado = catalogo.runCommand({
      collMod: coleccion, validator: validador, validationLevel: "strict", validationAction: "error"
    });
    if (resultado.ok !== 1) {
      throw new Error("M008: collMod de " + coleccion + " falló: " + JSON.stringify(resultado));
    }
  }

  // 4. Índices de los listados: la cola del Administrador (más antiguo primero, DEC-144) y «Mis servicios» (DEC-143).
  catalogo.servicios.createIndex({ estadoPublicacion: 1, fechaRegistro: 1, _id: 1 }, { name: "admin_estado_fecha" });
  catalogo.servicios.createIndex({ prestadorId: 1, fechaRegistro: -1, _id: -1 }, { name: "prestador_fecha" });

  print("    M008: validadores v3 (servicios) y v2 (cuentas); fechaRegistro: " + conFecha.modifiedCount
    + ", oferta: " + (alojamientos.modifiedCount + tours.modifiedCount));
})();
