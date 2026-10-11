# Modelo de datos NoSQL (MongoDB)

Transcripción fiel de los validadores `$jsonSchema` del **Documento Entregable 2** (sección "Diagramas de Base de Datos NoSQL", versión del 10/10/2026). Si el SAD cambia, se actualiza primero el documento y después estos archivos.

| Archivo | Colección | Base de datos | Microservicio |
|---|---|---|---|
| `servicios.schema.json` | `servicios` | `catalog_db` | ms-catalog |
| `atractivos.schema.json` | `atractivos` | `catalog_db` | ms-catalog |

Cada archivo contiene exactamente el objeto que va en `validator` de `db.createCollection(nombre, { validator: … })`.

Las demás colecciones del Entregable 2 (`eventos`, `faq`, `favoritos`, `historial_busqueda`, `itinerarios_sugeridos`, `base_conocimiento_turistico`, `calificaciones`, `reportes`, `logs_auditoria`, `notificaciones`) se transcribirán aquí cuando un caso de uso del prototipo las necesite.

## Notas para la implementación

- `bsonType: "int"` exige enteros de 32 bits: en mongosh, `NumberInt(10)`; en Java, `Integer`.
- `bsonType: "decimal"` exige Decimal128: en mongosh, `NumberDecimal("120000")`; en Java, `BigDecimal` con `@Field(targetType = FieldType.DECIMAL128)`.
- Las coordenadas van en orden **[longitud, latitud]** y requieren un índice `2dsphere`, que no forma parte del validador.
- El `oneOf` de `servicios` discrimina por `categoria`: cada rama fija un único valor y exige su `detalleCategoria`.
