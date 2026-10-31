# Modelo de datos NoSQL (MongoDB)

Validadores de `catalog_db` en su **última versión**. Parten de la transcripción fiel del **Documento Entregable 2** (sección "Diagramas de Base de Datos NoSQL", versión del 10/10/2026), con los cambios posteriores que se listan abajo.

**Dónde vive cada versión.** La versión del Entregable 2 (v1) está congelada en `infra/mongo/init/servicios.v1.schema.json` y `atractivos.v1.schema.json`, que usan los scripts de inicialización (línea base). Cada cambio posterior va en una migración de `infra/mongo/migraciones` con **su propia copia** del validador (p. ej. `esquema/M001__servicios.v2.schema.json`); las migraciones nunca leen estos archivos. Las pruebas de ms-catalog (`MigracionesMongoTest`) verifican que, después de aplicar todas las migraciones, el validador de la base sea igual al de esta carpeta.

| Archivo | Colección | Base de datos | Microservicio |
|---|---|---|---|
| `servicios.schema.json` | `servicios` | `catalog_db` | ms-catalog |
| `atractivos.schema.json` | `atractivos` | `catalog_db` | ms-catalog |
| `cuentas.schema.json` | `cuentas` | `catalog_db` | ms-catalog |
| `faq.schema.json` | `faq` | `catalog_db` | ms-catalog |
| `avisos.schema.json` | `avisos` | `catalog_db` | ms-catalog |
| `favoritos.schema.json` | `favoritos` | `catalog_db` | ms-catalog |
| `historial_busqueda.schema.json` | `historial_busqueda` | `catalog_db` | ms-catalog |

Cada archivo contiene exactamente el objeto que va en `validator` de `db.createCollection` o de `collMod`. `servicios.schema.json` tiene dos claves, `$jsonSchema` y `$expr`: MongoDB exige las dos (un AND implícito).

Las demás colecciones del Entregable 2 (`eventos`, `itinerarios_sugeridos`, `base_conocimiento_turistico`, `calificaciones`, `reportes`, `logs_auditoria`, `notificaciones`) se transcribirán aquí cuando un caso de uso del prototipo las necesite.

## Notas para la implementación

- `bsonType: "int"` exige enteros de 32 bits: en mongosh, `NumberInt(10)`; en Java, `Integer`.
- `bsonType: "decimal"` exige Decimal128: en mongosh, `NumberDecimal("120000")`; en Java, `BigDecimal` con `@Field(targetType = FieldType.DECIMAL128)`.
- Las coordenadas van en orden **[longitud, latitud]** y requieren un índice `2dsphere`, que no forma parte del validador.
- El `oneOf` de `servicios` discrimina por `categoria`: cada rama fija un único valor y exige su `detalleCategoria`.
- **Cambio posterior al Entregable 2:** `detalleCategoria.serviciosIncluidos` (Alojamiento) admite solo `WiFi`, `Desayuno`, `Piscina` y `Parqueadero` (`enum`), el vocabulario cerrado de RF10 y WF-08 que usa «Más filtros» (WF-05).
- **Cambio posterior al Entregable 2 — esquema v2 de `servicios` (migración M001, D1 de «Reservar servicio»):** un alojamiento tiene `detalleCategoria.unidades`, al menos una, en lugar de `tipoHabitacion`, `numHabitaciones` y `capacidadMaxHuespedes`. Cada unidad: `idUnidad` (`objectId`, estable aunque se renombre la unidad), `nombre` (1 a 100 caracteres), `tipoHabitacion`, `numHabitaciones` y `capacidadMaxHuespedes` (enteros ≥ 1) y `precioNoche` (decimal > 0). `serviciosIncluidos` sigue siendo del alojamiento. En Alojamiento `precioBase` es obligatorio y es el precio «desde» (RF37): el menor `precioNoche`. `$jsonSchema` no puede exigir que `idUnidad` sea única dentro del arreglo (`uniqueItems` compara la unidad completa) ni que `precioBase` sea el mínimo; lo hace la condición `$expr`.
- **Colección `faq` (M005; RF26, RF47; DEC-57, DEC-61):** es la del Entregable 2, con `titulo` y `cuerpo`, **más estricta**:
  - `fechaActualizacion` es obligatoria: la escribe siempre el servidor (en el Entregable 2 era opcional);
  - `additionalProperties: false`;
  - límites de 1 a 150 (`titulo`) y de 1 a 2000 (`cuerpo`) caracteres, más un `pattern` que exige el texto sin espacios en los extremos (`^\S([\s\S]*\S)?\z`; con `$`, PCRE aceptaría un salto de línea final). El dominio recorta con `strip()` y nunca guarda lo que el `pattern` rechaza. El `\s` de MongoDB solo reconoce los espacios ASCII (espacio, `\t`, `\n`, `\v`, `\f`, `\r`): los espacios Unicode (U+00A0, U+2003…) cuentan como `\S`.
  - `maxLength` cuenta puntos de código, igual que el dominio.
  - Índice `faq_titulo` con colación `es`, nivel 1 (orden alfabético, DEC-63).
- **Colección `avisos` (M005; RF40; DEC-57, DEC-58), ampliación del modelo del Entregable 2:**
  - `mensaje` (1 a 300 caracteres, mismo `pattern`), `tipo` (`Clima`, `Cierre`, `Comunicado`), `activo` (bool) y `fechaPublicacion` (date), todos obligatorios;
  - sin vigencia automática;
  - índices `avisos_activos` (banner) y `avisos_fecha` (listado del Administrador).
  - Falta llevarla al Documento Entregable 2.
- **Colección `favoritos` (M007; RF54; DEC-80, DEC-86):** los campos del Entregable 2 (§2.4) más una ampliación propia:
  - `turistaId` (UUID en minúsculas, el claim `sub`), `entidadId` (el ObjectId del servicio o atractivo, **en texto**, 24 hex) y `tipoEntidad` (`servicio` o `atractivo`); la API usa `Servicio` y `Atractivo` y el adaptador convierte;
  - **ampliación:** `fechaCreacion` (date), obligatoria, para el orden de «Mis favoritos»;
  - `additionalProperties: false`: no se desnormaliza el nombre ni la imagen (se resuelven al listar con un `$in`, que convierte `entidadId` a ObjectId);
  - índices `favoritos_turista_entidad` (único: un favorito por turista, tipo y entidad) y `favoritos_turista_fecha`.
- **Colección `historial_busqueda` (M007; RF60; DEC-56, DEC-88):** los campos del Entregable 2 (§2.4) más una ampliación propia:
  - `turistaId`, `criteriosBusqueda` (string: el JSON canónico de los criterios normalizados de `GET /oferta`, sin coordenadas ni página; máximo 2000) y `fecha` (date, la última vez que se hizo);
  - **ampliación:** `clave` (string, máximo 2000), la forma canónica de los criterios, con índice único `(turistaId, clave)` para la deduplicación atómica;
  - índice `historial_turista_fecha` para las 10 más recientes y la poda.
  - Las dos ampliaciones faltan en el Documento Entregable 2 (punto 9 de «Pendientes para el SAD (modelo NoSQL)» en `docs/trazabilidad.md`).
- **Esquema v3 de `servicios` (M008; iteración C «Registrar y aprobar servicio»; RF09, RF10, RF37, RF21; DEC-126 a DEC-136, DEC-148):**
  - `fechaRegistro` (date) **obligatoria**: la escribe el servidor al registrar (`RelojPort`); los servicios anteriores la toman del `_id` (M008);
  - `precioBase` **obligatorio en las cuatro categorías** y mayor que 0 (DEC-127; antes, solo en Alojamiento);
  - `oferta` (DEC-136): `{diasOperacion, salidas}`, obligatoria en Tour (días y salidas `HH:mm`) y en Alojamiento (solo días), y prohibida en Gastronomía y Alquiler (`not required`). `diasOperacion`: de 1 a 7 de `Lunes`…`Domingo`, sin tildes y sin repetir; `salidas`: de 1 a 12, sin repetir. M008 rellena la de los servicios existentes con lo que ya ofrecía el generador D5 de ms-booking (todos los días; tours a las 08:00 y 14:00);
  - **ampliaciones** opcionales en el validador y obligatorias en el dominio para los registros nuevos: `direccion` (1 a 200), `rnt` (1 a 50, copia del RNT del prestador, HC2), `motivoRechazo` (1 a 500), `revisadoPor` (UUID) y `fechaRevision` (date). **Justificación:** la semilla de `init/` existe aunque `MONGO_SEMILLA=false`; exigir campos que no se pueden derivar dejaría documentos inválidos y rompería el `updateMulti` de RF22. La semilla los recibe en M009;
  - el `$expr` suma una condición: `motivoRechazo` existe si y solo si `estadoPublicacion` es `Rechazado`;
  - más estricto que el v2: `nombreComercial` y `descripcion` con mínimo 1 (y `descripcion` hasta 2000), `aforoMaxTurno`, `aforoMaxGrupo` y `unidadesDisponibles` ≥ 1, `duracionHoras` > 0 y `condicionesAccesibilidad` con el `enum` de RF27 y `uniqueItems` (resuelve el punto 3 de «Pendientes para el SAD (modelo NoSQL)»);
  - índices `admin_estado_fecha` (cola del Administrador, del más antiguo al más reciente) y `prestador_fecha` («Mis servicios»).
  - Falta llevarlo al Documento Entregable 2.
- **Colección `cuentas` v2 (M008; DEC-146):** suma `rnt` (string de 1 a 50), opcional: solo lo tienen las cuentas de rol Prestador, y falta en las que todavía no recibieron un evento posterior a V17 de ms-auth.
- **Esquema v2 de `atractivos` (M012; iteración E «Atractivos y agenda», sesión A1; RF06, RF07, RF24, RF27, RF33, RF58; DEC-207, DEC-208, DEC-214):**
  - `version` (int ≥ 0) y `fechaRegistro` (date) **obligatorias**: M012 las rellena en los atractivos existentes (0 y la fecha del `_id`) antes de instalar el validador;
  - **ampliaciones:** `horarioAtencion` (1 a 500, RF58; cierra HC4) y `fechaActualizacion` (date), opcionales;
  - más estricto que el v1: `gestorId` con forma de UUID; `nombre` de 1 a 120; `descripcion` y `normasAmbientales` de 1 a 1000; `categoria` con `enum` `Naturaleza` o `Cultura` (**decisión del equipo, no del SRS**, DEC-214; en el v1 era texto libre); `limiteCapacidadCarga` de 1 a 100 000; `imagenes` hasta 5; `condicionesAccesibilidad` con el `enum` de RF27 y `uniqueItems`;
  - `descripcion`, `categoria`, `estadoOperativo` e `imagenes` siguen opcionales en el validador y son obligatorios en el dominio para los registros nuevos (como DEC-148);
  - M012 **no normaliza datos**: un atractivo que no cumple el v2 la detiene con la lista de `_id`;
  - `imagenes[]` guarda rutas públicas: `/api/v1/catalogo/imagenes/{id}` (GridFS, bucket `imagenes`, [ADR-009](../adr/ADR-009-imagenes-gridfs.md)) o, en la semilla, `/imagenes/catalogo/*.webp`.
  - Falta llevarlo al Documento Entregable 2 (punto 12 de «Pendientes para el SAD (modelo NoSQL)» en `docs/trazabilidad.md`).
