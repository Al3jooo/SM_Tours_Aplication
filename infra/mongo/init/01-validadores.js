// Samaria360 - catalog_db (ms-catalog). Se ejecuta una sola vez, con el volumen de MongoDB vacío
// (docker-entrypoint-initdb.d), en orden alfabético: 01 validadores, 02 índices, 03 semilla.
//
// Validadores $jsonSchema del Documento Entregable 2, copiados TAL CUAL de docs/sad/modelo-nosql/
// (servicios.schema.json y atractivos.schema.json). No se editan aquí: si el SAD cambia, se actualizan esos
// archivos y se vuelve a copiar. La prueba ScriptsInicializacionMongoTest compara ambos.
//
// Solo se crean las colecciones del caso de uso Buscar oferta turística; las demás colecciones del
// Entregable 2 se crearán cuando un caso de uso las necesite.

const catalogo = db.getSiblingDB("catalog_db");

catalogo.createCollection("servicios", {
  validator: {
    "$jsonSchema": {
      "bsonType": "object",
      "required": [
        "prestadorId",
        "nombreComercial",
        "categoria",
        "coordenadas",
        "estadoPublicacion",
        "prestadorActivo"
      ],
      "properties": {
        "_id": { "bsonType": "objectId" },
        "prestadorId": {
          "bsonType": "string",
          "description": "UUID — referencia a Auth Service, sin FK física"
        },
        "nombreComercial": { "bsonType": "string", "maxLength": 150 },
        "categoria": {
          "enum": ["Alojamiento", "Gastronomia", "Tour", "AlquilerEquipo"]
        },
        "descripcion": { "bsonType": "string" },
        "coordenadas": {
          "bsonType": "object",
          "required": ["type", "coordinates"],
          "properties": {
            "type": { "enum": ["Point"] },
            "coordinates": {
              "bsonType": "array",
              "description": "¡ALERTA GEOJSON! El orden exacto debe ser [longitud, latitud]. Rango válido: longitud (-180 a 180), latitud (-90 a 90).",
              "minItems": 2,
              "maxItems": 2,
              "items": { "bsonType": "double" }
            }
          }
        },
        "contacto": {
          "bsonType": "object",
          "properties": {
            "telefono": { "bsonType": "string" },
            "correo": { "bsonType": "string" }
          }
        },
        "estadoPublicacion": {
          "enum": ["Pendiente", "Publicado", "Rechazado"]
        },
        "prestadorActivo": {
          "bsonType": "bool",
          "description": "Campo denormalizado desde Auth. Si es false (Inactivo), se filtra de las búsquedas (RF22)."
        },
        "condicionesAccesibilidad": {
          "bsonType": "array",
          "items": { "bsonType": "string" }
        },
        "horarioAtencion": { "bsonType": "string" },
        "precioBase": { "bsonType": "decimal" },
        "imagenes": {
          "bsonType": "array",
          "items": { "bsonType": "string" }
        }
      },
      "oneOf": [
        {
          "properties": {
            "categoria": { "enum": ["Alojamiento"] },
            "detalleCategoria": {
              "bsonType": "object",
              "required": ["tipoHabitacion", "numHabitaciones", "capacidadMaxHuespedes"],
              "additionalProperties": false,
              "properties": {
                "tipoHabitacion": { "bsonType": "string" },
                "numHabitaciones": { "bsonType": "int" },
                "capacidadMaxHuespedes": { "bsonType": "int" },
                "serviciosIncluidos": {
                  "bsonType": "array",
                  "items": { "bsonType": "string", "enum": ["WiFi", "Desayuno", "Piscina", "Parqueadero"] }
                }
              }
            }
          },
          "required": ["detalleCategoria"]
        },
        {
          "properties": {
            "categoria": { "enum": ["Gastronomia"] },
            "detalleCategoria": {
              "bsonType": "object",
              "required": ["tipoCocina", "aforoMaxTurno", "rangoPrecioPlato"],
              "additionalProperties": false,
              "properties": {
                "tipoCocina": { "bsonType": "string" },
                "aforoMaxTurno": { "bsonType": "int" },
                "menuVegetariano": { "bsonType": "bool" },
                "rangoPrecioPlato": { "bsonType": "string" }
              }
            }
          },
          "required": ["detalleCategoria"]
        },
        {
          "properties": {
            "categoria": { "enum": ["Tour"] },
            "detalleCategoria": {
              "bsonType": "object",
              "required": ["puntoEncuentro", "duracionHoras", "aforoMaxGrupo"],
              "additionalProperties": false,
              "properties": {
                "puntoEncuentro": { "bsonType": "string" },
                "duracionHoras": { "bsonType": "decimal" },
                "nivelDificultad": { "bsonType": "string" },
                "aforoMaxGrupo": { "bsonType": "int" }
              }
            }
          },
          "required": ["detalleCategoria"]
        },
        {
          "properties": {
            "categoria": { "enum": ["AlquilerEquipo"] },
            "detalleCategoria": {
              "bsonType": "object",
              "required": ["tipoVehiculoEquipo", "unidadesDisponibles"],
              "additionalProperties": false,
              "properties": {
                "tipoVehiculoEquipo": { "bsonType": "string" },
                "unidadesDisponibles": { "bsonType": "int" },
                "condicionesAlquiler": { "bsonType": "string" }
              }
            }
          },
          "required": ["detalleCategoria"]
        }
      ]
    }
  },
  validationLevel: "strict",
  validationAction: "error"
});

catalogo.createCollection("atractivos", {
  validator: {
    "$jsonSchema": {
      "bsonType": "object",
      "required": ["gestorId", "nombre", "coordenadas", "limiteCapacidadCarga"],
      "properties": {
        "_id": { "bsonType": "objectId" },
        "gestorId": {
          "bsonType": "string",
          "description": "UUID — referencia a Auth Service, sin FK física"
        },
        "nombre": { "bsonType": "string" },
        "coordenadas": {
          "bsonType": "object",
          "required": ["type", "coordinates"],
          "properties": {
            "type": { "enum": ["Point"] },
            "coordinates": {
              "bsonType": "array",
              "description": "¡ALERTA GEOJSON! El orden exacto debe ser [longitud, latitud]. Rango válido: longitud (-180 a 180), latitud (-90 a 90).",
              "minItems": 2,
              "maxItems": 2,
              "items": { "bsonType": "double" }
            }
          }
        },
        "descripcion": { "bsonType": "string" },
        "categoria": { "bsonType": "string" },
        "imagenes": {
          "bsonType": "array",
          "items": { "bsonType": "string" }
        },
        "limiteCapacidadCarga": { "bsonType": "int", "minimum": 1 },
        "estadoOperativo": {
          "enum": ["Abierto", "Cerrado_temporalmente", "En_mantenimiento"]
        },
        "normasAmbientales": { "bsonType": "string" },
        "condicionesAccesibilidad": {
          "bsonType": "array",
          "items": { "bsonType": "string" }
        }
      }
    }
  },
  validationLevel: "strict",
  validationAction: "error"
});
