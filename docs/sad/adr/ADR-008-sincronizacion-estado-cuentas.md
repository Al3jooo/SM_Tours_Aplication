# ADR-008 — Sincronización del estado de las cuentas: outbox, relevo HTTP y versión de sesión

- **Estado:** Aceptada
- **Fecha:** 2026-10-21
- **Fase:** Construcción (RUP)
- **Componentes:** `ms-auth` (`auth_db`), `ms-catalog` (`catalog_db`) y `ms-booking` (`reservas_db`)
- **Requisitos:** RF22, RF28, RF62 (RF45 queda servido por el diseño), RNF01, RNF02, RNF06
- **Registra las decisiones:** DEC-26 y DEC-29

## Contexto

- **RF22** oculta los servicios de un prestador inactivo de las búsquedas públicas. Pero `servicios.prestadorActivo` vive en `catalog_db` y el estado de la cuenta, en `auth_db`. Ningún servicio lee la base de otro.
- **RF62 y RF45** piden que el cambio de estado o de rol se aplique de inmediato. Hasta ahora el JWT era válido hasta su `exp` (60 minutos) y ms-booking no consultaba a ms-auth: un turista desactivado seguía reservando con su token (hallazgo M4).
- [ADR-005](ADR-005-api-gateway.md) deja que cada servicio valide el JWT con el JWKS de ms-auth. [ADR-007](ADR-007-reservas-y-catalogo.md) fija que reservar no depende de ms-auth y que el prototipo no tiene mensajería (D7 de «Reservar servicio»: ms-booking no publica eventos).

## Decisión

1. **Outbox transaccional en `auth_db` (DEC-26).** El cambio de `estado_cuenta` o de `estado_validacion`, `usuario.version_cuenta + 1` y el evento `CuentaActualizada` (`idUsuario`, `rol`, `estadoCuenta`, `version`) van en la misma transacción. El cambio es un UPDATE condicional del estado y la versión esperados (DEC-39): si otro cambio llegó antes, 409 `ESTADO_CAMBIADO`. El `estadoCuenta` del evento es el estado **derivado** (Pendiente, Activo, Rechazado, Inactivo o Desactivado; DEC-43).
2. **Relevo HTTP con entrega por destino.** Una tarea de ms-auth (`samaria360.outbox.cada`, 1 s) toma las entregas pendientes de cada destino con `FOR UPDATE SKIP LOCKED`, las envía a `PUT /internal/v1/cuentas/{id}` de **ms-catalog** y **ms-booking** y reintenta con espera creciente (2 s, 4 s… hasta 60 s). La entrega se registra por destino (`outbox_entrega`): uno caído no bloquea al otro. Autentica con un **JWT de servicio** (RS256, mismo JWKS, `sub = ms-auth`, `rol = SERVICIO`, `aud` = el destino y `exp` de 5 minutos). El API Gateway **no enruta `/internal/**`**. Las URI de los destinos van en variables de Compose, no son secretos.
3. **Proyección `cuentas` con versión, en cada consumidor.** ms-catalog (colección `cuentas`) y ms-booking (tabla `cuenta`, V4) guardan `(id, rol, estado, versión)` de **todos los roles** con un *upsert* que solo avanza si la versión es mayor: tolera duplicados y desorden, y responde 204 se aplique o no. ms-catalog, si la cuenta es de un Prestador, deriva `servicios.prestadorActivo = (estado == Activo)` antes de guardar la proyección; si algo falla, no avanza y el relevo reintenta.
4. **Versión de sesión (DEC-29).** El JWT lleva el claim `ver` (= `version_cuenta` al iniciar sesión). Cada servicio rechaza con **401 `SESION_INVALIDADA`** un token con `ver` menor que la versión conocida. Sin fila en la proyección lo acepta; un token sin `ver` cuenta como 0. ms-auth compara contra su propia tabla (`/me` y los endpoints del Administrador). Los endpoints públicos también validan el Bearer que llegue: un token viejo recibe 401, no se ignora.
5. **TTL del token de 15 minutos** (antes 60): es el techo de la ventana cuando el relevo o un consumidor fallan. Es una decisión de arquitectura, no un parámetro de despliegue: está fijo en `application.yml`.

### Supuesto de orden en ms-catalog

La secuencia de ms-catalog (leer la versión guardada, actualizar `servicios.prestadorActivo` y guardar la proyección) **no es atómica**. Solo es correcta si cada par (destino, usuario) se entrega **de a uno por vez**. Lo garantizan, juntos:

- un hilo por destino (`spring.task.scheduling.pool.size = 2`);
- la entrega de solo la última versión pendiente de cada usuario: las anteriores quedan reemplazadas;
- el arriendo de la entrega reclamada con `FOR UPDATE SKIP LOCKED` (`proximo_intento = ahora + 30 s`).

Si ms-auth se escala a varias instancias, el arriendo debe ser **mayor que el tiempo máximo de una entrega**: 30 s frente a 2 s (0,5 s de conexión y 1,5 s de lectura). Se cumple. Si ese supuesto dejara de valer, la secuencia de ms-catalog tendría que pasar a una sola operación atómica por cuenta.

### Relación con D7 de «Reservar servicio» y con ADR-007

D7 dice: «**Ninguna en esta iteración:** ms-booking no publica eventos». Recibir el estado de las cuentas por HTTP **no la contradice**: no se agrega ningún broker y ms-booking sigue sin publicar. Sí la **precisa**; la redacción nueva (en `docs/trazabilidad.md`):

> **Sin broker de mensajería.** ms-booking no publica eventos (RF13, RF30 y RF63 siguen con ms-notificaciones). Desde «Administrar cuentas» (ADR-008) **recibe** el estado de las cuentas: el relevo del outbox de ms-auth lo entrega por HTTP a `PUT /internal/v1/cuentas/{id}`, autenticado con un JWT de servicio. Es una entrega punto a punto con reintentos, no un bus de eventos. Reservar sigue sin publicar nada y el inventario solo depende de `reservas_db`.

ADR-007 dice «No hay endpoint interno, ni JWT propagado, ni privilegios por estar en la red de Docker» (consulta de ms-booking a ms-catalog). Eso no cambia: la consulta al detalle sigue siendo la pública. Los endpoints internos nuevos mantienen el principio Zero-Trust: **no se confía en la red**, solo en un JWT de servicio con la `aud` del destino.

### Relación con ADR-005

ADR-005 queda **ampliada**: los servicios validan, además de la firma, `iss` y `exp`, el claim `ver`. El gateway sigue sin validar el JWT.

## Alternativas descartadas

- **Aceptar 60 minutos.** Incumple RF62 y la inmediatez de RF45: un turista desactivado reservaría una hora más.
- **TTL corto con *refresh token*.** Necesita un caso de uso nuevo (renovar sesión) y traslada el problema al *refresh*, que también habría que revocar. Queda como evolución.
- **Introspección síncrona en ms-auth.** Vuelve a ms-auth un punto único de fallo de las reservas, contradice ADR-007 y RNF01, y suma latencia a cada petición (RNF02).
- **Broker de mensajería.** Infraestructura nueva sin necesidad en el prototipo (D7).

## Consecuencias

- **Consistencia eventual:** de segundos con todo en marcha (el relevo corre cada 1 s; criterio de 5 s o menos del CU) y de hasta 15 minutos con un consumidor caído.
- **Si se pierde la proyección** de un consumidor, el sistema queda en modo permisivo (acepta tokens) hasta el `exp` del token. No hay reconstrucción automática: se reentregaría un evento.
- **El turista vuelve a iniciar sesión cada 15 minutos** hasta que haya *refresh token*. Con el frontend actual, un 401 (también en peticiones públicas) cierra la sesión y la muestra como «expiró»; la sesión B del frontend debe atenderlo.
- **RF55 (cerrar sesión)** sigue sin revocar el token del dispositivo: solo se descarta en el cliente.
- Un token viejo que llega a un endpoint **público autenticado** recibe 401 (Spring valida cualquier Bearer).
- **El JWT de servicio comparte la llave de firma** con los de usuario; se acepta para el prototipo. Lo separan el `rol = SERVICIO` (la API pública lo rechaza con 401) y la `aud` (el destino lo exige).
- **El outbox crece sin límite:** las filas entregadas no se purgan. No se implementa ahora; queda en «Pendientes» de `docs/trazabilidad.md`.
- Se precisa D7 (arriba) y se amplía ADR-005.
- **Rutas no normalizadas (N8).** El gateway reenvía la ruta sin reescribirla, así que una como `/api/v1/reservas/../../internal/...` coincidiría con el prefijo de reservas y llegaría tal cual a ms-booking. Defensa en dos capas: el gateway la rechaza antes de enrutar con 400 `RUTA_INVALIDA` (`RutaSeguraWebFilter`: segmentos `.` o `..` en la ruta cruda o decodificada, barra invertida, `%2e`, `%2f` o `%5c`; un segmento como `.well-known` sigue siendo válido), y si llega directo a un servicio, el `StrictHttpFirewall` de Spring Security responde 400. Además, `/internal/**` exige el JWT de servicio con la `aud` del destino.

## Cumplimiento

Pruebas que verifican cada punto:

| Punto | Prueba |
|---|---|
| 1. Cambio y evento juntos o ninguno; estado esperado; 409 | `AdminCuentasApiIntegracionTest` (`siFallaElEventoNoQuedaElCambio`, `dosValidacionesSimultaneasDanUn200YUn409`), `AdministrarCuentasServiceTest`, `CuentaAdministradaTest` |
| 2. Relevo por destino, reintento y JWT de servicio | `RelevoCuentasIntegracionTest`, `RelevarCuentasServiceTest`, `PoliticaReintentoTest`; `EnrutamientoTest` (`lasRutasInternasNoSeEnrutanANingunServicio`) |
| 2. El endpoint interno solo acepta el JWT de servicio de su `aud` | `CuentaInternaApiIntegracionTest` de ms-catalog y de ms-booking |
| 3. Proyección idempotente por versión; `prestadorActivo` | `ActualizarCuentaServiceTest`, `CuentaInternaApiIntegracionTest`, `VisibilidadPrestadorApiIntegracionTest`, `MigracionesMongoTest` (colección `cuentas`, índice por prestador) |
| 4. `ver` y `SESION_INVALIDADA` | `SesionVersionApiIntegracionTest` (ms-catalog), `SesionVersionIntegracionTest` (ms-booking), `AdminCuentasApiIntegracionTest` (`/me` y `/admin/**`), `PoliticaSesionTest` |
| 5. Token de 15 minutos | `AdminCuentasApiIntegracionTest` (`elTokenTraeVerYVenceALos15Minutos`) |
| Migraciones V11 a V14 | `MigracionesAdministrarCuentasIntegracionTest` |
| N8. El gateway rechaza las rutas no normalizadas antes de enrutar | `EnrutamientoTest` (`unaRutaConPuntosOSeparadoresCodificadosEs400SinLlegarANingunServicio`, `unSegmentoQueSoloEmpiezaConPuntoSigueSiendoValido`) |
| N8. El servicio rechaza una ruta no normalizada que llegue directo (`StrictHttpFirewall`) | `CuentaInternaApiIntegracionTest` de ms-booking (`unaRutaNoNormalizadaHaciaLosEndpointsInternosEs400`) |

## Cuándo revisarla

- Cuando exista el *refresh token*, el TTL podrá bajar más y esta ventana se reduce.
- Cuando exista mensajería (ms-notificaciones), el relevo podría publicar el evento en ella.
- Si ms-auth se escala a varias instancias (revisar el arriendo) o si se agrega la purga del outbox.
