# ADR-007 — Reservas: inventario propio y consulta síncrona al catálogo

- **Estado:** Aceptada
- **Fecha:** 2026-10-15
- **Fase:** Construcción (RUP)
- **Componentes:** `ms-booking` (`reservas_db`) y `ms-catalog` (`GET /api/v1/catalogo/servicios/{id}`)
- **Requisitos:** RF10, RF11, RF18, RF20, RF32, RF37, RF44, RNF01, RNF02, RNF03

## Contexto

Para reservar (RF20) hay que saber:
- si la oferta existe y está Publicada, de un prestador activo (RF16, RF22);
- su precio (RF37);
- la capacidad de la **unidad** elegida (RF10; un alojamiento tiene varias unidades, decisión D1 de `docs/trazabilidad.md`).

Esos datos son de ms-catalog (MongoDB). Los cupos de cada día (RF11) no existen en el catálogo. El prototipo no tiene mensajería. Y el principio Zero-Trust del Documento Entregable 2 exige que ninguna petición se acepte solo por venir de la red interna.

## Opciones

| Opción | Descripción |
|---|---|
| **(a)** | ms-booking llama directamente a ms-catalog por HTTP, con circuit breaker. |
| **(b)** | La misma llamada, pero a través del API Gateway. |

## Decisión: (a)

- **Inventario en `reservas_db`, dueño ms-booking.** Una fila por unidad y noche (alojamiento) o por horario (tour). Es la fuente de verdad de lo que queda; la capacidad configurada sigue en el catálogo. Los cupos solo cambian al reservar o cancelar, con UPDATE condicionales sobre una fila a la vez, en orden de fecha.
- **Consulta al endpoint público de detalle** (`GET /api/v1/catalogo/servicios/{id}`), el mismo que usará el frontend. Solo devuelve servicios Publicados de prestadores activos. No hay endpoint interno, ni JWT propagado, ni privilegios por estar en la red de Docker.
- **La llamada se hace antes de abrir la transacción.** Nunca hay filas de inventario bloqueadas durante una llamada HTTP; una prueba de integración lo verifica con `lock_timeout`.
- **Tiempos y reintento:**
  - conexión 500 ms y lectura 1,5 s;
  - **un reintento**, solo ante errores de red o respuestas 5xx; nunca ante 404 ni otros 4xx;
  - el reintento va **dentro** de la llamada protegida por el circuit breaker (Resilience4j): el circuito cuenta una sola llamada lógica.
- **Circuit breaker `catalogo`:** se abre si falla la mitad de las últimas 10 llamadas (con al menos 5) y vuelve a probar a los 10 s. Un 404 es una respuesta válida («no se puede reservar») y no cuenta como fallo.
- **El precio lo calcula el servidor y queda congelado en la reserva** (RF32). Si no coincide con el que vio el turista (`precioTotalEsperado`), 409 `PRECIO_CAMBIADO` con el precio vigente (RF37).
- **Disponibilidad en las tarjetas:** la compone el cliente con el resumen público de ms-booking (`/api/v1/reservas/disponibilidad/resumen`); el catálogo no guarda disponibilidad.

## Consecuencias

- **Acoplamiento temporal:** reservar depende de que ms-catalog responda. Con 99 % de disponibilidad cada uno (RNF01), la de «reservar» queda en ≈ 98 %. Se acepta en esta iteración.
- **Latencia máxima de «reservar» con el catálogo lento:** ≈ 4 s con 1 reintento (dos intentos de hasta 0,5 s de conexión más 1,5 s de lectura). Queda dentro de los 5 s de respuesta del API Gateway. Si ms-catalog no escucha (conexión rechazada), la respuesta 503 llega enseguida; con el circuito abierto, sin llamar al catálogo. Si su contenedor está detenido, el nombre `ms-catalog` no resuelve y el DNS de Docker tarda varios segundos en responderlo: por eso ms-booking usa `dns_opt` (`timeout:1`, `attempts:1`) en Docker Compose, y la respuesta llega en ~1–1,5 s.
- **Lo que no depende del catálogo:** cancelar, consultar las reservas propias y la disponibilidad. Responden con lo guardado en `reservas_db`.
- **Dos datos de capacidad con dueños distintos:** la capacidad configurada (catálogo, RF10) y los cupos de cada día (`inventario_cupo`). Un cambio en el catálogo no reescribe el inventario existente: eso le corresponde a RF19.
- **Unidades eliminadas del catálogo:** sus filas de inventario quedan huérfanas y no se pueden reservar (409 `UNIDAD_NO_DISPONIBLE`); las reservas ya hechas se conservan.
- **Errores:** `SERVICIO_NO_DISPONIBLE` (503, `servicio: ms-catalog`) significa lo mismo que en el gateway: el servicio que no responde. Para que llegue tal cual al cliente, el circuito del gateway en la ruta de reservas cuenta solo 502 y 504; si ms-booking no responde, el fallback del gateway sí responde `servicio: ms-booking`.

## Cuándo revisarla

Cuando exista mensajería (ms-notificaciones) o si las pruebas de carga (RNF02, RNF03) muestran que la llamada síncrona pesa.
