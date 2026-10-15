# Modelo de datos NoSQL (MongoDB)

Validadores de `catalog_db` en su **última versión**. Parten de la transcripción fiel del **Documento Entregable 2** (sección "Diagramas de Base de Datos NoSQL", versión del 10/10/2026), con los cambios posteriores que se listan abajo, pendientes de llevar al SAD.

**Dónde vive cada versión.** La versión del Entregable 2 (v1) está congelada en `infra/mongo/init/servicios.v1.schema.json` y `atractivos.v1.schema.json`, que usan los scripts de inicialización (línea base). Cada cambio posterior va en una migración de `infra/mongo/migraciones` con **su propia copia** del validador (p. ej. `esquema/M001__servicios.v2.schema.json`); las migraciones nunca leen estos archivos. Las pruebas de ms-catalog (`MigracionesMongoTest`) verifican que, después de aplicar todas las migraciones, el validador de la base sea igual al de esta carpeta.

| Archivo | Colección | Base de datos | Microservicio |
|---|---|---|---|
| `servicios.schema.json` | `servicios` | `catalog_db` | ms-catalog |
| `atractivos.schema.json` | `atractivos` | `catalog_db` | ms-catalog |

Cada archivo contiene exactamente el objeto que va en `validator` de `db.createCollection` o de `collMod`. `servicios.schema.json` tiene dos claves, `$jsonSchema` y `$expr`: MongoDB exige las dos (un AND implícito).

Las demás colecciones del Entregable 2 (`eventos`, `faq`, `favoritos`, `historial_busqueda`, `itinerarios_sugeridos`, `base_conocimiento_turistico`, `calificaciones`, `reportes`, `logs_auditoria`, `notificaciones`) se transcribirán aquí cuando un caso de uso del prototipo las necesite.

## Notas para la implementación

- `bsonType: "int"` exige enteros de 32 bits: en mongosh, `NumberInt(10)`; en Java, `Integer`.
- `bsonType: "decimal"` exige Decimal128: en mongosh, `NumberDecimal("120000")`; en Java, `BigDecimal` con `@Field(targetType = FieldType.DECIMAL128)`.
- Las coordenadas van en orden **[longitud, latitud]** y requieren un índice `2dsphere`, que no forma parte del validador.
- El `oneOf` de `servicios` discrimina por `categoria`: cada rama fija un único valor y exige su `detalleCategoria`.
- **Cambio posterior al Entregable 2:** `detalleCategoria.serviciosIncluidos` (Alojamiento) admite solo `WiFi`, `Desayuno`, `Piscina` y `Parqueadero` (`enum`), el vocabulario cerrado de RF10 y WF-08 que usa «Más filtros» (WF-05). Pendiente de llevarlo a la sección NoSQL del SAD (ver `docs/trazabilidad.md`).
- **Cambio posterior al Entregable 2 — esquema v2 de `servicios` (migración M001, D1 de «Reservar servicio»):** un alojamiento tiene `detalleCategoria.unidades`, al menos una, en lugar de `tipoHabitacion`, `numHabitaciones` y `capacidadMaxHuespedes`. Cada unidad: `idUnidad` (`objectId`, estable aunque se renombre la unidad), `nombre` (1 a 100 caracteres), `tipoHabitacion`, `numHabitaciones` y `capacidadMaxHuespedes` (enteros ≥ 1) y `precioNoche` (decimal > 0). `serviciosIncluidos` sigue siendo del alojamiento. En Alojamiento `precioBase` es obligatorio y es el precio «desde» (RF37): el menor `precioNoche`. `$jsonSchema` no puede exigir que `idUnidad` sea única dentro del arreglo (`uniqueItems` compara la unidad completa) ni que `precioBase` sea el mínimo; lo hace la condición `$expr`. Pendiente de llevarlo a la sección NoSQL del SAD (ver `docs/trazabilidad.md`).
