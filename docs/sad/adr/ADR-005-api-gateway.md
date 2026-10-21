# ADR-005 — API Gateway como único punto de entrada, sin validar el JWT en el borde

- **Estado:** Aceptada (ampliada por [ADR-008](ADR-008-sincronizacion-estado-cuentas.md): los servicios validan también el claim `ver`)
- **Fecha:** 2026-10-11
- **Fase:** Elaboración (RUP), prototipo arquitectónico ejecutable
- **Componente:** `api-gateway` (Spring Cloud Gateway 4.3.5, WebFlux; Spring Cloud 2025.0.3 sobre Spring Boot 3.5.16)
- **Requisitos:** RNF01 (disponibilidad), RNF02 (tiempo de respuesta), RNF03 (concurrencia), RNF06 (control de acceso por rol)

## Contexto

El diagrama de despliegue del SAD define un API Gateway como único punto de entrada del sistema. El Entregable 2 argumenta que la arquitectura de microservicios aísla los fallos: la caída o lentitud de un servicio no debe arrastrar a los demás.

Hoy hay dos servicios con API:

- **ms-auth** emite los JWT RS256 y publica su JWKS en `/api/v1/auth/.well-known/jwks.json`;
- **ms-catalog** expone la búsqueda pública.

**Ambos validan el JWT** de sus endpoints protegidos con ese JWKS (resource server de Spring Security). Falta decidir qué responsabilidades asume el gateway y, en particular, si debe validar el token.

| Componente | Puerto |
|---|---|
| api-gateway | 8080 |
| ms-auth | 8081 |
| ms-catalog | 8082 |
| frontend (Vite, proxy `/api` → 8080) | 5173 |

## Decisión

El gateway es el **único punto de entrada** y se limita a:

1. **Enrutar** por prefijo, sin reescribir la ruta: `/api/v1/auth/**` → `AUTH_URI` y `/api/v1/catalogo/**` → `CATALOG_URI`, configurables por variables de entorno.
2. **Aislar fallos**:
   - timeouts de **2 s de conexión** y **5 s de respuesta**;
   - **circuit breaker por ruta** con Resilience4j: ventana de 10 llamadas, mínimo de 5 y 50 % de fallos para abrirse, y 10 s en estado abierto. Lo abren solo los errores de conexión, los timeouts y las respuestas 502, 503 o 504. El `TimeLimiter` se fija en 6 s, por encima del timeout de respuesta;
   - **fallback** con un ProblemDetail **503**, `codigo: SERVICIO_NO_DISPONIBLE` y el nombre del `servicio`, en el mismo formato de errores que los servicios.
3. Resolver **aspectos transversales**:
   - **CORS** global (`CORS_ORIGENES`), con `Authorization` permitida;
   - **`X-Correlation-Id`**: se genera si no viene o no es válido, se reenvía al servicio y se devuelve en la respuesta;
   - `/actuator/health` público.
4. **No validar el JWT**: reenvía la cabecera `Authorization` sin cambios. Cada servicio la valida con el JWKS de ms-auth (firma RS256, `iss` y `exp`) y aplica sus reglas de rol (RNF06).

## Alternativa descartada: validar el JWT en el borde

El gateway actuaría como resource server: validaría la firma con el JWKS, el emisor y la expiración, y rechazaría con 401 antes de enrutar. Tiene dos variantes:

- **a)** el gateway valida y los servicios confían en él;
- **b)** el gateway valida y los servicios vuelven a validar.

Se descarta para el prototipo:

- **(a)** convierte al gateway en el único punto de validación. Un servicio alcanzado sin pasar por el gateway, por error de red o de despliegue, quedaría sin protección, y la seguridad dependería de la topología.
- **(b)** duplica la configuración y la latencia de validación sin agregar garantías nuevas, porque los servicios ya validan.
- La búsqueda pública (la mayor parte del tráfico esperado en temporada alta, RNF03) no lleva token: validar en el borde no le aporta nada.

## Consecuencias

**Positivas**

- Es más simple: el gateway no depende de Spring Security ni del JWKS, y ms-auth puede rotar sus llaves sin tocarlo.
- No hay un punto único de validación del que dependa la seguridad (defensa en profundidad): cada servicio protege sus propios endpoints aunque se lo invoque directamente.
- El aislamiento de fallos queda demostrado con pruebas automáticas:
  - con ms-catalog caído o lento, la búsqueda responde 503 `SERVICIO_NO_DISPONIBLE` y **el login sigue funcionando**;
  - con el circuito abierto, el gateway responde sin llamar al servicio.
- Toda petición queda identificada con `X-Correlation-Id`, que permite seguirla en los logs.

**Negativas**

- **No hay rechazo temprano de tokens inválidos o vencidos en el borde**: llegan al servicio y consumen sus recursos antes del 401.
- Cada servicio con endpoints protegidos debe configurar el JWKS y el emisor (`AUTH_JWKS_URI`).
- El gateway no puede aplicar políticas que dependan del usuario, como limitar la tasa por usuario o enrutar según el rol.
- `X-Correlation-Id` llega a los servicios, pero estos todavía no lo registran en sus logs (MDC); queda como pendiente.

## Cuándo revisar esta decisión

- Si las pruebas de carga (RNF02, RNF03) muestran un volumen relevante de peticiones con tokens inválidos llegando a los servicios.
- Si se requiere limitación de tasa o auditoría por usuario en el borde.
- Si se incorporan servicios que no puedan validar el JWT por sí mismos. En ese caso se adoptaría la variante (b), sin quitar la validación en los servicios.

## Nota: despliegue con Docker Compose

En el despliegue con Docker Compose (`docker-compose.yml`), el navegador entra por el contenedor **frontend**
(nginx). nginx sirve la SPA y pasa `/api/` al api-gateway sin reescribir la ruta, como hace el proxy de Vite en
desarrollo, así que el navegador trabaja con un solo origen. La decisión no cambia: nginx no enruta a los
servicios ni valida el JWT, y el gateway sigue siendo el único punto de entrada a ellos.

- En el host se publican solo el frontend (8000) y el gateway (8080).
- ms-auth y ms-catalog solo se alcanzan por la red interna de Docker.
- PostgreSQL y MongoDB se publican solo en 127.0.0.1.
