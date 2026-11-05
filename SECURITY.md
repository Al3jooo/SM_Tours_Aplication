# Política de seguridad

## Alcance

Samaria360 es un **prototipo arquitectónico académico** (RUP, fase de Construcción) que solo se ejecuta en local. No
tiene un despliegue con tráfico real y no debe usarse en producción:

- usa Spring Boot 3.5, sin soporte OSS desde el 30/06/2026 ([ADR-010](docs/sad/adr/ADR-010-linea-spring-boot-3-5.md));
- Docker Compose activa el perfil `dev`, que carga usuarios de prueba con una contraseña conocida (README, «Arranque
  en un comando»); fuera de Compose ese perfil solo se activa si se pide;
- ms-auth y ms-booking se conectan con el superusuario de PostgreSQL, y ms-catalog con `root` de MongoDB (pendiente
  conocido en CLAUDE.md).

Estos puntos son conocidos y no hace falta reportarlos.

## Reportar una vulnerabilidad

No abras un *issue* público. Repórtala en privado desde la pestaña **Security → Report a vulnerability** del
repositorio, con:

- el componente afectado (`frontend`, `api-gateway`, `ms-auth`, `ms-catalog` o `ms-booking`);
- los pasos para reproducirla y su impacto;
- la versión (commit) en que la encontraste.

No incluyas tokens, contraseñas reales ni datos personales en el reporte.

## Prácticas del proyecto

- Contraseñas con BCrypt (RNF05) y JWT RS256 de 15 minutos, validados por cada servicio con el JWKS de ms-auth.
- Los secretos (`.env`, `infra/keys/`) no se versionan; las llaves se montan como *secrets* de Docker de solo lectura.
- El API Gateway no expone los endpoints `/internal/**` y rechaza rutas no normalizadas.
