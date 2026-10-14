# Samaria360 — Reglas para agentes de código

Plataforma de turismo de Santa Marta. Proyecto Capstone Design (Arquitectura de Software, Universidad del Magdalena).

## Idioma

- Responde siempre en **español**: planes, resúmenes, preguntas y explicaciones.
- El código, los comandos, los nombres de archivos y los términos técnicos sin traducción establecida (por ejemplo, `fitBounds`, *commit*, *bundle*) pueden quedar como están.

## Contexto de proceso

- Metodología **RUP**, fase de **Elaboración**. Este código es el **prototipo arquitectónico ejecutable** que valida la línea base de la arquitectura; no es un producto completo.
- Usar solo terminología RUP: fases, iteraciones, casos de uso, requisitos funcionales (RF), requisitos no funcionales (RNF). No usar terminología de métodos ágiles.
- **Alcance de esta iteración:** inicio de sesión y registro de turistas y prestadores (`ms-auth`: RF15, RF01, RF02) y vista principal del catálogo (`ms-catalog`).
- **Fuera de alcance:** reservas, chatbot real, notificaciones (el aviso por correo del registro llega con RF34), analítica. No implementarlos aunque parezcan fáciles.
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
- Tablas de `auth_db` (modelo lógico del SAD): `Rol`, `Usuario`, `Turista`, `Turista_Categoria_Interes`, `Prestador`, `Gestor`, `Administrador`, `Token_Recuperacion`. Las tablas de reservas pertenecen a `reservas_db` y **no** van en ms-auth.
- Identificadores de usuario: tipo nativo PostgreSQL `uuid` ↔ `java.util.UUID`.
- `estado_cuenta` es un enum de PostgreSQL (`CREATE TYPE`). Mapear con `@Enumerated(EnumType.STRING)` + `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`.
- Fechas con hora: `timestamptz` ↔ `Instant`. Obtener la hora actual siempre a través de un `RelojPort`, para poder probar el bloqueo sin esperar 15 minutos.
- **RF15 (login):** 3 intentos fallidos consecutivos → `bloqueado_hasta = ahora + 15 min`. Orden (decisión D3, `docs/trazabilidad.md`):
  1. **solo el 423** (cuenta bloqueada) va antes de verificar la contraseña;
  2. contraseña incorrecta → 401 con el **mismo mensaje** que un correo inexistente, y suma un intento, sea cual sea el estado de la cuenta;
  3. con la contraseña correcta: 403 `CUENTA_INACTIVA` si `estado_cuenta` no es Activo (RF22, RF62); 403 `CUENTA_PENDIENTE_VALIDACION` o `CUENTA_RECHAZADA` si es un prestador sin validar (RF02, RF28). En esos 403 no se emite token y el conteo vuelve a 0;
  4. el acceso solo se registra al emitir el token.
- **RNF05:** contraseñas con BCrypt (`spring-security-crypto`), detrás de `PasswordHasherPort`.
- **JWT RS256** con `NimbusJwtEncoder`. Claims: `sub` = id_usuario, `rol`, `exp`. La clave pública se publica en `GET /api/v1/auth/.well-known/jwks.json`. Las llaves se leen de las rutas en `JWT_LLAVE_PRIVADA` / `JWT_LLAVE_PUBLICA` (ver `.env.example`).
- Endpoints del prototipo: `POST /api/v1/auth/login`, `GET /api/v1/auth/me`, `POST /api/v1/auth/registro/turista`, `POST /api/v1/auth/registro/prestador` y `GET /api/v1/auth/paises` (los tres últimos, públicos).
- **Registro (RF01, RF02):** respuesta uniforme `202 {"estado":"RECIBIDO"}`, exista o no el correo, con BCrypt siempre y sin inicio de sesión automático ([ADR-006](docs/sad/adr/ADR-006-registro-sin-enumeracion.md)). `DATOS_INVALIDOS` lleva en `campos` un código por campo, con los nombres JSON (nunca textos). Decisiones D1 a D6 en `docs/trazabilidad.md`.
- `Token_Recuperacion` guarda solo `token_hash` (SHA-256), nunca el token en claro.

## ms-catalog (MongoDB `catalog_db`)

- Coordenadas GeoJSON en orden **[longitud, latitud]**; índice `2dsphere`.
- `BigDecimal` → `@Field(targetType = FieldType.DECIMAL128)`. En scripts mongosh usar `NumberInt(...)` y `NumberDecimal(...)`.
- La búsqueda pública devuelve solo `estadoPublicacion = "Publicado"` y `prestadorActivo = true`. Página de máximo 10 elementos (RF16).
- Los validadores `$jsonSchema` viven en `infra/mongo/init/`; el `oneOf` de `detalleCategoria` discrimina por `categoria` en el nivel superior.

## API Gateway (Spring Cloud Gateway 4.3, WebFlux)

- Único punto de entrada (diagrama de despliegue del SAD). Spring Cloud 2025.0.3 sobre el mismo parent de Boot; starter `spring-cloud-starter-gateway-server-webflux` y propiedades con el prefijo `spring.cloud.gateway.server.webflux.*`.
- Prefijos de rutas: `/api/v1/auth/**`, `/api/v1/catalogo/**`, `/api/v1/reservas/**`, `/api/v1/chatbot/**`, `/api/v1/analytics/**`. Implementados: auth (`AUTH_URI`) y catálogo (`CATALOG_URI`); las rutas llegan sin reescribir.
- **No valida el JWT**: reenvía `Authorization` sin cambios y cada servicio lo valida con el JWKS de ms-auth (ADR-005). No agregar Spring Security al gateway.
- Resiliencia: 2 s de conexión, 5 s de respuesta, circuit breaker por ruta (Resilience4j, solo 502/503/504 lo abren) con fallback ProblemDetail 503 `codigo: SERVICIO_NO_DISPONIBLE` y `servicio`.
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
docker compose up -d postgres mongo   # solo las bases, para desarrollar con ./mvnw y Vite
cd ms-auth && ./mvnw test       # pruebas
cd ms-auth && ./mvnw spring-boot:run
cd ms-catalog && ./mvnw spring-boot:run
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

## Modelo de datos (leer antes de tocar bases de datos)
- El modelo conceptual y el modelo lógico (`docs/sad/diagramas/modelo-logico.dbml`) están **unificados en un solo diagrama por petición del profesor**. Esa unificación es un requisito académico: no representa una base única. No propongas separarlos ni simplificarlos.
- En la arquitectura son **dos microservicios con dos bases PostgreSQL distintas**:
  - **ms-auth → `auth_db`**: `rol`, `usuario`, `turista`, `prestador`, `gestor`, `administrador`, `token_recuperacion`, `turista_categoria_interes`.
  - **ms-booking (planificado) → `reservas_db`**: `reserva`, `inventario_cupo`, `reserva_cupo_dia`.
- Ningún servicio lee ni escribe la base de otro. No crees tablas de reservas en `auth_db` ni tablas de autenticación en `reservas_db`.
- Las relaciones del diagrama que cruzan bases son **referencias lógicas sin llave foránea física**, y la integridad la valida la aplicación:
  - `Reserva.id_turista` → `usuario.id_usuario` de `auth_db` (`uuid`, tomado del claim `sub` del JWT);
  - `id_servicio_mongo` → `_id` del documento en `catalog_db` (MongoDB).
- El catálogo vive en MongoDB (`catalog_db`, ms-catalog), con los esquemas de `docs/sad/modelo-nosql/`.
- Pendiente conocido, que no se corrige sin un pedido explícito: ms-auth y ms-catalog se conectan con las credenciales de superusuario de PostgreSQL y `root` de MongoDB.