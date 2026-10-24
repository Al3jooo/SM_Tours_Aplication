# API de ms-catalog

Contrato de ms-catalog para el frontend y para las pruebas. Todas las rutas van por el API Gateway (`/api/v1/catalogo/**`); el servicio escucha en el 8082 solo dentro de la red de Docker.

Este documento nace con la iteración «Contenido público» (RF26, RF47, RF40; DEC-60). Fija:
- las **preguntas frecuentes** (CU [«Gestionar FAQs»](../../casos-de-uso/CU-gestionar-faqs.md));
- los **avisos generales** (CU [«Crear y publicar aviso general»](../../casos-de-uso/CU-crear-publicar-aviso-general.md));
- los **favoritos** (CU [«Guardar favoritos»](../../casos-de-uso/CU-guardar-favoritos.md)) y el **historial de búsquedas** (CU [«Consultar historial de búsquedas»](../../casos-de-uso/CU-consultar-historial-busquedas.md)), de la iteración B1 «Turista» (RF54, RF60; DEC-80, DEC-56).

La búsqueda, los filtros, el detalle de un servicio y el endpoint interno de la proyección `cuentas` están descritos en `docs/trazabilidad.md` («Endpoints de ms-catalog») y en [`ms-auth.md`](ms-auth.md) («Endpoint interno de los consumidores»). Las decisiones están en `docs/trazabilidad.md` («Contenido público — decisiones» y «Turista (B1) — decisiones»).

## Convenciones

- **Autenticación:** `Authorization: Bearer <JWT de ms-auth>`.
  - `/api/v1/catalogo/admin/**` exige el rol **Administrador** (RNF06, DEC-65).
  - `/api/v1/catalogo/favoritos/**` y `/api/v1/catalogo/historial` exigen el rol **Turista** (RNF06, DEC-80, DEC-56); el turista es siempre el del claim `sub`.
  - `GET /faq` y `GET /avisos` son públicos y se llaman **sin token** (DEC-47).
  - Si a una ruta pública le llega un Bearer, también se valida ([ADR-008](../adr/ADR-008-sincronizacion-estado-cuentas.md)):
    - vencido, mal firmado o de servicio → 401 `NO_AUTENTICADO`;
    - con `ver` menor que la versión de la proyección `cuentas` → 401 `SESION_INVALIDADA`.
- **Errores:** ProblemDetail (RFC 9457) con `codigo` estable, como ms-auth y ms-booking. `DATOS_INVALIDOS` lleva `campos`: un código por campo con el nombre JSON, nunca textos (`REQUERIDO`, `FORMATO`, `LONGITUD_MAXIMA`, `NO_PERMITIDO`). Un cuerpo que no es JSON (o que falta) también es `DATOS_INVALIDOS`, sin `campos`.
- **Textos:** se guardan **sin los espacios de los extremos** (`strip()`).
  - Los límites se cuentan después de recortar y en **puntos de código**: un emoji cuenta uno, como `maxLength` en MongoDB (DEC-61).
  - El contenido va en el idioma en que lo escribe el Administrador, sin campos por idioma (DEC-69).
- **Fechas:** instantes ISO 8601 en UTC, con milisegundos si los hay (`2026-10-22T15:30:00.123Z`).
- **Ids:** ObjectId en hexadecimal (24 caracteres en minúscula). Un id que no tiene esa forma responde **404**, igual que uno inexistente.
- **Página** (solo el listado de avisos del Administrador): de 10, con `pagina` desde 0, como ms-auth (DEC-41): `{elementos, pagina, tamano: 10, totalElementos, totalPaginas}`.

## Preguntas frecuentes (RF26, RF47)

### Listar (público)

`GET /api/v1/catalogo/faq` → **200**:

```json
{
  "elementos": [
    { "id": "6a1f00000000000000000005", "titulo": "¿Cómo encuentro lugares accesibles?",
      "cuerpo": "En la exploración, usa el filtro de accesibilidad…", "fechaActualizacion": "2026-10-22T13:00:00Z" }
  ]
}
```

- **Todas, sin paginar** (DEC-62): la colección tiene como máximo 200. El filtro por palabra clave de RF47 lo hace el cliente, en tiempo real:
  - coincidencia parcial en `titulo` o `cuerpo`;
  - sin distinguir mayúsculas ni tildes (`normalize('NFD')`, sin marcas diacríticas y en minúsculas).
- **Orden** (DEC-63): por `titulo` en orden alfabético, sin distinguir mayúsculas ni tildes (colación `es`, nivel 1), con `id` de desempate.
- El Administrador usa este mismo listado para la gestión: no hay `GET /admin/faq`.

### Crear (Administrador)

`POST /api/v1/catalogo/admin/faq`

```json
{ "titulo": "¿Hay parqueadero?", "cuerpo": "Sí, en la entrada principal." }
```

- `titulo`: de 1 a 150 caracteres. `cuerpo`: de 1 a 2000 (DEC-61).
- **201** con `Location: /api/v1/catalogo/admin/faq/{id}` y la pregunta (`id`, `titulo`, `cuerpo`, `fechaActualizacion`).
- **400 `DATOS_INVALIDOS`:** `titulo` o `cuerpo` → `REQUERIDO` (falta, vacío o solo espacios) o `LONGITUD_MAXIMA`. No guarda nada.
- **409 `LIMITE_FAQ`:** ya hay 200 preguntas (DEC-62).
  - El tope se comprueba contando antes de insertar, **sin atomicidad**: dos creaciones simultáneas pueden dejar 201.
  - Se acepta porque el único actor es el Administrador y el riesgo es bajo.
  - Los datos se validan antes que el tope.

### Editar (Administrador)

`PUT /api/v1/catalogo/admin/faq/{id}` con el mismo cuerpo.
- **200** con la pregunta: título y cuerpo nuevos y `fechaActualizacion` renovada.
- **400** como al crear; los datos se validan antes de buscar la pregunta.
- **404 `FAQ_NO_ENCONTRADA`:** el id no existe (también si la pregunta se eliminó) o no es un ObjectId.
- **Sin control de concurrencia:** gana la última escritura (RF26 no pide versiones).

### Eliminar (Administrador)

`DELETE /api/v1/catalogo/admin/faq/{id}`
- **204**: borrado físico (RF26), sin registro de auditoría (RF13 está fuera de alcance).
- **404 `FAQ_NO_ENCONTRADA`:** el id no existe, ya se eliminó o no es un ObjectId.

## Avisos generales (RF40)

Un aviso tiene `mensaje` (de 1 a 300 caracteres), `tipo` (`Clima`, `Cierre` o `Comunicado`), `activo` y `fechaPublicacion` (DEC-58). Se publica activo. No tiene vigencia automática: el Administrador lo despublica. No se edita ni se borra (DEC-64).

### Avisos activos (público, el banner)

`GET /api/v1/catalogo/avisos` → **200**:

```json
{
  "elementos": [
    { "id": "6a1e00000000000000000001",
      "mensaje": "El sendero de la Cascada El Salto está cerrado temporalmente por lluvias.",
      "tipo": "Cierre", "activo": true, "fechaPublicacion": "2026-10-22T13:00:00Z" }
  ]
}
```

- Solo los activos, **todos** (DEC-58), de `fechaPublicacion` más reciente a más antigua, con `id` descendente de desempate.
- Sin avisos activos, `elementos` va vacío y el banner no se muestra.

### Listado del Administrador

`GET /api/v1/catalogo/admin/avisos?pagina=0` → **200**: página de 10 con todos los avisos, activos e inactivos, en el mismo orden.
- `pagina` negativa o que no es un entero → **400 `DATOS_INVALIDOS`** con `campos.pagina: FORMATO`.
- Una página fuera de rango llega con `elementos` vacío y el total real.

### Publicar

`POST /api/v1/catalogo/admin/avisos`

```json
{ "mensaje": "Lluvias fuertes en la Sierra Nevada esta tarde.", "tipo": "Clima" }
```

- **201** con `Location: /api/v1/catalogo/admin/avisos/{id}` y el aviso (`activo: true`; `fechaPublicacion` la pone el servidor).
- **400 `DATOS_INVALIDOS`:**
  - `mensaje` → `REQUERIDO` o `LONGITUD_MAXIMA`;
  - `tipo` → `REQUERIDO` (falta o vacío) o `NO_PERMITIDO` (otro valor; distingue mayúsculas).

### Despublicar

`POST /api/v1/catalogo/admin/avisos/{id}/despublicacion` · sin cuerpo
- **200** con el aviso (`activo: false`).
- Es un UPDATE condicional (activo → inactivo, como DEC-39). Si no cambia nada, se busca el aviso para distinguir el caso:
  - **404 `AVISO_NO_ENCONTRADO`:** el id no existe o no es un ObjectId;
  - **409 `ESTADO_CAMBIADO`** con `estadoActual: "Inactivo"`: ya estaba despublicado. Otro Administrador pudo hacerlo antes (DEC-64).

## Favoritos (RF54)

Solo el rol **Turista**. `{tipo}` es `Servicio` o `Atractivo`, exacto; cualquier otro valor → **400** `DATOS_INVALIDOS` con `campos.tipo = NO_PERMITIDO` (DEC-80).

### Listar «Mis favoritos»

`GET /api/v1/catalogo/favoritos` → **200**, todos (como máximo 100, sin paginar), del más reciente al más antiguo (DEC-86):

```json
{
  "elementos": [
    { "tipo": "Servicio", "id": "6a1f…", "nombre": "Cabañas El Roble", "categoria": "Alojamiento",
      "imagen": "/imagenes/catalogo/cabanas-el-roble.webp", "fechaCreacion": "2026-10-23T10:00:00.250Z",
      "disponible": true }
  ]
}
```

- Los datos se leen de `servicios` y `atractivos` al listar, sin desnormalizar. `imagen` es la primera de `imagenes` (la portada), o null.
- `disponible`: un servicio Publicado de un prestador activo (RF16, RF22); un atractivo, mientras exista (no tiene estado de publicación).
- Un favorito que deja de estar visible **no se borra**: llega con `disponible: false`. Si el documento ya no existe, `nombre`, `categoria` e `imagen` van en null.

### Guardar

`PUT /api/v1/catalogo/favoritos/{tipo}/{id}` · sin cuerpo
- **201** con el favorito (mismo formato que un elemento de la lista) si se agregó ahora; **200** si ya estaba. Nunca se duplica: si dos `PUT` iguales llegan a la vez, el índice único rechaza uno y se responde 200 con el que ya existe.
- Un favorito ya guardado responde 200 aunque ya no esté disponible.
- **404 `OFERTA_NO_DISPONIBLE`:** el id no es un ObjectId, no existe en la colección de su tipo o el servicio no es visible. No se distingue cuál.
- **409 `LIMITE_FAVORITOS`:** el turista ya tiene 100. Se cuenta antes de insertar, sin atomicidad (como `LIMITE_FAQ`).

### Quitar

`DELETE /api/v1/catalogo/favoritos/{tipo}/{id}` → **204**, existiera o no.

## Historial de búsquedas (RF60)

Solo el rol **Turista**. Es una llamada aparte de `GET /oferta`, que sigue pública y sin token (DEC-56, DEC-47).

### Registrar una búsqueda

`POST /api/v1/catalogo/historial` → **204** siempre que el cuerpo sea válido: si la guarda, si solo actualiza su fecha y si no la guarda por no tener criterios (DEC-88).

```json
{ "texto": "playa", "categoria": "Alojamiento", "precioMax": 200000, "usarUbicacion": true, "distanciaKm": 5,
  "accesibilidad": ["MovilidadReducida"], "orden": "Distancia", "serviciosIncluidos": ["WiFi"], "huespedes": 2 }
```

- Los mismos nombres que los parámetros de `GET /oferta` (incluidos los de «Más filtros»: `serviciosIncluidos`, `huespedes`, `tipoHabitacion`, `menuVegetariano`, `tipoCocina`, `nivelDificultad`, `duracionMaxHoras`, `tipoVehiculoEquipo`), más `usarUbicacion`. Todos opcionales.
- **Sin `lat`, `lng`, `pagina` ni `tamano`:** si llegan, se ignoran. Por RF38 la ubicación del turista no se guarda; `usarUbicacion` dice si la búsqueda la usó, y al repetirla el frontend la vuelve a pedir.
- **Normalización:** texto recortado; `accesibilidad` y `serviciosIncluidos` sin repetir y en el orden del vocabulario; `menuVegetariano: false` equivale a no enviarlo; los decimales, sin ceros a la derecha.
- **Sin criterios** (solo `orden`, o nada) → 204 sin guardar.
- **La misma combinación** (el texto sin distinguir mayúsculas y con los espacios colapsados) no crea otro registro: actualiza la fecha y el texto tal como se escribió, y sube al primer lugar. Si dos `POST` iguales llegan a la vez, el índice único rechaza uno, que se reintenta una vez como actualización; si tampoco cambia nada, igual responde 204.
- Se conservan las **10 más recientes** por turista; lo demás se poda al escribir.
- **400 `CRITERIOS_INVALIDOS`:** las mismas reglas y el mismo código de `GET /oferta` (`distanciaKm` u `orden=Distancia` sin `usarUbicacion`, precio negativo, texto de más de 100 caracteres, un filtro de «Más filtros» sin su categoría, un servicio incluido desconocido). **Es una excepción explícita** a la regla «`DATOS_INVALIDOS` con `campos`»: mismo contrato que `/oferta` para los mismos criterios.
- **400 `DATOS_INVALIDOS`** (sin `campos`): un cuerpo que no es JSON, o un valor que no es del tipo o del enum esperado (`categoria: "Hotel"`, `precioMax: "caro"`).

### Consultar

`GET /api/v1/catalogo/historial` → **200**, como máximo 10, la más reciente primero:

```json
{
  "elementos": [
    { "id": "6a20…", "fecha": "2026-10-23T10:00:00.500Z",
      "criterios": { "texto": "playa", "categoria": "Alojamiento", "precioMax": 200000, "usarUbicacion": true,
                     "distanciaKm": 5.0, "accesibilidad": ["MovilidadReducida"], "orden": "Distancia",
                     "serviciosIncluidos": ["WiFi"], "huespedes": 2, "tipoHabitacion": null,
                     "menuVegetariano": null, "tipoCocina": null, "nivelDificultad": null,
                     "duracionMaxHoras": null, "tipoVehiculoEquipo": null } }
  ]
}
```

`criterios` trae todos los campos (los que no se usaron, en null; las listas, vacías), con los nombres de `GET /oferta`, para repetir la búsqueda con un clic.

## Códigos de error

| Código | HTTP | Cuándo |
|---|---|---|
| `DATOS_INVALIDOS` | 400 | `campos` como se indica arriba. Un cuerpo que no es JSON, sin `campos` |
| `CRITERIOS_INVALIDOS` | 400 | `POST /historial` con criterios que `GET /oferta` también rechaza (DEC-88) |
| `NO_AUTENTICADO` | 401 | Sin token en `/admin/**`, `/favoritos/**` o `/historial`, o con un token vencido, inválido o de servicio (`rol = SERVICIO`) |
| `SESION_INVALIDADA` | 401 | `ver` menor que la versión de la proyección `cuentas`. **Limitación conocida:** hoy el Administrador no tiene fila en la proyección (no hay RF45), así que su `ver` no se invalida; se cierra con RF45 |
| `ACCESO_DENEGADO` | 403 | `/admin/**` con un rol que no es Administrador (Turista, Prestador o Gestor); `/favoritos/**` o `/historial` con un rol que no es Turista |
| `OFERTA_NO_DISPONIBLE` | 404 | `PUT /favoritos/{tipo}/{id}` con un id inválido, inexistente o no visible |
| `FAQ_NO_ENCONTRADA` | 404 | Pregunta inexistente o id que no es un ObjectId |
| `AVISO_NO_ENCONTRADO` | 404 | Aviso inexistente o id que no es un ObjectId |
| `LIMITE_FAQ` | 409 | Ya hay 200 preguntas frecuentes |
| `LIMITE_FAVORITOS` | 409 | El turista ya tiene 100 favoritos |
| `ESTADO_CAMBIADO` | 409 | El aviso ya estaba despublicado; lleva `estadoActual: "Inactivo"` |
| `ERROR_INTERNO` | 500 | Cualquier otro error |

## Colecciones

`faq` y `avisos` en `catalog_db` (DEC-57). Los validadores están en [`modelo-nosql/faq.schema.json`](../modelo-nosql/faq.schema.json) y [`modelo-nosql/avisos.schema.json`](../modelo-nosql/avisos.schema.json); la migración M005 lleva su propia copia.

Índices:
- `faq_titulo`: `{titulo: 1, _id: 1}` con colación `es`, nivel 1;
- `avisos_activos`: `{activo: 1, fechaPublicacion: -1, _id: -1}`;
- `avisos_fecha`: `{fechaPublicacion: -1, _id: -1}`.

La semilla de demostración (M006, solo con `MONGO_SEMILLA=true`) trae 5 preguntas y 1 aviso activo.

`favoritos` e `historial_busqueda` (M007; [`modelo-nosql/favoritos.schema.json`](../modelo-nosql/favoritos.schema.json) y [`modelo-nosql/historial_busqueda.schema.json`](../modelo-nosql/historial_busqueda.schema.json)). Los campos son los del Entregable 2 (§2.4), más dos ampliaciones propias:
- `favoritos`: `{_id, turistaId, entidadId, tipoEntidad, fechaCreacion}`. `entidadId` es el ObjectId en texto; `tipoEntidad`, `servicio` o `atractivo` (la API usa `Servicio` y `Atractivo`). **Ampliación:** `fechaCreacion`, para el orden de «Mis favoritos».
- `historial_busqueda`: `{_id, turistaId, criteriosBusqueda, fecha, clave}`. `criteriosBusqueda` es el JSON canónico de los criterios normalizados (máximo 2000 caracteres). **Ampliación:** `clave`, la forma canónica para deduplicar (máximo 2000).

Índices:
- `favoritos_turista_entidad` (único): `{turistaId: 1, tipoEntidad: 1, entidadId: 1}`;
- `favoritos_turista_fecha`: `{turistaId: 1, fechaCreacion: -1, _id: -1}`;
- `historial_turista_fecha`: `{turistaId: 1, fecha: -1, _id: -1}`;
- `historial_turista_clave` (único): `{turistaId: 1, clave: 1}`.

Sin semilla (no hay M008): el turista de la demostración crea sus favoritos y su historial al usar la aplicación.
