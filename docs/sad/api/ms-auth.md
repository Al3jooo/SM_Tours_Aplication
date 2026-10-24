# API de ms-auth

Contrato de ms-auth para el frontend y para las pruebas. Todas las rutas van por el API Gateway (`/api/v1/auth/**`); el servicio escucha en el 8081 solo dentro de la red de Docker. Este documento nace con el CU «Administrar cuentas» (RF28, RF22, RF62; WF-12) y fija los endpoints del **Administrador**, el token y el endpoint interno. Los endpoints de inicio de sesión y registro (RF15, RF01, RF02) están descritos en `docs/trazabilidad.md` («Endpoints de ms-auth»). Decisiones en `docs/trazabilidad.md` («Administrar cuentas — decisiones»), [ADR-008](../adr/ADR-008-sincronizacion-estado-cuentas.md) y el [CU](../../casos-de-uso/CU-administrar-cuentas.md). La iteración B1 agrega el **perfil del turista** (RF53; [CU](../../casos-de-uso/CU-actualizar-perfil.md), «Turista (B1) — decisiones»), y la B2, el **cambio de rol** (RF45; [CU](../../casos-de-uso/CU-cambiar-rol-cuenta.md), «Cambiar rol de cuenta — decisiones»).

## Convenciones

- **Autenticación:** `Authorization: Bearer <JWT de ms-auth>`. Los endpoints de `/admin/**` exigen el rol **Administrador**; `/perfil`, el rol **Turista**; `/me`, cualquier rol.
- **Errores:** ProblemDetail (RFC 9457) con `codigo` estable, como ms-catalog y ms-booking. `DATOS_INVALIDOS` lleva `campos`: un código por campo con el nombre JSON, nunca textos (`REQUERIDO`, `FORMATO`, `LONGITUD_MINIMA`, `LONGITUD_MAXIMA`, `NO_PERMITIDO`, `DUPLICADO`).
- **Fechas:** `aaaa-mm-dd` (el día de registro; el modelo no guarda la hora) e instantes ISO 8601 en UTC.
- **Páginas de 10** (DEC-41), con `pagina` desde 0, como «Mis reservas»: `{elementos, pagina, tamano: 10, totalElementos, totalPaginas}`.

## Token de sesión (cambios de ADR-008)

`POST /api/v1/auth/login` emite un JWT RS256 con:

| Claim | Valor |
|---|---|
| `iss` | `samaria360-auth` |
| `sub` | `id_usuario` (UUID) |
| `rol` | `Turista`, `Prestador`, `Gestor` o `Administrador` |
| `ver` | `usuario.version_cuenta` al iniciar sesión (DEC-29) |
| `iat`, `exp` | `exp - iat` = **15 minutos** (antes, 60) |

`version_cuenta` sube en 1 con cada cambio de estado o de rol de la cuenta (RF45), y V15 la subió una vez para los Gestores y Administradores existentes (DEC-108). Cada servicio rechaza con **401 `SESION_INVALIDADA`** un token con `ver` menor que la versión conocida; un token sin `ver` cuenta como 0. ms-catalog y ms-booking rechazan también un token de Gestor o Administrador sin fila en su proyección (DEC-110). ms-auth compara contra su propia tabla en **todos** sus endpoints autenticados (`/me`, `/perfil` y `/admin/**`). La respuesta de `/login` no cambia (`expiraEn` es ahora 15 minutos después).

## Estados de la cuenta

| Estado | Quién | Significado |
|---|---|---|
| `Pendiente` | Prestador | «Pendiente de Validación» (RF02); no inicia sesión |
| `Activo` | Prestador y turista | Validado (prestador) y con la cuenta activa |
| `Rechazado` | Prestador | Rechazado con justificación (RF28); no inicia sesión |
| `Inactivo` | Prestador | Validado, pero con la cuenta inactiva (RF22); sus servicios no se ven |
| `Desactivado` | Turista | Desactivado temporalmente (RF62); no inicia sesión |

El prestador tiene dos columnas en `auth_db` (`estado_validacion` y `estado_cuenta`); el estado que viaja es el **derivado** (DEC-43), con la precedencia: Pendiente, Rechazado, Activo, Inactivo.

| Transición | Origen → destino |
|---|---|
| Aprobar | Pendiente → Activo |
| Rechazar | Pendiente → Rechazado (con justificación) |
| Inactivar | Activo → Inactivo |
| Desactivar | Activo → Desactivado (turista) |
| Reactivar | Desactivado → Activo (turista) |

Cualquier otra responde **409 `ESTADO_CAMBIADO`** (DEC-39), con `estadoActual`. El prestador Rechazado o Inactivo no se reactiva (DEC-37).

## Prestadores

### Listar

`GET /api/v1/auth/admin/prestadores?estado=&q=&pagina=0`

- `estado` (opcional): `Pendiente`, `Activo`, `Rechazado` o `Inactivo`. Otro valor: 400 `estado: NO_PERMITIDO`.
- `q` (opcional, hasta 100 caracteres): busca, sin distinguir mayúsculas, el texto contenido en el nombre del representante legal, el correo corporativo o el RNT. Los comodines de `LIKE` se escapan: «50%» busca esos caracteres. Más de 100: `q: LONGITUD_MAXIMA`.
- Orden: fecha de registro ascendente y `idUsuario` de desempate. La pestaña «Prestadores por validar» pide `estado=Pendiente`.

```json
{
  "elementos": [
    { "idUsuario": "0000…0006", "representanteLegal": "Marta Rojas", "rnt": "RNT-00002",
      "correoCorporativo": "contacto@kayaktaganga.test", "fechaRegistro": "2026-10-19", "estado": "Pendiente" }
  ],
  "pagina": 0, "tamano": 10, "totalElementos": 2, "totalPaginas": 1
}
```

### Detalle

`GET /api/v1/auth/admin/prestadores/{id}` → `200`:

```json
{
  "idUsuario": "0000…0006", "representanteLegal": "Marta Rojas", "razonSocial": null,
  "rnt": "RNT-00002", "correoCorporativo": "contacto@kayaktaganga.test", "telefono": "+57 3000000002",
  "aceptoPoliticas": true, "fechaRegistro": "2026-10-19", "estado": "Pendiente",
  "motivoRechazo": null, "revisadoEn": null
}
```

`razonSocial` es opcional (decisión D2 de «Registrarse»). `fechaRegistro` es el día del registro y de la aceptación de políticas; no hay hora (D6). `motivoRechazo` y `revisadoEn` los llena la validación. **404 `CUENTA_NO_ENCONTRADA`** si no existe o no es un prestador.

### Validar (RF28)

`POST /api/v1/auth/admin/prestadores/{id}/validacion`

```json
{ "decision": "Aprobar" }
{ "decision": "Rechazar", "justificacion": "El RNT no está vigente" }
```

- `decision`: `Aprobar` o `Rechazar`.
- `justificacion`: obligatoria al rechazar, de 1 a 500 caracteres sin contar los espacios de los extremos (DEC-42); al aprobar se ignora.
- **200**: el detalle del prestador con el estado nuevo. Guarda `revisadoEn`, quién revisó y, si se rechazó, el motivo.
- **400 `DATOS_INVALIDOS`**: `decision: REQUERIDO` o `FORMATO`; `justificacion: REQUERIDO` (vacía o en blanco) o `LONGITUD_MAXIMA`. No cambia nada.
- **404** `CUENTA_NO_ENCONTRADA`. **409** `ESTADO_CAMBIADO` (el prestador ya no está Pendiente).

### Inactivar (RF22)

`POST /api/v1/auth/admin/prestadores/{id}/inactivacion` · sin cuerpo (sin motivo, DEC-38) → **200** con el detalle (`estado: Inactivo`). **404**, **409** (no está Activo; también si ya está Inactivo). No hay reactivación (DEC-37).

## Turistas

`GET /api/v1/auth/admin/turistas?estado=&q=&pagina=0`

- `estado` (opcional): `Activo` o `Desactivado`. `q`: nombre (primer nombre y primer apellido) o correo, con las mismas reglas que en los prestadores.
- Un turista con `estado_cuenta = Inactivo` (dato heredado de antes de esta iteración; Inactivo es el estado de RF22) se lista como `Desactivado`.

```json
{
  "elementos": [
    { "idUsuario": "0000…0008", "nombre": "Marta Lee", "correo": "turista2@samaria360.test",
      "pais": "US", "fechaRegistro": "2026-10-21", "estado": "Activo" }
  ],
  "pagina": 0, "tamano": 10, "totalElementos": 1, "totalPaginas": 1
}
```

`pais` es el código ISO 3166-1 alfa-2 (decisión D5 de «Registrarse»); el frontend pone el nombre.

- `POST /api/v1/auth/admin/turistas/{id}/desactivacion` (RF62) → **200** con el turista (`estado: Desactivado`). **409** si no está Activo.
- `POST /api/v1/auth/admin/turistas/{id}/reactivacion` (RF62) → **200** con el turista (`estado: Activo`). **409** si no está Desactivado.
- Sin cuerpo (sin motivo, DEC-38). **404** `CUENTA_NO_ENCONTRADA` si no existe o no es un turista.

Un turista desactivado recibe **403 `CUENTA_INACTIVA`** al iniciar sesión (con la contraseña correcta) y **401 `SESION_INVALIDADA`** con un token de antes de la desactivación. Un prestador Rechazado recibe 403 `CUENTA_RECHAZADA`; uno Inactivo, 403 `CUENTA_INACTIVA`.

## Personal y cambio de rol (RF45)

Solo el rol **Administrador** (`/admin/**`). Asignar, modificar o revocar el rol de Gestor o Administrador ([CU](../../casos-de-uso/CU-cambiar-rol-cuenta.md); DEC-103 a DEC-106).

### Listar el personal

`GET /api/v1/auth/admin/personal?rol=&q=&pagina=0`

- `rol` (opcional): `Gestor` o `Administrador`; sin él, los dos. Otro valor: 400 `rol: NO_PERMITIDO`.
- `q` (opcional, hasta 100 caracteres): busca el texto contenido en el nombre (primer nombre y primer apellido del turista) o en el correo, con las mismas reglas que los demás listados.
- Orden: fecha de registro ascendente y `idUsuario` de desempate. Páginas de 10.

```json
{
  "elementos": [
    { "idUsuario": "0000…0003", "nombre": null, "correo": "gestor@samaria360.test", "rol": "Gestor",
      "fechaRegistro": "2026-10-19", "revocable": false }
  ],
  "pagina": 0, "tamano": 10, "totalElementos": 2, "totalPaginas": 1
}
```

- `nombre` es el del turista; es null si la cuenta no tiene fila en `turista` (las de la semilla).
- `revocable` dice si la tiene: solo esas cuentas vuelven a Turista.
- Para **asignar** un rol se elige un turista del listado de turistas (`GET /admin/turistas`).

### Cambiar el rol

`POST /api/v1/auth/admin/cuentas/{id}/cambio-rol`

```json
{ "rolNuevo": "Gestor", "rolActual": "Turista" }
```

- `rolNuevo`: `Turista`, `Gestor` o `Administrador`, distinto de `rolActual`. `rolActual` es el rol que vio el Administrador (control de concurrencia, como `estadoActual`).
- **Asignar:** de Turista a Gestor o Administrador. **Modificar:** de Gestor a Administrador o al revés. **Revocar:** a Turista.
- En una transacción: `usuario.id_rol`, la fila de `gestor` o `administrador` (la de `turista` se conserva), `version_cuenta + 1` y el evento `CuentaActualizada` con el rol nuevo y `estadoCuenta: Activo`.
- **200**: el elemento del listado del personal, con el rol nuevo (`rol: Turista` si se revocó).
- El token anterior de la cuenta recibe **401 `SESION_INVALIDADA`**. Al volver a iniciar sesión, el token trae el rol nuevo.

| Error | Código | Cuándo |
|---|---|---|
| 400 | `DATOS_INVALIDOS` | `campos.rolNuevo` o `campos.rolActual`: `REQUERIDO` (falta) o `NO_PERMITIDO` (desconocido, `Prestador` o `rolNuevo` igual a `rolActual`). Un cuerpo que no es JSON, sin `campos` |
| 404 | `CUENTA_NO_ENCONTRADA` | El id no existe |
| 409 | `CAMBIO_ROL_PROPIO` | La cuenta es la del Administrador que actúa |
| 409 | `CUENTA_NO_ELEGIBLE` | Lleva `motivo`: `PRESTADOR` (el Prestador no cambia de rol), `DESACTIVADA` (la cuenta no está Activa) o `SIN_ROL_BASE` (revocar una cuenta sin fila en `turista`) |
| 409 | `ROL_CAMBIADO` | El rol actual no es `rolActual`, o la cuenta cambió mientras tanto; lleva `rolActual` |
| 409 | `ULTIMO_ADMINISTRADOR` | El cambio dejaría el sistema sin Administradores activos (también con dos cambios simultáneos, DEC-106) |

Orden de evaluación: 400, 404 y, sobre la cuenta leída, `CAMBIO_ROL_PROPIO`, `CUENTA_NO_ELEGIBLE` (`PRESTADOR`), `ROL_CAMBIADO`, `CUENTA_NO_ELEGIBLE` (`DESACTIVADA`, `SIN_ROL_BASE`) y, al final, `ULTIMO_ADMINISTRADOR`.

Las rutas de turistas siguen filtrando por el rol actual: un Gestor o un Administrador con fila en `turista` no aparece en `GET /admin/turistas`, y desactivarlo o reactivarlo por esa ruta da 404 `CUENTA_NO_ENCONTRADA`.

## Perfil del turista (RF53)

Solo el rol **Turista** (otro rol → 403 `ACCESO_DENEGADO`); el turista es siempre el del claim `sub`. Se editan los nombres, los apellidos, el país y las categorías de interés. **El correo y la contraseña no se editan** (DEC-79). No sube `version_cuenta` ni escribe en el outbox.

`GET /api/v1/auth/perfil` → **200**

```json
{ "correo": "turista@samaria360.test", "primerNombre": "Ana", "segundoNombre": null,
  "primerApellido": "Pérez", "segundoApellido": null, "paisOrigen": "CO",
  "categoriasInteres": ["Naturaleza", "Cultura"] }
```

`PUT /api/v1/auth/perfil` · el mismo cuerpo sin `correo` → **200** con el perfil resultante (DEC-84).

- **Reemplaza el perfil completo**, no es parcial: un `segundoNombre` o `segundoApellido` ausente o vacío queda en null.
- Un `correo` en el cuerpo se ignora, como cualquier campo desconocido, y no cambia.
- **Reglas (las del registro, RF01; DEC-85):**
  - `primerNombre` y `primerApellido` obligatorios; los cuatro, de hasta 50 caracteres (puntos de código), recortados;
  - `paisOrigen`: código ISO 3166-1 alfa-2 de `GET /paises`, sin distinguir mayúsculas;
  - `categoriasInteres`: de 1 a 3, distintas, de `Naturaleza`, `Cultura` y `Aventura`. Se responden en ese orden.
- Las categorías se **reemplazan como conjunto** en la misma transacción que los datos: si algo falla, no cambia nada.
- **400** `DATOS_INVALIDOS` con `campos` (`REQUERIDO`, `FORMATO`, `LONGITUD_MAXIMA`, `NO_PERMITIDO`, `DUPLICADO`); un cuerpo que no es JSON, sin `campos`. **401** `NO_AUTENTICADO` (también si el token es válido, pero no hay fila en `turista`) o `SESION_INVALIDADA`. **403** `ACCESO_DENEGADO`.

## Códigos de error

| Código | HTTP | Cuándo |
|---|---|---|
| `DATOS_INVALIDOS` | 400 | `campos` como se indica arriba. Un cuerpo que no es JSON también, sin `campos` |
| `NO_AUTENTICADO` | 401 | Sin token, vencido, inválido o de servicio (`rol = SERVICIO`) |
| `SESION_INVALIDADA` | 401 | `ver` menor que `version_cuenta`: la cuenta cambió después de iniciar sesión |
| `ACCESO_DENEGADO` | 403 | El rol no es Administrador (`/admin/**`) o no es Turista (`/perfil`) |
| `CUENTA_NO_ENCONTRADA` | 404 | No existe, o es de otro rol (en el cambio de rol: no existe) |
| `ESTADO_CAMBIADO` | 409 | La cuenta no está en el estado de origen de la transición; lleva `estadoActual` (DEC-39) |
| `ROL_CAMBIADO` | 409 | La cuenta ya no tiene el rol desde el que se pidió el cambio; lleva `rolActual` (DEC-104) |
| `CUENTA_NO_ELEGIBLE` | 409 | La cuenta no admite el cambio de rol; lleva `motivo` (DEC-105) |
| `CAMBIO_ROL_PROPIO` | 409 | El Administrador intentó cambiar su propio rol (DEC-82) |
| `ULTIMO_ADMINISTRADOR` | 409 | El cambio dejaría el sistema sin Administradores activos (DEC-82) |
| `ERROR_INTERNO` | 500 | Cualquier otro error (si falla el evento, el cambio tampoco queda) |

## Endpoint interno de los consumidores

No es de ms-auth: lo exponen **ms-catalog y ms-booking** y lo llama el relevo de ms-auth. Se documenta aquí porque su cliente es ms-auth. **No pasa por el gateway** (no enruta `/internal/**`).

`PUT /internal/v1/cuentas/{id}` · `Authorization: Bearer <JWT de servicio>`

```json
{ "rol": "Turista", "estadoCuenta": "Desactivado", "version": 5 }
{ "rol": "Administrador", "estadoCuenta": "Activo", "version": 2 }
```

- **JWT de servicio:** RS256 con el mismo JWKS; `sub = ms-auth`, `rol = SERVICIO`, `aud` = `ms-catalog` o `ms-booking`, `exp` de 5 minutos.
- `rol`: `Turista`, `Prestador`, `Gestor` o `Administrador`. `estadoCuenta`: `Pendiente`, `Activo`, `Rechazado`, `Inactivo` o `Desactivado`. `version`: entero de 1 en adelante.
- **204** se aplique o se ignore: *upsert* idempotente que solo avanza si `version` es mayor que la guardada (tolera duplicados y desorden). El evento de una cuenta desconocida crea la fila; así recibe el destino a cada Gestor y Administrador (V15) y cada cambio de rol (RF45).
- **400** `DATOS_INVALIDOS` con `campos` (`REQUERIDO`, `NO_PERMITIDO`, `VALOR_MINIMO`). **401** sin token o con una `aud` distinta del destino. **403** con un token de usuario.
