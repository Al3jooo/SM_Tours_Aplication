// M006 - SEMILLA (solo demostración, MONGO_SEMILLA=true): 5 preguntas frecuentes (RF26) y 1 aviso activo (RF40), en
// español (DEC-69: el contenido va en el idioma en que lo escribe el Administrador). El aviso es el ejemplo de WF-04: el
// sendero de la Cascada El Salto, que la semilla del catálogo tiene «Cerrado_temporalmente».
//
// Idempotente: _id y fechas fijos, y replaceOne con upsert. Un volumen nuevo y uno existente quedan iguales.

(function () {
  const catalogo = db.getSiblingDB("catalog_db");
  const fecha = ISODate("2026-10-22T13:00:00Z");

  const preguntas = [
    {
      _id: ObjectId("6a1f00000000000000000001"),
      titulo: "¿Cómo reservo un servicio?",
      cuerpo: "Busca el servicio en la exploración, abre su detalle y elige «Ver disponibilidad y reservar». " +
        "Selecciona las fechas, la unidad y el número de huéspedes, y confirma la reserva con tu cuenta de turista."
    },
    {
      _id: ObjectId("6a1f00000000000000000002"),
      titulo: "¿Hasta cuándo puedo cancelar una reserva?",
      cuerpo: "Puedes cancelar una reserva confirmada desde «Mis reservas» hasta 24 horas antes de su inicio."
    },
    {
      _id: ObjectId("6a1f00000000000000000003"),
      titulo: "¿Cómo registro mi negocio como prestador?",
      cuerpo: "Crea una cuenta de prestador de servicios con tu número RNT. La cuenta queda pendiente de validación " +
        "hasta que un administrador revise tus datos."
    },
    {
      _id: ObjectId("6a1f00000000000000000004"),
      titulo: "¿Puedo usar Samaria360 en inglés?",
      cuerpo: "Sí. Cambia el idioma con el selector ES / EN del encabezado; no pierdes tu sesión ni lo que tienes en " +
        "pantalla."
    },
    {
      _id: ObjectId("6a1f00000000000000000005"),
      titulo: "¿Cómo encuentro lugares accesibles?",
      cuerpo: "En la exploración, usa el filtro de accesibilidad. Los servicios y atractivos declaran sus condiciones " +
        "para personas con movilidad reducida, discapacidad visual o auditiva."
    }
  ];
  for (const pregunta of preguntas) {
    catalogo.faq.replaceOne({ _id: pregunta._id }, Object.assign({}, pregunta, { fechaActualizacion: fecha }),
      { upsert: true });
  }

  const aviso = {
    _id: ObjectId("6a1e00000000000000000001"),
    mensaje: "El sendero de la Cascada El Salto está cerrado temporalmente por lluvias.",
    tipo: "Cierre",
    activo: true,
    fechaPublicacion: fecha
  };
  catalogo.avisos.replaceOne({ _id: aviso._id }, aviso, { upsert: true });

  print("    M006: " + preguntas.length + " preguntas frecuentes y 1 aviso activo");
})();
