# Samaria360 — Reglas para agentes de código

Plataforma de turismo de Santa Marta. Proyecto Capstone Design (Arquitectura de Software, Universidad del Magdalena).

## Idioma

- Responde siempre en **español**: planes, resúmenes, preguntas y explicaciones.
- El código, los comandos, los nombres de archivos y los términos técnicos sin traducción establecida (por ejemplo, `fitBounds`, *commit*, *bundle*) pueden quedar como están.

## Contexto de proceso

- Metodología **RUP**, fase de **Construcción**. Este código es el **prototipo arquitectónico ejecutable** que valida la línea base de la arquitectura; no es un producto completo.
- Usar solo terminología RUP: fases, iteraciones, casos de uso, requisitos funcionales (RF), requisitos no funcionales (RNF). No usar terminología de métodos ágiles.
- **Alcance de esta iteración:**
  - inicio de sesión y registro de turistas y prestadores (`ms-auth`: RF15, RF01, RF02);
  - vista principal del catálogo (`ms-catalog`);
  - el CU «Reservar servicio» (`ms-booking`: RF11, RF18, RF20, RF32, RF37, RF44), con RF12 del lado del turista («Mis reservas» con filtros) y RF52 (Confirmada → Completada automática) ya implementados;
  - el CU «Administrar cuentas» (`ms-auth`, con proyecciones en `ms-catalog` y `ms-booking`: RF28, RF22 y RF62; WF-12, propuesto por arquitectura y pendiente de validación). La parte de backend y documentos está hecha; la pantalla WF-12 es la sesión B. RF45 no se implementa, pero el diseño le sirve ([CU](docs/casos-de-uso/CU-administrar-cuentas.md), [ADR-008](docs/sad/adr/ADR-008-sincronizacion-estado-cuentas.md));
  - las pantallas de reservas, con carga diferida: WF-06 mínimo (`/servicios/:id`), WF-10 (`/servicios/:id/reservar`, la selección vive en la URL) y WF-11 «Mis reservas» (`/mis-reservas`, privada, solo turistas; pendiente de diseño), más la disponibilidad de las tarjetas y la miniatura del popup del mapa (RF23);
  - «Contenido público» (DEC-54, iteración A del plan del 60 %): preguntas frecuentes (RF26, RF47), avisos generales (RF40) y términos y condiciones (RF46). El backend vive en `ms-catalog` (DEC-57) y ya está hecho; las pantallas WF-13 a WF-15 (propuestas por arquitectura, pendientes de validación) y el banner común son la sesión B. CU: [Gestionar FAQs](docs/casos-de-uso/CU-gestionar-faqs.md), [Crear y publicar aviso general](docs/casos-de-uso/CU-crear-publicar-aviso-general.md) y [Consultar términos y condiciones](docs/casos-de-uso/CU-consultar-terminos.md).
- **Fuera de alcance:** chatbot real, notificaciones (el aviso por correo del registro llega con RF34; RF13, RF30 y RF63, con ms-notificaciones), analítica, en reservas RF12 del lado del prestador y RF19, y en contenido público «Noticias» del pie, la vigencia automática de los avisos y el contenido en varios idiomas (DEC-58, DEC-69). No implementarlos aunque parezcan fáciles.
- Si una regla de negocio no está definida en el SRS (`docs/srs/`), **preguntar**; no inventarla.
- Fuente única de requisitos: `docs/srs/especificacion de requisitos.pdf`. Modelo de datos: `docs/sad/diagramas/modelo-logico.dbml`. Ignorar las carpetas `Documentos extras/`, `DocumentosEntregable1/` y `Diagramas de casos de uso/`, que contienen versiones anteriores.
- Las pantallas del frontend y los datos que exponen los endpoints deben corresponder a los wireframes de `docs/wireframes/`. Si un elemento del wireframe queda fuera del alcance del prototipo, se muestra deshabilitado y se lista como pendiente.
- Wireframes: `docs/wireframes/` (leer primero su README.md). Las pantallas del frontend y los datos que exponen los endpoints deben corresponder a ellos; lo que quede fuera del alcance se muestra deshabilitado y se lista como pendiente.

## Arquitectura

- Microservicios con **diseño hexagonal** interno. Cada carpeta raíz es un contenedor del diagrama de despliegue.
- Paquete base Java: `co.edu.unimagdalena.samaria360.<servicio>`.
- Capas y reglas de dependencia (verificadas con ArchUnit en `src/test/.../architecture/`):
  - `domain/`: Java puro. **Sin** Spring, JPA, Jackson ni Lombok.
  - `application/`: puertos (`port/in`, `port/out`) y casos de uso (`service/`). Depende solo de `domain`. Sin anotaciones de Spring.
  - `infrastructure/`: adaptadores de entrada (`adapter/in/web`) y salida (`adapter/out/...`), y `config/`. Los casos de uso se registran como beans en `infrastructure/config/BeanConfig.java`.
- No hay `pom.xml` raíz ni librería compartida entre servicios. Cada servicio se compila con su propio `./mvnw`.
- Sin Lombok. Usar `record` para DTOs y value objects.
- Nombres del dominio en español (Usuario, Rol, PoliticaBloqueo); nombres técnicos de patrones en inglés (Adapter, Port, Controller).

## ms-auth (Java 17, Spring Boot 3.5.16, PostgreSQL `auth_db`)

- Esquema con **Flyway**. `spring.jpa.hibernate.ddl-auto=validate`; nunca `update` ni `create`.
- **Migraciones:**
  - **nunca se edita una migración ya aplicada**: rompe el checksum de Flyway en las bases de los compañeros; cada cambio va en una migración nueva (una por cambio);
  - la numeración es compartida entre las dos carpetas;
  - el esquema y los **datos de referencia** (p. ej. V10, catálogo de roles) van en `src/main/resources/db/migration`;
  - la **semilla** (datos de prueba, solo perfiles dev y test) va en `src/main/resources/db/semilla`.
- Tablas de `auth_db` (modelo lógico del SAD): `Rol`, `Usuario`, `Turista`, `Turista_Categoria_Interes`, `Prestador`, `Gestor`, `Administrador`, `Token_Recuperacion`, y el outbox de «Administrar cuentas» (`outbox_evento`, `outbox_entrega`; V13). Las tablas de reservas pertenecen a `reservas_db` y **no** van en ms-auth.
- Migraciones de «Administrar cuentas»: V11 (`prestador.motivo_rechazo`, `revisado_en`, `revisado_por`; rellena los Rechazados anteriores antes del CHECK), V12 (`usuario.version_cuenta`), V13 (outbox) y, solo semilla, V14. El estado del prestador es **derivado** de `estado_validacion` y `estado_cuenta` (`EstadoAdministrativo`: Pendiente, Rechazado, Activo, Inactivo); el turista desactivado (RF62) guarda `estado_cuenta = 'Desactivado'` y el prestador inactivo (RF22), `'Inactivo'` (DEC-43, DEC-44).
- Identificadores de usuario: tipo nativo PostgreSQL `uuid` ↔ `java.util.UUID`.
- `estado_cuenta` es un enum de PostgreSQL (`CREATE TYPE`). Mapear con `@Enumerated(EnumType.STRING)` + `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`.
- Fechas con hora: `timestamptz` ↔ `Instant`. Obtener la hora actual siempre a través de un `RelojPort`, para poder probar el bloqueo sin esperar 15 minutos.
- **RF15 (login):** 3 intentos fallidos consecutivos → `bloqueado_hasta = ahora + 15 min`. Orden (decisión D3, `docs/trazabilidad.md`):
  1. **solo el 423** (cuenta bloqueada) va antes de verificar la contraseña;
  2. contraseña incorrecta → 401 con el **mismo mensaje** que un correo inexistente, y suma un intento, sea cual sea el estado de la cuenta;
  3. con la contraseña correcta: 403 `CUENTA_INACTIVA` si `estado_cuenta` no es Activo (RF22, RF62); 403 `CUENTA_PENDIENTE_VALIDACION` o `CUENTA_RECHAZADA` si es un prestador sin validar (RF02, RF28). En esos 403 no se emite token y el conteo vuelve a 0;
  4. el acceso solo se registra al emitir el token.
- **RNF05:** contraseñas con BCrypt (`spring-security-crypto`), detrás de `PasswordHasherPort`.
- **JWT RS256** con `NimbusJwtEncoder`. Claims: `sub` = id_usuario, `rol`, **`ver`** (= `usuario.version_cuenta` al iniciar sesión), `iss`, `iat` y `exp`. **El token vence a los 15 minutos** (fijo en `application.yml`, DEC-29; ya no hay `JWT_EXPIRACION_MINUTOS`). ms-catalog y ms-booking validan la firma, `iss` y `exp` con el JWKS (ADR-005) y además `ver`: con `ver` menor que la versión de su proyección, 401 `SESION_INVALIDADA`; sin fila, aceptan; un token sin `ver` cuenta como 0. ms-auth compara con su propia tabla. Un Bearer que llegue a un endpoint público también se valida. La clave pública se publica en `GET /api/v1/auth/.well-known/jwks.json`. Las llaves se leen de las rutas en `JWT_LLAVE_PRIVADA` / `JWT_LLAVE_PUBLICA` (ver `.env.example`).
- Endpoints del prototipo: `POST /api/v1/auth/login`, `GET /api/v1/auth/me`, `POST /api/v1/auth/registro/turista`, `POST /api/v1/auth/registro/prestador` y `GET /api/v1/auth/paises` (los tres últimos, públicos).
- **Administrar cuentas (RF28, RF22, RF62):** `/api/v1/auth/admin/**`, solo el rol Administrador (contrato en `docs/sad/api/ms-auth.md`): listar prestadores (`estado`, `q`) y turistas, detalle del prestador, `POST …/prestadores/{id}/validacion` (Aprobar, o Rechazar con justificación de 1 a 500 caracteres), `…/inactivacion`, `…/turistas/{id}/desactivacion` y `…/reactivacion`. Páginas de 10, `pagina` desde 0. Cada cambio es un UPDATE condicional del estado y la versión esperados más `version_cuenta + 1` y el INSERT del evento `CuentaActualizada`, **en una transacción** (DEC-26); si el estado ya cambió, 409 `ESTADO_CAMBIADO` (DEC-39).
- **Relevo del outbox** (`RelevoCuentasProgramador`, `samaria360.outbox.cada`, 1 s; un hilo por destino): toma las entregas con `FOR UPDATE SKIP LOCKED` (solo la última versión por usuario), las envía a `PUT /internal/v1/cuentas/{id}` de ms-catalog y ms-booking (`CATALOG_INTERNO_URI` y `BOOKING_INTERNO_URI`, en Compose) con un **JWT de servicio** (`sub = ms-auth`, `rol = SERVICIO`, `aud` = el destino, `exp` de 5 min) y reintenta con espera creciente; un destino caído no bloquea al otro. El supuesto de orden está en ADR-008.
- **Registro (RF01, RF02):** respuesta uniforme `202 {"estado":"RECIBIDO"}`, exista o no el correo, con BCrypt siempre y sin inicio de sesión automático ([ADR-006](docs/sad/adr/ADR-006-registro-sin-enumeracion.md)). `DATOS_INVALIDOS` lleva en `campos` un código por campo, con los nombres JSON (nunca textos). Decisiones D1 a D6 en `docs/trazabilidad.md`.
- `Token_Recuperacion` guarda solo `token_hash` (SHA-256), nunca el token en claro.

## ms-catalog (MongoDB `catalog_db`)

- Coordenadas GeoJSON en orden **[longitud, latitud]**; índice `2dsphere`.
- `BigDecimal` → `@Field(targetType = FieldType.DECIMAL128)`. En scripts mongosh usar `NumberInt(...)` y `NumberDecimal(...)`.
- La búsqueda pública devuelve solo `estadoPublicacion = "Publicado"` y `prestadorActivo = true`. Página de máximo 10 elementos (RF16). El detalle (`GET /api/v1/catalogo/servicios/{id}`) aplica la misma visibilidad: si no, 404 `OFERTA_NO_DISPONIBLE`.
- **Alojamiento con unidades** (D1 de «Reservar servicio», `docs/trazabilidad.md`):
  - `detalleCategoria.unidades` tiene 1..N unidades: `idUnidad` (ObjectId, estable), `nombre`, `tipoHabitacion`, `numHabitaciones`, `capacidadMaxHuespedes` y `precioNoche`; `serviciosIncluidos` es del alojamiento;
  - una reserva es de una sola unidad;
  - `precioBase` es el precio «desde»: el menor `precioNoche`. Lo deriva el dominio al guardar y lo exige el `$expr` del validador;
  - huéspedes, tipo de habitación y precio máximo los debe cumplir **una misma unidad** (`$elemMatch` y `PoliticaBusqueda`).
- **Esquema y migraciones de catalog_db:**
  - `infra/mongo/init/` (01, 02, 03 y `*.v1.schema.json`) es la **línea base congelada** (formato v1): no se vuelve a editar;
  - cualquier cambio de esquema o de datos va como una **migración M nueva** en `infra/mongo/migraciones/`, con **su propia copia del validador**; nunca se edita una migración ya aplicada;
  - `esquema/` se aplica siempre y `semilla/` solo en la demo (`MONGO_SEMILLA=true`); la numeración es compartida;
  - las aplica el servicio de un solo uso `mongo-migraciones` (`aplicar.js`, registro en la colección `migraciones`), igual en un volumen nuevo que en uno existente;
  - `docs/sad/modelo-nosql/*.schema.json` describe la última versión; `MigracionesMongoTest` lo verifica;
  - el `oneOf` de `detalleCategoria` discrimina por `categoria` en el nivel superior.
- **Proyección `cuentas`** (M003; `docs/sad/modelo-nosql/cuentas.schema.json`, ADR-008): el estado y la versión de las cuentas de **todos** los roles. `PUT /internal/v1/cuentas/{id}` solo acepta un JWT con `rol = SERVICIO` y `aud = ms-catalog` (403 con un token de usuario, 401 con otra `aud`); *upsert* idempotente que solo avanza si `version` es mayor (204 también si se ignora). Si la cuenta es de un Prestador, primero fija `servicios.prestadorActivo = (estado == Activo)` (índice `prestador`, M004) y después guarda la proyección; si algo falla, el relevo reintenta. Dos `SecurityFilterChain`: `/internal/**` y la pública (con el validador de `ver`).
- **Contenido público** (contrato en `docs/sad/api/ms-catalog.md`; DEC-60 a DEC-69):
  - **Públicos, sin token:** `GET /api/v1/catalogo/faq` (todas, sin paginar, por título con colación `es` nivel 1) y `GET /api/v1/catalogo/avisos` (los activos, del más reciente al más antiguo).
  - **Solo el Administrador** (`/api/v1/catalogo/admin/**`, con el validador de `ver`; otro rol, 403 `ACCESO_DENEGADO`):
    - `POST`, `PUT /{id}` y `DELETE /{id}` de `/admin/faq`;
    - `GET /admin/avisos` (páginas de 10, `pagina` desde 0), `POST /admin/avisos` y `POST /admin/avisos/{id}/despublicacion`.
  - **Colección `faq`:** `titulo` de 1 a 150 y `cuerpo` de 1 a 2000, recortados con `strip()` y contados en puntos de código; tope de 200 (409 `LIMITE_FAQ`, sin atomicidad); borrado físico; el PUT no tiene control de concurrencia.
  - **Colección `avisos`:** `mensaje` de 1 a 300, `tipo` Clima, Cierre o Comunicado, `activo` y `fechaPublicacion`. Sin vigencia automática y sin edición ni borrado. Despublicar es un UPDATE condicional: si ya estaba inactivo, 409 `ESTADO_CAMBIADO` con `estadoActual: Inactivo`; si no existe, 404.
  - **Ids:** un id que no es un ObjectId es 404 (`FAQ_NO_ENCONTRADA`, `AVISO_NO_ENCONTRADO`).
  - **Esquema:** M005 (validadores e índices) y M006 (semilla).
  - **Hora:** la de `fechaActualizacion` y `fechaPublicacion` sale de `RelojPort`.

## ms-booking (Java 17, Spring Boot 3.5.16, PostgreSQL `reservas_db`)

- Misma estructura hexagonal, ArchUnit y ProblemDetail que ms-auth; además, `io.github.resilience4j..` solo en `infrastructure`. Decisiones D1 a D10 en `docs/trazabilidad.md` («Reservar servicio»), [ADR-007](docs/sad/adr/ADR-007-reservas-y-catalogo.md) y el contrato en `docs/sad/api/ms-booking.md`.
- Flyway y `ddl-auto=validate`, con las mismas reglas de migraciones que ms-auth. La reserva es una entidad JPA que se inserta con `EntityManager.persist` + `flush` (el uuid lo asigna el dominio; nunca `save()`); el inventario usa SQL nativo (`JdbcTemplate`), sin entidades.
- **Concurrencia (D3):** UPDATE condicional + CHECK, un día por sentencia, en orden de fecha y hora, dentro de una transacción. Nunca cargar filas de inventario para modificarlas en memoria.
- **Catálogo (D4):** llamada al detalle público de ms-catalog **antes** de abrir la transacción (`ReservarServicioTransaccional`); timeouts de 500 ms y 1,5 s, un reintento solo ante red o 5xx, circuit breaker `catalogo`. Consultar, cancelar y la disponibilidad no llaman al catálogo.
- **Idempotencia (D6):** `Idempotency-Key` obligatoria en `POST /api/v1/reservas`; el reenvío responde 201 con `Idempotent-Replayed: true`. El frontend guarda la clave del intento en `sessionStorage`, asociada al cuerpo exacto, y la borra con una respuesta definitiva (201 o 4xx).
- Fechas en `America/Bogota`; la hora actual sale de `RelojPort`. RF44: cancelar hasta 24 h antes del inicio, límite incluido; `cancelableHasta` lo calcula el servidor y viaja en todas las respuestas de una reserva.
- **Mis reservas (RF12, turista):** `GET /api/v1/reservas/mias` con `desde`, `hasta` y `estado` (uno o varios, separados por coma).
  - Regla del cruce: la actividad aparece si `fecha_inicio < (hasta + 1 día) 00:00` y `fecha_fin > desde 00:00`, en Bogotá.
  - Máximo 366 días; orden `fecha_inicio` descendente con `id_reserva` como desempate.
- **RF52:** `CompletarReservasProgramador` pasa de Confirmada a Completada las reservas con `fecha_fin` vencida, con un UPDATE condicional idempotente, al arrancar y cada 15 minutos (`samaria360.completar-reservas.cada`).
- **Proyección `cuenta`** (V4, ADR-008): `PUT /internal/v1/cuentas/{id}` con el mismo contrato y el mismo validador de `ver` que ms-catalog (`aud = ms-booking`); sin eventos que publicar (D7 precisada en ADR-008). Consultar, cancelar y la disponibilidad no cambian ni llaman al catálogo.
- **Índices** (además de los de V1): V2 `ix_reserva_turista_inicio` (`id_turista, fecha_inicio DESC, id_reserva DESC`, para `/mias`) y V3 `ix_reserva_confirmada_fin` (parcial, `fecha_fin WHERE estado = 'Confirmada'`, para RF52).
- El generador de inventario demo (D5) solo corre con `INVENTARIO_DEMO_HABILITADO=true` (Docker Compose). Es un sustituto temporal de RF19.

## API Gateway (Spring Cloud Gateway 4.3, WebFlux)

- Único punto de entrada (diagrama de despliegue del SAD). Spring Cloud 2025.0.3 sobre el mismo parent de Boot; starter `spring-cloud-starter-gateway-server-webflux` y propiedades con el prefijo `spring.cloud.gateway.server.webflux.*`.
- Prefijos de rutas: `/api/v1/auth/**`, `/api/v1/catalogo/**`, `/api/v1/reservas/**`, `/api/v1/chatbot/**`, `/api/v1/analytics/**`. Implementados: auth (`AUTH_URI`), catálogo (`CATALOG_URI`) y reservas (`BOOKING_URI`); las rutas llegan sin reescribir. CORS permite `Idempotency-Key` y expone `Idempotent-Replayed` y `Location`.
- **No valida el JWT**: reenvía `Authorization` sin cambios y cada servicio lo valida con el JWKS de ms-auth (ADR-005). No agregar Spring Security al gateway.
- **No enruta `/internal/**`** (los endpoints internos de ms-catalog y ms-booking solo los alcanza el relevo de ms-auth, ADR-008): no agregar una ruta que los exponga. Lo prueba `EnrutamientoTest`.
- `RutaSeguraWebFilter` rechaza rutas no normalizadas (`.`, `..`, `%2e`, `%2f`, `%5c`, `\`) con 400 `RUTA_INVALIDA` antes de enrutar.
- Resiliencia: 2 s de conexión, 5 s de respuesta, circuit breaker por ruta (Resilience4j, solo 502/503/504 lo abren; en reservas, solo 502/504, para que el 503 propio de ms-booking con `servicio: ms-catalog` llegue tal cual) con fallback ProblemDetail 503 `codigo: SERVICIO_NO_DISPONIBLE` y `servicio`.
- Transversal: CORS global (`CORS_ORIGENES`) y `X-Correlation-Id` (se genera si falta o no es válido, se reenvía y se devuelve).
- **No tiene dominio: no aplica la estructura hexagonal ni ArchUnit.** Paquete `co.edu.unimagdalena.samaria360.gateway` con `config/`.
- Pruebas: `@SpringBootTest` + `WebTestClient` con servicios simulados sobre Reactor Netty `HttpServer` (ya incluido; sin librerías extra).

## Puertos

| Componente | Puerto | Nota |
|---|---|---|
| frontend (nginx, Docker Compose) | `FRONTEND_PORT` (8000) | Sirve la SPA y pasa `/api/` a api-gateway (un solo origen) |
| api-gateway | 8080 | Único punto de entrada a los servicios; publicado en el host |
| ms-auth | 8081 | En Compose no se publica: solo red interna (`http://ms-auth:8081`) |
| ms-catalog | 8082 | En Compose no se publica: solo red interna (`http://ms-catalog:8082`) |
| ms-booking | 8083 | En Compose no se publica: solo red interna (`http://ms-booking:8083`) |
| frontend (Vite, desarrollo) | 5173 | Proxy `/api` → 8080 |
| PostgreSQL / MongoDB | `POSTGRES_PORT` / `MONGO_PORT` | Definidos en `.env`; publicados solo en 127.0.0.1 |

## Pruebas (obligatorias en cada cambio)

- `domain`: pruebas unitarias puras, sin Spring.
- `application`: casos de uso con dobles (fakes) de los puertos.
- `infrastructure`: Testcontainers contra PostgreSQL/MongoDB reales.
- `architecture`: reglas ArchUnit de capas.
- Antes de dar algo por terminado: `./mvnw test` debe pasar.

## Comandos

```bash
docker compose up -d --build    # sistema completo → http://localhost:8000 (ver README, «Arranque en un comando»)
docker compose up -d postgres-bases mongo-migraciones   # solo las bases (y sus migraciones), para desarrollar con ./mvnw y Vite
cd ms-auth && ./mvnw test       # pruebas
cd ms-auth && ./mvnw spring-boot:run
cd ms-catalog && ./mvnw spring-boot:run
cd ms-booking && ./mvnw spring-boot:run
cd api-gateway && ./mvnw spring-boot:run   # http://localhost:8080
cd frontend && npm run dev                 # http://localhost:5173
```
## Diseño visual del frontend

Los wireframes (`docs/wireframes/`) son una guía de **estructura**, no de estética.

- **Estructura (obligatoria, trazable):** qué elementos hay en cada pantalla, cómo se agrupan en regiones (encabezado, tarjeta de login, filtros, mapa, resultados, pie…) y el flujo entre pantallas. No se elimina ni se fusiona un elemento del wireframe.
- **Diseño (libertad total):** colores, tipografía, espaciado, proporciones, distribución dentro de cada región, ilustraciones, iconografía, animaciones y estados (hover, foco, carga). Se busca una apariencia profesional y memorable, no la de un wireframe.
- **Identidad visual:** inspirada en Santa Marta (mar Caribe, Sierra Nevada, café, atardecer). Debe ser coherente en todas las pantallas: los tokens (colores, tipografía, espaciado, radios, sombras) viven en `frontend/src/app/estilos.css` y se documentan en la sección "Guía visual" de `frontend/README.md`. Toda pantalla nueva reutiliza esos tokens.
- **Elementos nuevos** que no están en el wireframe (p. ej. una ilustración o un mensaje de bienvenida) se permiten si son decorativos o mejoran la experiencia. Si tienen función, se registran en `docs/trazabilidad.md` como "Mejora de diseño".
- **Imágenes del catálogo:** las fotos de `_entrada/imagenes/` no tienen autor, fuente ni licencia verificados. Se integran igual porque el prototipo solo se ejecuta en local con fines académicos. No se inventan créditos. La portada del inicio y el logo usan las imágenes de `_entrada/imagenes/` (`ambiente/` y `marca/`), optimizadas con `frontend/scripts/optimizar-imagenes.mjs` (nunca se amplían); los SVG propios (`MarcaSamaria` y las ilustraciones de categoría) quedan como respaldo. El login no lleva imagen, como WF-01. La foto con texto encima se oscurece de forma uniforme con mar-900 al `--opacidad-velo-texto` y el texto es blanco y grande (WCAG: 3:1); `tokens.test.ts` verifica el peor caso (sobre un píxel blanco). Contraste WCAG AA, foco visible y diseño responsivo desde 360 px.
- **Única excepción de recurso externo: las teselas de OpenStreetMap** del mapa de WF-05 (`https://tile.openstreetmap.org/{z}/{x}/{y}.png`, en `features/catalogo/componentes/MapaOferta.tsx`). Política de uso obligatoria: atribución «© OpenStreetMap contributors» siempre visible (no ocultar el control de atribución); zoom máximo 19; sin descarga masiva, prefetching ni caché offline (Leaflet solo pide las teselas del área visible, sin plugins de caché); `Referer` por defecto del navegador. Para un despliegue con tráfico real hay que usar un proveedor de teselas propio o contratado. El mapa usa **Leaflet directo** (BSD-2-Clause); no usar react-leaflet (licencia Hippocratic-2.1).

## No hacer

- No ejecutar comandos de Git que modifiquen el repositorio (commit, push, merge, rebase, crear o cambiar de rama). El desarrollador gestiona Git; al terminar, resume qué archivos cambiaste. Solo puedes usar git status y git diff para consultar.
- No subir `.env` ni nada de `infra/keys/` (salvo su README).
- No modificar `docs/` (SRS, SAD, diagramas) sin pedir confirmación.
- No agregar dependencias al `pom.xml` sin justificarlas.
- Nunca ejecutar `docker compose down -v` sin permiso explícito: borra los volúmenes con los datos.
- No implementar integraciones con microservicios que todavía no existen.

## Modelo de datos (leer antes de tocar bases de datos)
- El modelo conceptual y el modelo lógico (`docs/sad/diagramas/modelo-logico.dbml`) están **unificados en un solo diagrama por petición del profesor**. Esa unificación es un requisito académico: no representa una base única. No propongas separarlos ni simplificarlos.
- En la arquitectura son **dos microservicios con dos bases PostgreSQL distintas**:
  - **ms-auth → `auth_db`**: `rol`, `usuario`, `turista`, `prestador`, `gestor`, `administrador`, `token_recuperacion`, `turista_categoria_interes`, `outbox_evento`, `outbox_entrega`.
  - **ms-booking → `reservas_db`**: `reserva`, `inventario_cupo`, `reserva_cupo_dia`, `cuenta` (proyección del estado de las cuentas: la llena el endpoint interno que llama el relevo de ms-auth; ms-auth nunca escribe en `reservas_db`; la colección equivalente de `catalog_db` es `cuentas`).
- Ningún servicio lee ni escribe la base de otro. No crees tablas de reservas en `auth_db` ni tablas de autenticación en `reservas_db`.
- El servicio de un solo uso `postgres-bases` (Compose) crea en cada arranque las bases que falten (`infra/postgres/asegurar-bases.sql`), también en volúmenes creados antes; `infra/postgres/init/` solo corre con el volumen vacío.
- Las relaciones del diagrama que cruzan bases son **referencias lógicas sin llave foránea física**, y la integridad la valida la aplicación:
  - `Reserva.id_turista` → `usuario.id_usuario` de `auth_db` (`uuid`, tomado del claim `sub` del JWT);
  - `id_servicio_mongo` → `_id` del documento en `catalog_db` (MongoDB).
- El catálogo vive en MongoDB (`catalog_db`, ms-catalog), con los esquemas de `docs/sad/modelo-nosql/`.
- Pendiente conocido, que no se corrige sin un pedido explícito: ms-auth y ms-booking se conectan con el superusuario de PostgreSQL, y ms-catalog con `root` de MongoDB.