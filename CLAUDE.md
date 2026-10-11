# Samaria360 — Reglas para agentes de código

Plataforma de turismo de Santa Marta. Proyecto Capstone Design (Arquitectura de Software, Universidad del Magdalena).

## Contexto de proceso

- Metodología **RUP**, fase de **Elaboración**. Este código es el **prototipo arquitectónico ejecutable** que valida la línea base de la arquitectura; no es un producto completo.
- Usar solo terminología RUP: fases, iteraciones, casos de uso, requisitos funcionales (RF), requisitos no funcionales (RNF). No usar terminología de métodos ágiles.
- **Alcance de esta iteración:** inicio de sesión (`ms-auth`) y vista principal del catálogo (`ms-catalog`).
- **Fuera de alcance:** registro de usuarios, reservas, chatbot real, notificaciones, analítica. No implementarlos aunque parezcan fáciles.
- Si una regla de negocio no está definida en el SRS (`docs/srs/`), **preguntar**; no inventarla.
- Fuente única de requisitos: `docs/srs/especificacion de requisitos.pdf`. Modelo de datos: `docs/sad/diagramas/modelo-logico.dbml`. Ignorar las carpetas `Documentos extras/`, `DocumentosEntregable1/` y `Diagramas de casos de uso/`, que contienen versiones anteriores.

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

- Esquema con **Flyway** en `src/main/resources/db/migration` (`V1__...sql`). `spring.jpa.hibernate.ddl-auto=validate`; nunca `update` ni `create`.
- Tablas de `auth_db` (modelo lógico del SAD): `Rol`, `Usuario`, `Turista`, `Turista_Categoria_Interes`, `Prestador`, `Gestor`, `Administrador`, `Token_Recuperacion`. Las tablas de reservas pertenecen a `reservas_db` y **no** van en ms-auth.
- Identificadores de usuario: tipo nativo PostgreSQL `uuid` ↔ `java.util.UUID`.
- `estado_cuenta` es un enum de PostgreSQL (`CREATE TYPE`). Mapear con `@Enumerated(EnumType.STRING)` + `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`.
- Fechas con hora: `timestamptz` ↔ `Instant`. Obtener la hora actual siempre a través de un `RelojPort`, para poder probar el bloqueo sin esperar 15 minutos.
- **RF15 (login):** 3 intentos fallidos consecutivos → `bloqueado_hasta = ahora + 15 min`; un login exitoso reinicia `intentos_fallidos`. Una cuenta con `estado_cuenta` distinto de Activo no puede iniciar sesión (RF22, RF62).
- Respuestas: credenciales inválidas → 401 con el **mismo mensaje** para correo inexistente o contraseña incorrecta; cuenta bloqueada → 423; cuenta inactiva → 403.
- **RNF05:** contraseñas con BCrypt (`spring-security-crypto`), detrás de `PasswordHasherPort`.
- **JWT RS256** con `NimbusJwtEncoder`. Claims: `sub` = id_usuario, `rol`, `exp`. La clave pública se publica en `GET /api/v1/auth/.well-known/jwks.json`. Las llaves se leen de las rutas en `JWT_LLAVE_PRIVADA` / `JWT_LLAVE_PUBLICA` (ver `.env.example`).
- Endpoints del prototipo: `POST /api/v1/auth/login`, `GET /api/v1/auth/me`.
- `Token_Recuperacion` guarda solo `token_hash` (SHA-256), nunca el token en claro.

## ms-catalog (MongoDB `catalog_db`)

- Coordenadas GeoJSON en orden **[longitud, latitud]**; índice `2dsphere`.
- `BigDecimal` → `@Field(targetType = FieldType.DECIMAL128)`. En scripts mongosh usar `NumberInt(...)` y `NumberDecimal(...)`.
- La búsqueda pública devuelve solo `estadoPublicacion = "Publicado"` y `prestadorActivo = true`. Página de máximo 10 elementos (RF16).
- Los validadores `$jsonSchema` viven en `infra/mongo/init/`; el `oneOf` de `detalleCategoria` discrimina por `categoria` en el nivel superior.

## API Gateway

Prefijos de rutas: `/api/v1/auth/**`, `/api/v1/catalogo/**`, `/api/v1/reservas/**`, `/api/v1/chatbot/**`, `/api/v1/analytics/**`.

## Pruebas (obligatorias en cada cambio)

- `domain`: pruebas unitarias puras, sin Spring.
- `application`: casos de uso con dobles (fakes) de los puertos.
- `infrastructure`: Testcontainers contra PostgreSQL/MongoDB reales.
- `architecture`: reglas ArchUnit de capas.
- Antes de dar algo por terminado: `./mvnw test` debe pasar.

## Comandos

```bash
docker compose up -d            # PostgreSQL y MongoDB
cd ms-auth && ./mvnw test       # pruebas
cd ms-auth && ./mvnw spring-boot:run
```

## No hacer

- No ejecutar comandos de Git que modifiquen el repositorio (commit, push, merge, rebase, crear o cambiar de rama). El desarrollador gestiona Git; al terminar, resume qué archivos cambiaste. Solo puedes usar git status y git diff para consultar.
- No subir `.env` ni nada de `infra/keys/` (salvo su README).
- No modificar `docs/` (SRS, SAD, diagramas) sin pedir confirmación.
- No agregar dependencias al `pom.xml` sin justificarlas.
