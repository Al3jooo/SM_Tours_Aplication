# API de ms-auth

Contrato de ms-auth para el frontend y para las pruebas. Todas las rutas van por el API Gateway (`/api/v1/auth/**`); el servicio escucha en el 8081 solo dentro de la red de Docker. Este documento nace con el CU «Administrar cuentas» (RF28, RF22, RF62; WF-12) y fija los endpoints del **Administrador**, el token y el endpoint interno. Los endpoints de inicio de sesión y registro (RF15, RF01, RF02) están descritos en `docs/trazabilidad.md` («Endpoints de ms-auth»). Decisiones en `docs/trazabilidad.md` («Administrar cuentas — decisiones»), [ADR-008](../adr/ADR-008-sincronizacion-estado-cuentas.md) y el [CU](../../casos-de-uso/CU-administrar-cuentas.md).

## Convenciones

- **Autenticación:** `Authorization: Bearer <JWT de ms-auth>`. Los endpoints de `/admin/**` exigen el rol **Administrador**.
- **Errores:** ProblemDetail (RFC 9457) con `codigo` estable, como ms-catalog y ms-booking. `DATOS_INVALIDOS` lleva `campos`: un código por campo con el nombre JSON, nunca textos (`REQUERIDO`, `FORMATO`, `LONGITUD_MAXIMA`, `NO_PERMITIDO`).
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

`version_cuenta` sube en 1 con cada cambio de estado de la cuenta (y, en el futuro, de rol: RF45). Cada servicio rechaza con **401 `SESION_INVALIDADA`** un token con `ver` menor que la versión conocida; un token sin `ver` cuenta como 0. ms-auth compara contra su propia tabla en **todos** sus endpoints autenticados (`/me` y `/admin/**`). La respuesta de `/login` no cambia (`expiraEn` es ahora 15 minutos después).

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

## Códigos de error

| Código | HTTP | Cuándo |
|---|---|---|
| `DATOS_INVALIDOS` | 400 | `campos` como se indica arriba. Un cuerpo que no es JSON también, sin `campos` |
| `NO_AUTENTICADO` | 401 | Sin token, vencido, inválido o de servicio (`rol = SERVICIO`) |
| `SESION_INVALIDADA` | 401 | `ver` menor que `version_cuenta`: la cuenta cambió después de iniciar sesión |
| `ACCESO_DENEGADO` | 403 | El rol no es Administrador |
| `CUENTA_NO_ENCONTRADA` | 404 | No existe, o es de otro rol |
| `ESTADO_CAMBIADO` | 409 | La cuenta no está en el estado de origen de la transición; lleva `estadoActual` (DEC-39) |
| `ERROR_INTERNO` | 500 | Cualquier otro error (si falla el evento, el cambio tampoco queda) |

## Endpoint interno de los consumidores

No es de ms-auth: lo exponen **ms-catalog y ms-booking** y lo llama el relevo de ms-auth. Se documenta aquí porque su cliente es ms-auth. **No pasa por el gateway** (no enruta `/internal/**`).

`PUT /internal/v1/cuentas/{id}` · `Authorization: Bearer <JWT de servicio>`

```json
{ "rol": "Turista", "estadoCuenta": "Desactivado", "version": 5 }
```

- **JWT de servicio:** RS256 con el mismo JWKS; `sub = ms-auth`, `rol = SERVICIO`, `aud` = `ms-catalog` o `ms-booking`, `exp` de 5 minutos.
- `rol`: `Turista`, `Prestador`, `Gestor` o `Administrador`. `estadoCuenta`: `Pendiente`, `Activo`, `Rechazado`, `Inactivo` o `Desactivado`. `version`: entero de 1 en adelante.
- **204** se aplique o se ignore: *upsert* idempotente que solo avanza si `version` es mayor que la guardada (tolera duplicados y desorden).
- **400** `DATOS_INVALIDOS` con `campos` (`REQUERIDO`, `NO_PERMITIDO`, `VALOR_MINIMO`). **401** sin token o con una `aud` distinta del destino. **403** con un token de usuario.
