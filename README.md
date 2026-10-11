# Samaria360

Plataforma de turismo de Santa Marta. Proyecto Capstone Design — Arquitectura de Software, Universidad del Magdalena.

- **Metodología:** RUP. Fase actual: Elaboración (prototipo arquitectónico ejecutable).
- **Arquitectura:** microservicios con diseño hexagonal interno.
- **Persistencia:** PostgreSQL (`auth_db`, `reservas_db`) y MongoDB (`catalog_db`, `analytics_db`).

## Estructura del monorepo

Cada carpeta raíz corresponde a un contenedor del diagrama de despliegue del SAD.

```
samaria360/
├── .github/workflows/            # un workflow por servicio, filtrado por paths:
├── docs/                         # artefactos RUP versionados con el código
│   ├── srs/
│   ├── sad/
│   │   ├── adr/                  # ADR-001-metodologia.md, ADR-002-bd.md, ADR-003-conectividad.md...
│   │   └── diagramas/            # fuentes PlantUML (.puml)
│   ├── casos-de-uso/             # especificaciones textuales
│   └── iteraciones/              # planes y evaluaciones de iteración
├── infra/
│   ├── postgres/init/            # 01-crear-bases.sql (auth_db, reservas_db)
│   ├── mongo/init/               # 01-validadores.js, 02-indices.js, 03-semilla-catalogo.js
│   └── keys/                     # llaves RSA del JWT (no se versionan)
├── frontend/                     # React + Vite + TypeScript
│   └── src/
│       ├── app/                  # router, providers, rutas protegidas por rol
│       ├── features/auth/        # LoginPage, useAuth, authApi.ts
│       ├── features/catalogo/    # CatalogoPage, filtros, tarjetas, mapa (Leaflet)
│       ├── shared/api/           # httpClient.ts (axios + interceptor JWT)
│       ├── shared/components/
│       └── i18n/                 # es.json, en.json
├── api-gateway/                  # Spring Cloud Gateway
│   └── src/main/java/co/edu/unimagdalena/samaria360/gateway/config/
├── ms-auth/                      # Spring Boot 3 + PostgreSQL (auth_db)
│   ├── pom.xml
│   └── src/
│       ├── main/java/co/edu/unimagdalena/samaria360/auth/
│       │   ├── domain/
│       │   │   ├── model/        # Usuario, Rol, EstadoCuenta, TokenRecuperacion
│       │   │   ├── service/      # PoliticaBloqueo (3 intentos → 15 min)
│       │   │   └── exception/
│       │   ├── application/
│       │   │   ├── port/in/      # IniciarSesionUseCase, ConsultarUsuarioActualUseCase
│       │   │   ├── port/out/     # UsuarioRepositoryPort, PasswordHasherPort, TokenProviderPort, RelojPort
│       │   │   └── service/      # IniciarSesionService
│       │   └── infrastructure/
│       │       ├── adapter/in/web/           # AuthController, dto/
│       │       ├── adapter/out/persistence/  # entidades JPA, repositorios, mapper/
│       │       ├── adapter/out/security/     # BCrypt, JWT, JWKS
│       │       └── config/                   # SecurityConfig, BeanConfig
│       ├── main/resources/db/migration/      # V1__esquema_auth.sql, V2__semilla.sql
│       └── test/java/.../auth/               # domain/, application/, infrastructure/, architecture/
├── ms-catalog/                   # Spring Boot 3 + MongoDB (catalog_db), misma estructura hexagonal
├── ms-chatbot/                   # FastAPI
│   ├── app/
│   │   ├── domain/
│   │   ├── application/ports/    # LlmPort, CatalogoPort, ItinerarioRepositoryPort
│   │   ├── application/use_cases/
│   │   └── infrastructure/{api,adapters}/
│   └── tests/
├── docker-compose.yml
├── .env.example
├── .editorconfig
└── .gitignore
```

## Reglas de dependencia (verificadas con ArchUnit)

- `domain` no depende de nada externo: ni Spring, ni JPA, ni Mongo.
- `application` depende solo de `domain`.
- `infrastructure` implementa los puertos de `application`.
- No hay `pom.xml` raíz ni librería compartida: cada servicio se compila y despliega de forma independiente.

## Arranque local

```bash
cp .env.example .env          # y cambiar las contraseñas
docker compose up -d          # PostgreSQL y MongoDB
cd ms-auth
mvn -N wrapper:wrapper        # solo la primera vez: genera mvnw y .mvn/
./mvnw spring-boot:run
```
