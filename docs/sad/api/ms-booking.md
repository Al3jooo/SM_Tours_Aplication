# API de ms-booking (reservas)

Contrato del CU «Reservar servicio» (RF11, RF18, RF20, RF32, RF37, RF44) para el frontend (WF-10) y para las pruebas. Todas las rutas van por el API Gateway (`/api/v1/reservas/**`); el servicio escucha en el 8083 solo dentro de la red de Docker. Decisiones en `docs/trazabilidad.md` («Reservar servicio») y [ADR-007](../adr/ADR-007-reservas-y-catalogo.md).

## Convenciones

- **Autenticación:** `Authorization: Bearer <JWT de ms-auth>`. Reservar, consultar y cancelar exigen el rol **Turista**; el turista es el `sub` del token. La disponibilidad es pública.
- **Fechas:** `aaaa-mm-dd`; horas: `hh:mm` (la respuesta usa `hh:mm:ss`); instantes: ISO 8601 en UTC (`2030-03-30T13:00:00Z`). Todo se interpreta en la hora de Santa Marta (`America/Bogota`).
- **Dinero:** COP, número decimal.
- **Errores:** ProblemDetail (RFC 9457) con `codigo` estable, como ms-auth y ms-catalog. `DATOS_INVALIDOS` lleva `campos`: un código por campo con el nombre JSON, nunca textos.

## Reservar

`POST /api/v1/reservas` · rol Turista · cabecera **`Idempotency-Key: <UUID>`** obligatoria

```json
{
  "idServicio": "6a0f…",          // ObjectId del servicio (catalog_db)
  "idUnidad": "6b2c…",            // solo alojamiento: la unidad del detalle de ms-catalog
  "fechaEntrada": "2030-03-30",   // alojamiento: entrada; tour: el día
  "fechaSalida": "2030-04-01",    // solo alojamiento: rango [entrada, salida), máximo 14 noches
  "horaInicio": "08:00",          // solo tour: un horario publicado
  "cantidad": 1,                  // solo alojamiento: habitaciones de la unidad
  "personas": 2,                  // huéspedes o personas del tour
  "precioTotalEsperado": 520000   // el total que ve el turista (RF37)
}
```

**Reglas.**
- **Una reserva es de una sola unidad.** Para dos tipos de habitación se hacen dos reservas.
- **Alojamiento:** cobra `precioNoche` de la unidad × noches × habitaciones. `personas` ≤ habitaciones × `capacidadMaxHuespedes` de la unidad. Check-in 15:00 y check-out 12:00; con entrada hoy, se puede reservar hasta las 15:00.
- **Tour:** cobra `precioBase` × personas. `personas` ≤ `aforoMaxGrupo`. No se reserva un horario que ya pasó.
- La confirmación es automática (RF20): los cupos se descuentan en el momento.
- **`precioTotalEsperado` se compara por valor, no por escala:** `160001` y `160001.00` son el mismo total. Solo un valor distinto responde `PRECIO_CAMBIADO`.

**Respuesta `201 Created`**, con `Location: /api/v1/reservas/{idReserva}`:

```json
{
  "idReserva": "3f1c…", "estado": "Confirmada",
  "idServicio": "6a0f…", "nombreServicio": "Hotel Casa Tayrona", "categoria": "Alojamiento",
  "idUnidad": "6b2c…", "nombreUnidad": "Suite familiar",
  "fechaEntrada": "2030-03-30", "fechaSalida": "2030-04-01", "horaInicio": null,
  "fechaInicio": "2030-03-30T20:00:00Z", "fechaFin": "2030-04-01T17:00:00Z",
  "cantidad": 1, "personas": 2, "precioUnitario": 260000, "unidadCobro": "Noche",
  "precioTotal": 520000, "moneda": "COP",
  "fechaCreacion": "2030-03-10T17:00:00Z", "fechaCancelacion": null,
  "cancelableHasta": "2030-03-29T20:00:00Z"
}
```

El nombre del servicio, el de la unidad y los precios quedan **congelados** (RF32): no cambian aunque cambie el catálogo.

**`cancelableHasta`** lo calcula ms-booking: `fechaInicio` menos el plazo de RF44 (24 h). Viaja en todas las respuestas de una reserva (reservar, `/mias`, `/{id}` y cancelar). El cliente no recalcula el plazo: lo usa para mostrar si todavía se puede cancelar, y el servidor decide igual al cancelar.

**Idempotencia.**
- Reenviar con la misma clave y el mismo pedido responde **`201` con la misma reserva** y la cabecera `Idempotent-Replayed: true`, sin descontar otra vez.
- La misma clave con otro pedido (incluida otra unidad) responde `422 IDEMPOTENCIA_CONFLICTO`.
- La clave es de cada turista. Un intento que falla (por ejemplo, `CUPO_AGOTADO`) no la guarda.
- **Regla para el frontend:** generar una clave nueva por cada intención de reservar y reutilizarla en los reintentos de esa misma intención.

## Mis reservas

`GET /api/v1/reservas/mias?desde=&hasta=&estado=&pagina=0` · rol Turista → `{elementos, pagina, tamano: 20, totalElementos, totalPaginas}`. RF12, del lado del turista.

- **`desde` y `hasta`** (opcionales): fechas de Santa Marta (`aaaa-mm-dd`), inclusivas, sobre la fecha de la **actividad**. Una reserva aparece si `fechaInicio < (hasta + 1 día) 00:00` y `fechaFin > desde 00:00`, en `America/Bogota`: una estadía del 30 de noviembre al 2 de diciembre aparece al filtrar diciembre, y una actividad que termina exactamente a las 00:00 de `desde`, no. Con uno solo, el rango queda abierto del otro lado.
- **`estado`** (opcional): uno o varios de `Confirmada`, `Cancelada` y `Completada`, separados por coma.
- **Orden:** `fechaInicio` descendente, con `idReserva` como desempate (la paginación es estable).
- **Errores (`DATOS_INVALIDOS`):** `hasta` anterior a `desde` → `hasta: RANGO_INVALIDO`; más de 366 días contando los dos extremos → `hasta: MAXIMO_DIAS`; una fecha mal escrita → `FORMATO`; un estado desconocido → `estado: NO_PERMITIDO`.

`GET /api/v1/reservas/{idReserva}` · rol Turista → la reserva. La de otro turista responde 404, igual que una que no existe.

## Cancelar

`POST /api/v1/reservas/{idReserva}/cancelacion` · rol Turista → `200` con la reserva en estado `Cancelada`.

- **RF44:** se puede cancelar si faltan **24 horas o más** para el inicio (`cancelableHasta`, límite incluido). Si no, `409 CANCELACION_FUERA_DE_PLAZO`.
- Cancelar una reserva ya Cancelada responde `200` con ella y no devuelve cupos otra vez.
- Una reserva Completada no se cancela: `409 RESERVA_NO_CANCELABLE`. También si RF52 la completa mientras se procesa la cancelación: gana uno de los dos y los cupos no cambian.
- Los cupos se devuelven a las mismas noches o al mismo horario que se descontaron.

## Disponibilidad (pública)

`GET /api/v1/reservas/disponibilidad?servicio=&unidad=&desde=&hasta=`
- `servicio` obligatorio; `unidad` opcional (solo alojamiento);
- por defecto, desde hoy y 30 días; como máximo 60 días.

```json
{
  "idServicio": "6a0f…", "desde": "2030-03-30", "hasta": "2030-03-31", "categoria": "Alojamiento",
  "unidades": [
    { "idUnidad": "6b2c…", "noches": [ { "fecha": "2030-03-30", "cuposTotales": 3, "cuposDisponibles": 1 } ] }
  ],
  "horarios": [],
  "horaCheckIn": "15:00:00", "horaCheckOut": "12:00:00", "plazoCancelacionHoras": 24
}
```

`horaCheckIn` y `horaCheckOut` son el horario de los alojamientos y `plazoCancelacionHoras`, el plazo de RF44. Salen de la configuración de ms-booking (`samaria360.reservas.hora-check-in` y `hora-check-out`) y van siempre, aunque no haya inventario en el rango: la pantalla los muestra tal cual, sin escribirlos por su cuenta.

En un tour, `unidades` va vacío y `horarios` trae `{fecha, hora, cuposTotales, cuposDisponibles}`. Sin inventario en el rango, `categoria` es `null` y las dos listas van vacías. **No trae nombres ni precios:** el cliente los toma del detalle de ms-catalog (`GET /api/v1/catalogo/servicios/{id}`).

`GET /api/v1/reservas/disponibilidad/resumen?servicios=id1,id2&desde=&hasta=` → `{"id1": "CON_CUPOS", "id2": "SIN_INVENTARIO"}`, en el orden pedido.
- Hasta 10 ids, cada uno un ObjectId; por defecto, hoy y los 7 días siguientes.
- `CON_CUPOS`: alguna unidad u horario tiene cupos en algún día del rango; `AGOTADO`: hay inventario, pero todo en 0; `SIN_INVENTARIO`: no hay filas.

## Completar reservas (RF52)

Una tarea de ms-booking pasa de `Confirmada` a `Completada` las reservas cuya `fechaFin` ya pasó, con un UPDATE condicional idempotente. Corre al arrancar y después cada 15 minutos (`samaria360.completar-reservas.cada`): una reserva queda Completada a más tardar 15 minutos después de terminar. No tiene endpoint.

## Códigos de error

| Código | HTTP | Cuándo |
|---|---|---|
| `DATOS_INVALIDOS` | 400 | `campos` con: `REQUERIDO`, `FORMATO`, `VALOR_MINIMO`, `NO_PERMITIDO`, `FECHA_PASADA`, `RANGO_INVALIDO`, `MAXIMO_NOCHES`, `MAXIMO_DIAS`, `MAXIMO_ELEMENTOS`, `EXCEDE_CAPACIDAD`, `EXCEDE_AFORO`, `CATEGORIA_NO_RESERVABLE`. `idUnidad` es `REQUERIDO` en un alojamiento y `NO_PERMITIDO` en un tour. Un cuerpo que no es JSON también es `DATOS_INVALIDOS`, sin `campos` |
| `IDEMPOTENCIA_REQUERIDA` | 400 | Falta `Idempotency-Key` o no es un UUID |
| `NO_AUTENTICADO` | 401 | Sin token o con uno inválido |
| `ACCESO_DENEGADO` | 403 | El rol no es Turista |
| `RESERVA_NO_ENCONTRADA` | 404 | No existe o es de otro turista |
| `OFERTA_NO_DISPONIBLE` | 409 | El servicio no existe, no está Publicado o el prestador está inactivo |
| `UNIDAD_NO_DISPONIBLE` | 409 | El alojamiento no tiene esa unidad: hay que volver a cargar el detalle |
| `FECHA_NO_DISPONIBLE` | 409 | No hay inventario publicado para alguna noche o para el horario |
| `CUPO_AGOTADO` | 409 | Hay inventario, pero no alcanza (RF11); no se descuenta nada |
| `PRECIO_CAMBIADO` | 409 | Lleva `precioTotal`, `precioUnitario` y `moneda` vigentes (RF37) |
| `CANCELACION_FUERA_DE_PLAZO` | 409 | Faltan menos de 24 horas (RF44) |
| `RESERVA_NO_CANCELABLE` | 409 | La reserva está Completada (también si RF52 la completó mientras se cancelaba) |
| `IDEMPOTENCIA_CONFLICTO` | 422 | La misma clave con otro pedido |
| `SERVICIO_NO_DISPONIBLE` | 503 | ms-catalog no responde (`servicio: ms-catalog`) o, desde el gateway, ms-booking caído (`servicio: ms-booking`) |
| `ERROR_INTERNO` | 500 | Cualquier otro error |
