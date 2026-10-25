# Demostración del CU «Reservar servicio»

Con el sistema levantado (`docker compose up -d --build`, ver el README de la raíz). Todas las llamadas van por el API Gateway (`http://localhost:8080`). El contrato está en [`docs/sad/api/ms-booking.md`](../../sad/api/ms-booking.md).

**Inventario.** ms-booking materializa bajo demanda los cupos de los próximos 60 días a partir de la oferta del catálogo (DEC-135, DEC-152; reemplaza al generador de inventario demo D5, DEC-156): la primera consulta de disponibilidad, del resumen de las tarjetas o de una reserva los crea, sin tocar los que ya existen. La semilla tiene la oferta que rellenó M008: todos los días.
- **Tours:** una fila por salida de cada día de operación (en la semilla, las 08:00 y las 14:00), con el aforo del grupo.
- **Alojamientos:** cada unidad y cada noche de operación, con sus habitaciones. Hotel Casa Tayrona tiene dos unidades: Doble (12 habitaciones) y Suite familiar (3).

## `reservas.http`

Para la extensión REST Client de VS Code. Recorre:
- el detalle del hotel con sus unidades y la disponibilidad;
- una reserva de la Suite, repetida con la misma clave (201 e `Idempotent-Replayed`) y con otra unidad (422);
- un precio desactualizado (409 `PRECIO_CAMBIADO`);
- un tour, «Mis reservas», la cancelación (y su repetición), RF44 y el resumen de las tarjetas;
- el catálogo caído (`docker compose stop ms-catalog`).

Las credenciales no están en el archivo: se leen de las variables de entorno `CORREO` y `CONTRASENA` de un turista de prueba. La clave `@clave` se cambia en cada demostración.

## `concurrencia.sh`

Demuestra que el último cupo de cada unidad se vende una sola vez y que las unidades no se afectan entre sí (D1, D3).

```bash
CORREO=<turista> CONTRASENA=<contraseña> bash docs/demo/reservas/concurrencia.sh 10
```

Requiere `curl` y Node.js (el del frontend; se usa para leer JSON, porque `jq` no viene en Windows).

1. Busca la primera noche, desde mañana, en la que la Doble y la Suite familiar tienen todos sus cupos. **Imprime la noche que usa.**
2. Reserva todas las habitaciones de cada unidad menos una.
3. Lanza N solicitudes por unidad a la vez (2N en total), cada una por la última habitación.
4. Espera **un solo 201 por unidad**; las demás deben ser 409 `CUPO_AGOTADO`, y las dos unidades deben quedar en 0. Si no, termina con código 1.

**Cada ejecución agota una noche en las dos unidades.** La siguiente usa la noche siguiente que siga completa. La ventana de 60 días alcanza para muchas ejecuciones; si se agota, se libera cancelando las reservas o esperando a que el generador cree días nuevos.

## Memoria después de la demo

```bash
docker stats --no-stream
```

Se mide **después** de `concurrencia.sh`, no en reposo: es el momento de más carga. Los límites están en `docker-compose.yml` y en el README de la raíz («Memoria»).
