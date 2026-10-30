# API de ms-catalog

Contrato de ms-catalog para el frontend y para las pruebas. Todas las rutas van por el API Gateway (`/api/v1/catalogo/**`); el servicio escucha en el 8082 solo dentro de la red de Docker.

Este documento nace con la iteración «Contenido público» (RF26, RF47, RF40; DEC-60). Fija:
- las **preguntas frecuentes** (CU [«Gestionar FAQs»](../../casos-de-uso/CU-gestionar-faqs.md));
- los **avisos generales** (CU [«Crear y publicar aviso general»](../../casos-de-uso/CU-crear-publicar-aviso-general.md));
- los **favoritos** (CU [«Guardar favoritos»](../../casos-de-uso/CU-guardar-favoritos.md)) y el **historial de búsquedas** (CU [«Consultar historial de búsquedas»](../../casos-de-uso/CU-consultar-historial-busquedas.md)), de la iteración B1 «Turista» (RF54, RF60; DEC-80, DEC-56);
- el **registro de servicios**, «Mis servicios» y la **aprobación** (CU [«Registrar servicio comercial»](../../casos-de-uso/CU-registrar-servicio.md) y [«Aprobar o rechazar servicio»](../../casos-de-uso/CU-aprobar-rechazar-servicio.md)), de la iteración C (RF09, RF10, RF37, RF21; DEC-124 a DEC-151), con los campos nuevos del detalle público y el RNT de la proyección `cuentas`.

La búsqueda, los filtros, el detalle de un servicio y el endpoint interno de la proyección `cuentas` están descritos en `docs/trazabilidad.md` («Endpoints de ms-catalog») y en [`ms-auth.md`](ms-auth.md) («Endpoint interno de los consumidores»). Las decisiones están en `docs/trazabilidad.md` («Contenido público — decisiones» y «Turista (B1) — decisiones»).

## Convenciones

- **Autenticación:** `Authorization: Bearer <JWT de ms-auth>`.
  - `/api/v1/catalogo/admin/**` exige el rol **Administrador** (RNF06, DEC-65).
  - `/api/v1/catalogo/favoritos/**` y `/api/v1/catalogo/historial` exigen el rol **Turista** (RNF06, DEC-80, DEC-56); el turista es siempre el del claim `sub`.
  - `/api/v1/catalogo/prestador/**` exige el rol **Prestador** (RNF06, DEC-125); el prestador es siempre el del claim `sub`.
  - `GET /faq` y `GET /avisos` son públicos y se llaman **sin token** (DEC-47).
  - Si a una ruta pública le llega un Bearer, también se valida ([ADR-008](../adr/ADR-008-sincronizacion-estado-cuentas.md)):
    - vencido, mal firmado o de servicio → 401 `NO_AUTENTICADO`;
    - con `ver` menor que la versión de la proyección `cuentas` → 401 `SESION_INVALIDADA`.
- **Errores:** ProblemDetail (RFC 9457) con `codigo` estable, como ms-auth y ms-booking. `DATOS_INVALIDOS` lleva `campos`: un código por campo con el nombre JSON, nunca textos (`REQUERIDO`, `FORMATO`, `LONGITUD_MAXIMA`, `NO_PERMITIDO`; el registro de servicios suma `DUPLICADO`, `VALOR_MINIMO`, `VALOR_MAXIMO`, `CANTIDAD_MINIMA` y `CANTIDAD_MAXIMA`, DEC-151). Un cuerpo que no es JSON (o que falta) también es `DATOS_INVALIDOS`, sin `campos`.
- **Textos:** se guardan **sin los espacios de los extremos** (`strip()`).
  - Los límites se cuentan después de recortar y en **puntos de código**: un emoji cuenta uno, como `maxLength` en MongoDB (DEC-61).
  - El contenido va en el idioma en que lo escribe el Administrador, sin campos por idioma (DEC-69).
- **Fechas:** instantes ISO 8601 en UTC, con milisegundos si los hay (`2026-10-22T15:30:00.123Z`).
- **Ids:** ObjectId en hexadecimal (24 caracteres en minúscula). Un id que no tiene esa forma responde **404**, igual que uno inexistente.
- **Página** (el listado de avisos del Administrador, «Mis servicios» y la cola de servicios): de 10, con `pagina` desde 0, como ms-auth (DEC-41): `{elementos, pagina, tamano: 10, totalElementos, totalPaginas}`.

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

## Registrar servicio comercial y Mis servicios (RF09, RF10, RF37)

CU [«Registrar servicio comercial»](../../casos-de-uso/CU-registrar-servicio.md), con el flujo «Consultar mis servicios» (DEC-129). Solo el rol **Prestador**; el prestador es siempre el del claim `sub`. Decisiones DEC-124 a DEC-151 en `docs/trazabilidad.md` («Registrar y aprobar servicio — decisiones»).

### Registrar

`POST /api/v1/catalogo/prestador/servicios`

```json
{
  "nombreComercial": "Tour Bahía Concha",
  "categoria": "Tour",
  "descripcion": "Salida en lancha con equipo de snorkel.",
  "direccion": "Muelle de Taganga, Santa Marta",
  "coordenadas": { "longitud": -74.1906, "latitud": 11.2672 },
  "contacto": { "telefono": "+57 300 000 0000", "correo": "hola@bahia.test" },
  "condicionesAccesibilidad": ["Visual"],
  "horarioAtencion": "Sábados y domingos, salidas a las 8:00 y 14:00",
  "precioBase": 95000,
  "detalleCategoria": { "puntoEncuentro": "Muelle de Taganga", "duracionHoras": 3.5, "nivelDificultad": "Baja", "aforoMaxGrupo": 12 },
  "oferta": { "diasOperacion": ["Sabado", "Domingo"], "salidas": ["08:00", "14:00"] }
}
```

| Campo | Regla (textos con `strip()`, contados en puntos de código; DEC-138) |
|---|---|
| `nombreComercial` | obligatorio, 1 a 150 |
| `categoria` | obligatorio: `Alojamiento`, `Gastronomia`, `Tour` o `AlquilerEquipo` |
| `descripcion` | obligatorio (RF09), 1 a 2000 |
| `direccion` | obligatorio, 1 a 200 (DEC-126) |
| `coordenadas` | obligatorio `{longitud, latitud}`; solo el rango global: −180 a 180 y −90 a 90 (DEC-137). Se guarda como GeoJSON `[longitud, latitud]` |
| `contacto.telefono` | obligatorio, 1 a 20 (la regla de ms-auth) |
| `contacto.correo` | obligatorio, hasta 254, con la forma `^[^\s@]+@[^\s@]+\.[^\s@]+$` (la de ms-auth) |
| `condicionesAccesibilidad` | opcional: `MovilidadReducida`, `Visual`, `Auditiva` (RF27), sin repetir |
| `horarioAtencion` | opcional (RF58), 1 a 500; vacío es «sin valor» |
| `precioBase` | obligatorio salvo en Alojamiento: mayor que 0, hasta 2 decimales y hasta 99 999 999,99 (cabe en la reserva de ms-booking). En Alojamiento se ignora y se deriva del menor `precioNoche` (DEC-127) |
| `detalleCategoria` (Alojamiento) | `unidades`: de 1 a 20, cada una con `nombre` (1 a 100), `tipoHabitacion` (1 a 50), `numHabitaciones` (1 a 1000), `capacidadMaxHuespedes` (1 a 50) y `precioNoche` (como `precioBase`); `serviciosIncluidos` opcional: `WiFi`, `Desayuno`, `Piscina`, `Parqueadero`, sin repetir. El `idUnidad` lo genera el servidor (DEC-133, DEC-141) |
| `detalleCategoria` (Gastronomia) | `tipoCocina` (1 a 100), `aforoMaxTurno` (1 a 10 000), `menuVegetariano` (opcional), `rangoPrecioPlato` (1 a 50) |
| `detalleCategoria` (Tour) | `puntoEncuentro` (1 a 200), `duracionHoras` (mayor que 0, hasta 720, 2 decimales), `nivelDificultad` (opcional, 1 a 50), `aforoMaxGrupo` (1 a 10 000) |
| `detalleCategoria` (AlquilerEquipo) | `tipoVehiculoEquipo` (1 a 100), `unidadesDisponibles` (1 a 10 000), `condicionesAlquiler` (opcional, 1 a 500) |
| `oferta` (DEC-136) | obligatoria en Tour y Alojamiento, prohibida en Gastronomia y AlquilerEquipo (`NO_PERMITIDO`). `diasOperacion`: de 1 a 7 de `Lunes`, `Martes`, `Miercoles`, `Jueves`, `Viernes`, `Sabado`, `Domingo`, sin repetir. `salidas` (solo Tour, obligatoria): de 1 a 12 horas `HH:mm`, sin repetir; se guardan en orden. En Alojamiento, `salidas` es `NO_PERMITIDO` |

Del cuerpo se ignoran `prestadorId`, `estadoPublicacion`, `prestadorActivo`, `rnt`, `idUnidad` y el `precioBase` de un alojamiento (DEC-141). Los campos de `detalleCategoria` de otra categoría también se ignoran.

Respuestas:
- **201** con `Location: /api/v1/catalogo/prestador/servicios/{id}` y el servicio en su forma de gestión (abajo), con `estadoPublicacion: "Pendiente"`.
- **400 `DATOS_INVALIDOS`** con `campos`; los anidados con punto e índice: `contacto.correo`, `coordenadas.latitud`, `detalleCategoria.unidades[0].precioNoche`, `oferta.salidas[1]`. Códigos: `REQUERIDO`, `FORMATO`, `LONGITUD_MAXIMA`, `NO_PERMITIDO`, `DUPLICADO`, `VALOR_MINIMO`, `VALOR_MAXIMO`, `CANTIDAD_MINIMA`, `CANTIDAD_MAXIMA` (DEC-151).
- **403 `PRESTADOR_NO_HABILITADO`**: la cuenta no tiene fila en la proyección `cuentas`, no está `Activo` o la fila no tiene RNT (DEC-125, DEC-139). Se evalúa antes que los datos.
- **403 `ACCESO_DENEGADO`** con otro rol; **401** sin token o con `ver` viejo.
- Si el prestador se inactiva mientras se registra, el servicio queda con `prestadorActivo: false` (DEC-142).

Forma de gestión (registro, detalle propio y detalle del Administrador):

```json
{
  "id": "6a20…", "idPrestador": "00000000-0000-4000-8000-000000000002", "nombreComercial": "Tour Bahía Concha",
  "categoria": "Tour", "descripcion": "…", "direccion": "Muelle de Taganga, Santa Marta",
  "coordenadas": { "longitud": -74.1906, "latitud": 11.2672 },
  "contacto": { "telefono": "+57 300 000 0000", "correo": "hola@bahia.test" },
  "condicionesAccesibilidad": ["Visual"], "horarioAtencion": "…", "precioBase": 95000, "moneda": "COP",
  "detalleCategoria": { "puntoEncuentro": "Muelle de Taganga", "duracionHoras": 3.5, "nivelDificultad": "Baja", "aforoMaxGrupo": 12 },
  "oferta": { "diasOperacion": ["Sabado", "Domingo"], "salidas": ["08:00", "14:00"] },
  "imagenes": [], "estadoPublicacion": "Pendiente", "prestadorActivo": true, "rnt": "RNT-00000",
  "fechaRegistro": "2026-10-24T15:00:00.321Z", "motivoRechazo": null, "fechaRevision": null
}
```

En la vista del Administrador se suman `revisadoPor` (quien decidió) y `estadoCuentaPrestador` (el estado según la proyección `cuentas`; se omite si no hay fila).

### Mis servicios (DEC-143)

- `GET /api/v1/catalogo/prestador/servicios?estado=&pagina=` → **200**, página de 10, del más reciente al más antiguo. `estado`: `Pendiente`, `Publicado` o `Rechazado`; sin él, todos. Cada elemento: `{id, nombreComercial, categoria, estadoPublicacion, motivoRechazo, fechaRegistro}`. **400** con `campos.estado` (`NO_PERMITIDO`) o `campos.pagina` (`FORMATO`).
- `GET /api/v1/catalogo/prestador/servicios/{id}` → **200** con la forma de gestión, en cualquier estado. **404 `SERVICIO_NO_ENCONTRADO`** si no existe, el id no es un ObjectId o es de otro prestador.

## Aprobar o rechazar servicio (RF21)

CU [«Aprobar o rechazar servicio»](../../casos-de-uso/CU-aprobar-rechazar-servicio.md), con «Consultar listado pendiente» y «Justificar rechazo de servicio». Solo el rol **Administrador** (`/admin/**`).

- `GET /api/v1/catalogo/admin/servicios?estado=&pagina=` → **200**, página de 10, del más antiguo al más reciente (la cola de revisión; DEC-144). `estado` por defecto `Pendiente`. Cada elemento: `{id, nombreComercial, categoria, idPrestador, rnt, estadoPublicacion, prestadorActivo, fechaRegistro}`.
- `GET /api/v1/catalogo/admin/servicios/{id}` → **200** con la forma de gestión de la vista del Administrador, sin la regla de visibilidad pública. **404 `SERVICIO_NO_ENCONTRADO`**.
- `POST /api/v1/catalogo/admin/servicios/{id}/aprobacion`

```json
{ "decision": "Aprobar" }
{ "decision": "Rechazar", "justificacion": "Las fotos no corresponden al lugar" }
```

- `decision`: `Aprobar` o `Rechazar` (ausente → `REQUERIDO`; otro valor → `NO_PERMITIDO`).
- `justificacion`: obligatoria al rechazar, de 1 a 500 caracteres sin los espacios de los extremos (como DEC-42); al aprobar se ignora (DEC-145).
- **200** con el detalle (`Publicado` o `Rechazado`), con `revisadoPor` (`sub`), `fechaRevision` y, si se rechazó, `motivoRechazo`.
- **400 `DATOS_INVALIDOS`** con `campos.decision` o `campos.justificacion`. **404 `SERVICIO_NO_ENCONTRADO`**. **409 `ESTADO_CAMBIADO`** con `estadoActual` (`Publicado` o `Rechazado`): otro Administrador ya decidió (DEC-128). Es un UPDATE condicional sobre `Pendiente`.
- Aprobar el servicio de un prestador inactivo se permite: queda `Publicado` y oculto por `prestadorActivo` (RF22).

## Detalle público: dirección, RNT y oferta (DEC-150)

`GET /api/v1/catalogo/servicios/{id}` suma tres campos al final de la respuesta: `direccion`, `rnt` (copia del RNT del prestador; HC2) y `oferta` (`{diasOperacion, salidas}` en Tour, `{diasOperacion}` en Alojamiento, `null` en Gastronomía y Alquiler). Son `null` en un servicio que no los tiene. No expone `estadoPublicacion` ni la revisión. ms-booking lee `oferta` de esta respuesta para materializar el inventario (DEC-135, sesión A2) e ignora los campos que no usa.

## Proyección `cuentas`: RNT (DEC-146)

`PUT /internal/v1/cuentas/{id}` (solo el relevo de ms-auth, con su JWT de servicio) acepta `rnt` además de `{rol, estadoCuenta, version}`. Se guarda solo si `rol = Prestador` (en otro rol se ignora); un evento sin `rnt` lo quita de la fila. Más de 50 caracteres → **400** con `campos.rnt = LONGITUD_MAXIMA`. El resto del contrato está en [`ms-auth.md`](ms-auth.md) («Endpoint interno de los consumidores»).

## Códigos de error

| Código | HTTP | Cuándo |
|---|---|---|
| `DATOS_INVALIDOS` | 400 | `campos` como se indica arriba. Un cuerpo que no es JSON, sin `campos` |
| `CRITERIOS_INVALIDOS` | 400 | `POST /historial` con criterios que `GET /oferta` también rechaza (DEC-88) |
| `NO_AUTENTICADO` | 401 | Sin token en `/admin/**`, `/favoritos/**` o `/historial`, o con un token vencido, inválido o de servicio (`rol = SERVICIO`) |
| `SESION_INVALIDADA` | 401 | `ver` menor que la versión de la proyección `cuentas`, o un token de Gestor o Administrador cuya cuenta no tiene fila en ella (DEC-110; L3 cerrado con RF45) |
| `ACCESO_DENEGADO` | 403 | `/admin/**` con un rol que no es Administrador (Turista, Prestador o Gestor); `/favoritos/**` o `/historial` con un rol que no es Turista; `/prestador/**` con un rol que no es Prestador |
| `PRESTADOR_NO_HABILITADO` | 403 | `POST /prestador/servicios` de un prestador sin fila en `cuentas`, no `Activo` o sin RNT (DEC-139) |
| `OFERTA_NO_DISPONIBLE` | 404 | `PUT /favoritos/{tipo}/{id}` con un id inválido, inexistente o no visible |
| `FAQ_NO_ENCONTRADA` | 404 | Pregunta inexistente o id que no es un ObjectId |
| `AVISO_NO_ENCONTRADO` | 404 | Aviso inexistente o id que no es un ObjectId |
| `SERVICIO_NO_ENCONTRADO` | 404 | Servicio inexistente, id que no es un ObjectId o, en «Mis servicios», de otro prestador |
| `LIMITE_FAQ` | 409 | Ya hay 200 preguntas frecuentes |
| `LIMITE_FAVORITOS` | 409 | El turista ya tiene 100 favoritos |
| `ESTADO_CAMBIADO` | 409 | El aviso ya estaba despublicado (`estadoActual: "Inactivo"`) o el servicio ya no estaba Pendiente (`estadoActual`: `Publicado` o `Rechazado`) |
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

Sin semilla propia: el turista de la demostración crea sus favoritos y su historial al usar la aplicación.

`servicios` en su esquema v3 (M008; [`modelo-nosql/servicios.schema.json`](../modelo-nosql/servicios.schema.json), DEC-148): suma `fechaRegistro` (obligatoria), `oferta` (obligatoria en Tour y Alojamiento), `direccion`, `rnt`, `motivoRechazo`, `revisadoPor` y `fechaRevision`; exige `precioBase` en todas las categorías y el motivo si y solo si el servicio está Rechazado. `cuentas` suma `rnt` (M008). La semilla M009 (solo con `MONGO_SEMILLA=true`) da dirección y RNT a los servicios de la demostración.

Índices nuevos de `servicios`:
- `admin_estado_fecha`: `{estadoPublicacion: 1, fechaRegistro: 1, _id: 1}`;
- `prestador_fecha`: `{prestadorId: 1, fechaRegistro: -1, _id: -1}`.
