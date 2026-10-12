// Samaria360 - catalog_db: semilla de demostración del caso de uso Buscar oferta turística.
//
// DATOS FICTICIOS DE DEMOSTRACIÓN. Los nombres provienen de los wireframes (docs/wireframes/, WF-04 y WF-05)
// para mantener la trazabilidad, pero los precios, descripciones, contactos y detalles son inventados.
// Las coordenadas son aproximadas a los lugares reales de Santa Marta, Taganga, Minca y el Tayrona.
//
// Reglas del esquema (docs/sad/modelo-nosql/README.md):
//   - coordenadas GeoJSON en orden [longitud, latitud], siempre con decimales (bsonType "double";
//     mongosh guardaría un número entero como int32 y el validador lo rechazaría);
//   - enteros con NumberInt(...); dinero y horas con NumberDecimal("...").
//
// Casos para probar los filtros de la búsqueda pública (estadoPublicacion = Publicado y prestadorActivo = true):
//   - 2 servicios Pendientes: «Cocina Caribe El Rodadero» y «Alquiler de kayaks Taganga»;
//   - 1 servicio de un prestador inactivo: «Snorkel en Bahía Concha» (prestador2 de ms-auth, estado_cuenta Inactivo).

const catalogo = db.getSiblingDB("catalog_db");

// UUID de la semilla de ms-auth (ms-auth/src/main/resources/db/semilla/).
const PRESTADOR = "00000000-0000-4000-8000-000000000002"; // prestador@samaria360.test (Activo)
const PRESTADOR_INACTIVO = "00000000-0000-4000-8000-000000000005"; // prestador2@samaria360.test (Inactivo)
const GESTOR = "00000000-0000-4000-8000-000000000003"; // gestor@samaria360.test

const punto = (longitud, latitud) => ({ type: "Point", coordinates: [longitud, latitud] });
// Fotos optimizadas por frontend/scripts/optimizar-imagenes.mjs (3:2, WebP). Si una no carga, el frontend usa la
// ilustración SVG de la categoría.
const imagen = (slug) => [`/imagenes/catalogo/${slug}.webp`];

catalogo.servicios.insertMany([
  // ------------------------------------------------------------------ Alojamiento
  {
    prestadorId: PRESTADOR,
    nombreComercial: "Hotel Casa Tayrona",
    categoria: "Alojamiento",
    descripcion: "Alojamiento boutique cerca del Parque de los Novios. Desayuno incluido.",
    coordenadas: punto(-74.2125, 11.2425),
    contacto: { telefono: "+57 300 000 0101", correo: "reservas@casatayrona.test" },
    estadoPublicacion: "Publicado",
    prestadorActivo: true,
    condicionesAccesibilidad: ["MovilidadReducida"],
    horarioAtencion: "Recepción 24 horas",
    precioBase: NumberDecimal("150000"),
    imagenes: imagen("hotel-casa-tayrona"),
    detalleCategoria: {
      tipoHabitacion: "Doble",
      numHabitaciones: NumberInt(12),
      capacidadMaxHuespedes: NumberInt(2),
      serviciosIncluidos: ["WiFi", "Desayuno"]
    }
  },
  {
    prestadorId: PRESTADOR,
    nombreComercial: "Cabañas El Roble",
    categoria: "Alojamiento",
    descripcion: "Cabañas entre cafetales con chimenea y desayuno típico en Minca.",
    coordenadas: punto(-74.1185, 11.143),
    contacto: { telefono: "+57 300 000 0102", correo: "hola@cabanaselroble.test" },
    estadoPublicacion: "Publicado",
    prestadorActivo: true,
    condicionesAccesibilidad: [],
    horarioAtencion: "Lunes a domingo, 7:00 a. m. - 9:00 p. m.",
    precioBase: NumberDecimal("180000"),
    imagenes: imagen("cabanas-el-roble"),
    detalleCategoria: {
      tipoHabitacion: "Cabaña",
      numHabitaciones: NumberInt(6),
      capacidadMaxHuespedes: NumberInt(4),
      serviciosIncluidos: ["WiFi", "Desayuno", "Parqueadero"]
    }
  },
  {
    prestadorId: PRESTADOR,
    nombreComercial: "Hostal Bahía Taganga",
    categoria: "Alojamiento",
    descripcion: "Hostal frente a la bahía, a pasos de los centros de buceo.",
    coordenadas: punto(-74.1905, 11.2668),
    contacto: { telefono: "+57 300 000 0103", correo: "info@hostalbahiataganga.test" },
    estadoPublicacion: "Publicado",
    prestadorActivo: true,
    condicionesAccesibilidad: ["Visual"],
    horarioAtencion: "Lunes a domingo, 6:00 a. m. - 11:00 p. m.",
    precioBase: NumberDecimal("90000"),
    imagenes: imagen("hostal-bahia-taganga"),
    detalleCategoria: {
      tipoHabitacion: "Compartida",
      numHabitaciones: NumberInt(8),
      capacidadMaxHuespedes: NumberInt(6),
      serviciosIncluidos: ["WiFi"]
    }
  },
  {
    prestadorId: PRESTADOR,
    nombreComercial: "Ecocabañas Cañaveral",
    categoria: "Alojamiento",
    descripcion: "Ecocabañas en la entrada de Cañaveral del Parque Tayrona, con vista al mar.",
    coordenadas: punto(-73.93, 11.311),
    contacto: { telefono: "+57 300 000 0104", correo: "reservas@ecocanaveral.test" },
    estadoPublicacion: "Publicado",
    prestadorActivo: true,
    condicionesAccesibilidad: [],
    horarioAtencion: "Lunes a domingo, 8:00 a. m. - 6:00 p. m.",
    precioBase: NumberDecimal("420000"),
    imagenes: imagen("ecocabanas-canaveral"),
    detalleCategoria: {
      tipoHabitacion: "Ecohabitación",
      numHabitaciones: NumberInt(10),
      capacidadMaxHuespedes: NumberInt(3),
      serviciosIncluidos: ["Desayuno", "Piscina"]
    }
  },

  // ------------------------------------------------------------------ Gastronomía
  {
    prestadorId: PRESTADOR,
    nombreComercial: "Restaurante Ouzo Santa Marta",
    categoria: "Gastronomia",
    descripcion: "Especialidad en comida mediterránea y mariscos locales. Menú vegetariano.",
    coordenadas: punto(-74.212, 11.243),
    contacto: { telefono: "+57 300 000 0201", correo: "reservas@ouzo.test" },
    estadoPublicacion: "Publicado",
    prestadorActivo: true,
    condicionesAccesibilidad: ["MovilidadReducida"],
    horarioAtencion: "Martes a domingo, 12:00 m. - 11:00 p. m.",
    precioBase: NumberDecimal("50000"),
    imagenes: imagen("restaurante-ouzo"),
    detalleCategoria: {
      tipoCocina: "Mediterránea",
      aforoMaxTurno: NumberInt(60),
      menuVegetariano: true,
      rangoPrecioPlato: "$50.000 - $120.000"
    }
  },
  {
    prestadorId: PRESTADOR,
    nombreComercial: "Mercado Gastronómico La Plaza",
    categoria: "Gastronomia",
    descripcion: "Plaza de comidas con cocina samaria, jugos naturales y postres típicos.",
    coordenadas: punto(-74.2105, 11.2445),
    contacto: { telefono: "+57 300 000 0202", correo: "contacto@mercadolaplaza.test" },
    estadoPublicacion: "Publicado",
    prestadorActivo: true,
    condicionesAccesibilidad: ["MovilidadReducida", "Auditiva"],
    horarioAtencion: "Lunes a domingo, 10:00 a. m. - 10:00 p. m.",
    precioBase: NumberDecimal("15000"),
    imagenes: imagen("mercado-la-plaza"),
    detalleCategoria: {
      tipoCocina: "Colombiana",
      aforoMaxTurno: NumberInt(120),
      menuVegetariano: true,
      rangoPrecioPlato: "$15.000 - $40.000"
    }
  },
  {
    prestadorId: PRESTADOR,
    nombreComercial: "Mirador Gourmet Taganga",
    categoria: "Gastronomia",
    descripcion: "Pescados y mariscos del día con vista a la bahía de Taganga.",
    coordenadas: punto(-74.192, 11.266),
    contacto: { telefono: "+57 300 000 0203", correo: "hola@miradorgourmet.test" },
    estadoPublicacion: "Publicado",
    prestadorActivo: true,
    condicionesAccesibilidad: [],
    horarioAtencion: "Miércoles a lunes, 12:00 m. - 9:00 p. m.",
    precioBase: NumberDecimal("35000"),
    imagenes: imagen("mirador-gourmet-taganga"),
    detalleCategoria: {
      tipoCocina: "Mariscos",
      aforoMaxTurno: NumberInt(40),
      menuVegetariano: false,
      rangoPrecioPlato: "$35.000 - $80.000"
    }
  },
  {
    prestadorId: PRESTADOR,
    nombreComercial: "Cocina Caribe El Rodadero",
    categoria: "Gastronomia",
    descripcion: "Cocina caribeña frente a la playa de El Rodadero.",
    coordenadas: punto(-74.229, 11.205),
    contacto: { telefono: "+57 300 000 0204", correo: "info@cocinacaribe.test" },
    estadoPublicacion: "Pendiente", // en revisión del Administrador: no aparece en la búsqueda
    prestadorActivo: true,
    condicionesAccesibilidad: [],
    horarioAtencion: "Lunes a domingo, 11:00 a. m. - 10:00 p. m.",
    precioBase: NumberDecimal("30000"),
    imagenes: imagen("cocina-caribe-el-rodadero"),
    detalleCategoria: {
      tipoCocina: "Caribeña",
      aforoMaxTurno: NumberInt(50),
      menuVegetariano: false,
      rangoPrecioPlato: "$30.000 - $70.000"
    }
  },

  // ------------------------------------------------------------------ Tour
  {
    prestadorId: PRESTADOR,
    nombreComercial: "Tour de Café en Minca",
    categoria: "Tour",
    descripcion: "Recorrido guiado de 4 horas por fincas cafeteras tradicionales.",
    coordenadas: punto(-74.119, 11.1435),
    contacto: { telefono: "+57 300 000 0301", correo: "tours@cafeminca.test" },
    estadoPublicacion: "Publicado",
    prestadorActivo: true,
    condicionesAccesibilidad: ["Auditiva"],
    horarioAtencion: "Salidas diarias a las 8:00 a. m. y 1:00 p. m.",
    precioBase: NumberDecimal("80000"),
    imagenes: imagen("tour-cafe-minca"),
    detalleCategoria: {
      puntoEncuentro: "Iglesia de Minca",
      duracionHoras: NumberDecimal("4"),
      nivelDificultad: "Media",
      aforoMaxGrupo: NumberInt(12)
    }
  },
  {
    prestadorId: PRESTADOR,
    nombreComercial: "Tour de café Samaria Alta",
    categoria: "Tour",
    descripcion: "Recorrido por la finca Samaria Alta con cata de café y almuerzo campesino.",
    coordenadas: punto(-74.11, 11.135),
    contacto: { telefono: "+57 300 000 0302", correo: "visitas@samariaalta.test" },
    estadoPublicacion: "Publicado",
    prestadorActivo: true,
    condicionesAccesibilidad: [],
    horarioAtencion: "Martes a domingo, salida a las 9:00 a. m.",
    precioBase: NumberDecimal("120000"),
    imagenes: imagen("tour-cafe-samaria-alta"),
    detalleCategoria: {
      puntoEncuentro: "Parque principal de Minca",
      duracionHoras: NumberDecimal("6"),
      nivelDificultad: "Media",
      aforoMaxGrupo: NumberInt(10)
    }
  },
  {
    prestadorId: PRESTADOR,
    nombreComercial: "Caminata a Pueblito Tayrona",
    categoria: "Tour",
    descripcion: "Caminata guiada al sitio arqueológico de Pueblito por senderos de la selva.",
    coordenadas: punto(-73.97, 11.315),
    contacto: { telefono: "+57 300 000 0303", correo: "guias@pueblito.test" },
    estadoPublicacion: "Publicado",
    prestadorActivo: true,
    condicionesAccesibilidad: [],
    horarioAtencion: "Salida diaria a las 6:30 a. m.",
    precioBase: NumberDecimal("150000"),
    imagenes: imagen("caminata-pueblito-tayrona"),
    detalleCategoria: {
      puntoEncuentro: "Sector Calabazo, Parque Tayrona",
      duracionHoras: NumberDecimal("8"),
      nivelDificultad: "Alta",
      aforoMaxGrupo: NumberInt(15)
    }
  },
  {
    prestadorId: PRESTADOR_INACTIVO, // prestador con estado_cuenta Inactivo: no aparece en la búsqueda (RF22)
    nombreComercial: "Snorkel en Bahía Concha",
    categoria: "Tour",
    descripcion: "Salida en lancha con equipo de snorkel incluido.",
    coordenadas: punto(-74.13, 11.295),
    contacto: { telefono: "+57 300 000 0304", correo: "snorkel@bahiaconcha.test" },
    estadoPublicacion: "Publicado",
    prestadorActivo: false,
    condicionesAccesibilidad: [],
    horarioAtencion: "Salidas a las 9:00 a. m.",
    precioBase: NumberDecimal("95000"),
    imagenes: imagen("snorkel-bahia-concha"),
    detalleCategoria: {
      puntoEncuentro: "Muelle de Taganga",
      duracionHoras: NumberDecimal("3.5"),
      nivelDificultad: "Baja",
      aforoMaxGrupo: NumberInt(8)
    }
  },

  // ------------------------------------------------------------------ Alquiler de equipos
  {
    prestadorId: PRESTADOR,
    nombreComercial: "Alquiler de kayaks Taganga",
    categoria: "AlquilerEquipo",
    descripcion: "Kayaks dobles para recorrer la bahía de Taganga.",
    coordenadas: punto(-74.191, 11.2672),
    contacto: { telefono: "+57 300 000 0401", correo: "kayaks@taganga.test" },
    estadoPublicacion: "Pendiente", // en revisión del Administrador: no aparece en la búsqueda
    prestadorActivo: true,
    condicionesAccesibilidad: [],
    horarioAtencion: "Lunes a domingo, 8:00 a. m. - 5:00 p. m.",
    precioBase: NumberDecimal("40000"),
    imagenes: imagen("kayaks-taganga"),
    detalleCategoria: {
      tipoVehiculoEquipo: "Kayak doble",
      unidadesDisponibles: NumberInt(10),
      condicionesAlquiler: "Documento de identidad y depósito de $50.000"
    }
  },
  {
    prestadorId: PRESTADOR,
    nombreComercial: "Bicicletas Centro Histórico",
    categoria: "AlquilerEquipo",
    descripcion: "Bicicletas urbanas para recorrer el Centro Histórico y el malecón.",
    coordenadas: punto(-74.211, 11.241),
    contacto: { telefono: "+57 300 000 0402", correo: "rodar@bicicentro.test" },
    estadoPublicacion: "Publicado",
    prestadorActivo: true,
    condicionesAccesibilidad: [],
    horarioAtencion: "Lunes a sábado, 7:00 a. m. - 7:00 p. m.",
    precioBase: NumberDecimal("30000"),
    imagenes: imagen("bicicletas-centro-historico"),
    detalleCategoria: {
      tipoVehiculoEquipo: "Bicicleta urbana",
      unidadesDisponibles: NumberInt(15),
      condicionesAlquiler: "Documento de identidad"
    }
  }
]);

catalogo.atractivos.insertMany([
  {
    gestorId: GESTOR,
    nombre: "Cascada Quebrada Valencia",
    coordenadas: punto(-73.796, 11.235),
    descripcion: "Reserva natural con piscinas cristalinas en la vía a Palomino.",
    categoria: "Naturaleza",
    imagenes: imagen("cascada-quebrada-valencia"),
    limiteCapacidadCarga: NumberInt(300),
    estadoOperativo: "Abierto",
    normasAmbientales: "No usar bloqueador ni repelente antes de entrar al agua. Lleva tus residuos de regreso.",
    condicionesAccesibilidad: []
  },
  {
    gestorId: GESTOR,
    nombre: "Cascada El Salto",
    coordenadas: punto(-74.11, 11.15),
    descripcion: "Sendero entre bosque húmedo hasta una caída de agua cerca de Minca.",
    categoria: "Naturaleza",
    imagenes: imagen("cascada-el-salto"),
    limiteCapacidadCarga: NumberInt(80),
    estadoOperativo: "Cerrado_temporalmente", // aviso de WF-04: sendero cerrado por lluvias
    normasAmbientales: "Permanece en el sendero señalizado. Prohibido hacer fogatas.",
    condicionesAccesibilidad: []
  },
  {
    gestorId: GESTOR,
    nombre: "Cabo San Juan del Guía",
    coordenadas: punto(-73.948, 11.334),
    descripcion: "Playa emblemática del Parque Nacional Natural Tayrona.",
    categoria: "Naturaleza",
    imagenes: imagen("cabo-san-juan"),
    limiteCapacidadCarga: NumberInt(300),
    estadoOperativo: "Abierto",
    normasAmbientales: "Prohibido ingresar plásticos de un solo uso. Respeta las zonas de baño señalizadas.",
    condicionesAccesibilidad: []
  },
  {
    gestorId: GESTOR,
    nombre: "Playa Cristal",
    coordenadas: punto(-74.05, 11.326),
    descripcion: "Playa de aguas transparentes en el sector de Neguanje, ideal para snorkel.",
    categoria: "Naturaleza",
    imagenes: imagen("playa-cristal"),
    limiteCapacidadCarga: NumberInt(400),
    estadoOperativo: "Abierto",
    normasAmbientales: "No toques ni pises los corales. Usa solo bloqueador biodegradable.",
    condicionesAccesibilidad: []
  },
  {
    gestorId: GESTOR,
    nombre: "Quinta de San Pedro Alejandrino",
    coordenadas: punto(-74.188, 11.2215),
    descripcion: "Hacienda histórica y museo donde murió Simón Bolívar, con jardín botánico.",
    categoria: "Cultura",
    imagenes: imagen("quinta-san-pedro-alejandrino"),
    limiteCapacidadCarga: NumberInt(500),
    estadoOperativo: "Abierto",
    normasAmbientales: "No arranques plantas del jardín botánico.",
    condicionesAccesibilidad: ["MovilidadReducida", "Visual"]
  },
  {
    gestorId: GESTOR,
    nombre: "Catedral Basílica de Santa Marta",
    coordenadas: punto(-74.2117, 11.2441),
    descripcion: "Una de las catedrales más antiguas de América, en el Centro Histórico.",
    categoria: "Cultura",
    imagenes: imagen("catedral-santa-marta"),
    limiteCapacidadCarga: NumberInt(400),
    estadoOperativo: "Abierto",
    normasAmbientales: "Guarda silencio durante las celebraciones.",
    condicionesAccesibilidad: ["MovilidadReducida"]
  },
  {
    gestorId: GESTOR,
    nombre: "Mirador de Taganga",
    coordenadas: punto(-74.196, 11.264),
    descripcion: "Mirador sobre la bahía de Taganga, famoso por sus atardeceres.",
    categoria: "Naturaleza",
    imagenes: imagen("mirador-taganga"),
    limiteCapacidadCarga: NumberInt(100),
    estadoOperativo: "En_mantenimiento",
    normasAmbientales: "No arrojes basura por el acantilado.",
    condicionesAccesibilidad: []
  }
]);
