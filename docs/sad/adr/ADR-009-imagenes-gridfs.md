# ADR-009 — Imágenes de los atractivos en GridFS, servidas por ms-catalog

- **Estado:** Aceptada
- **Fecha:** 2026-10-30
- **Fase:** Construcción (RUP), iteración E «Atractivos y agenda», sesión A1
- **Componentes:** `ms-catalog` (`catalog_db`: bucket GridFS `imagenes` y colección `atractivos`), `api-gateway` y el `frontend` (nginx)
- **Requisitos:** RF06 («carga de imágenes en formato JPEG/PNG»), RF23 (la miniatura del mapa); RNF06
- **Registra las decisiones:** DEC-55 (GridFS en lugar de MinIO), DEC-209 (alcance y reglas de la carga) y DEC-216 (petición multipart)

## Contexto

- **RF06** pide que el Gestor registre un atractivo «con carga de imágenes en formato JPEG/PNG». Hasta la iteración D, las imágenes del catálogo eran archivos estáticos de la semilla (`/imagenes/catalogo/*.webp`, servidos por nginx): no había forma de subir una.
- **DEC-55** (plan del 60 %) fijó el almacén: GridFS en `catalog_db`, de 1 a 5 imágenes por recurso, JPEG o PNG validados por su firma y 5 MB como máximo cada una, sin contenedores nuevos. Faltaba redactar este ADR.
- El dueño del dato es ms-catalog: el atractivo y sus imágenes se crean juntos y se leen juntos. Ningún otro servicio necesita las imágenes.
- El MongoDB de Docker Compose es una instancia sola (no un *replica set*): no hay transacciones entre GridFS y la colección `atractivos`.
- Las peticiones pasan por nginx (frontend) y por el api-gateway (ADR-005) antes de llegar a ms-catalog. nginx corta por defecto en 1 MB.

## Decisión

1. **Almacén.** Las imágenes viven en GridFS, bucket `imagenes` de `catalog_db` (`imagenes.files` e `imagenes.chunks`). ms-catalog usa el `GridFSBucket` del driver que ya trae spring-data-mongodb: **sin dependencias nuevas**. Puerto `AlmacenImagenesPort` y adaptador `ImagenGridFsAdapter`; el dominio no conoce GridFS.
2. **Reglas (dominio, `PoliticaImagen`).** De 1 a 5 imágenes por atractivo. Cada una es JPEG (`FF D8 FF`) o PNG (`89 50 4E 47 0D 0A 1A 0A`), reconocida por su **firma**, nunca por el nombre ni por el `Content-Type` que declara el navegador. Máximo 5 MB = 5 × 1024 × 1024 bytes cada una. Cada imagen inválida es un 400 `DATOS_INVALIDOS` con `campos["imagenes[i]"]` (`FORMATO`, `TAMANO_MAXIMO` o `REQUERIDO`).
3. **Carga (DEC-216).** El registro es una sola petición `multipart/form-data`: la parte `datos` (JSON, `application/json`) y de 1 a 5 partes `imagenes`. Primero se validan los datos y las imágenes; si todo es válido, se guardan las imágenes con ids que genera el servidor y, al final, se inserta el atractivo con sus rutas.
4. **Compensación.** Si una imagen o el insert fallan, el caso de uso borra las imágenes ya guardadas y relanza el error original (500 `ERROR_INTERNO`). Un borrado que también falla se agrega como excepción suprimida.
5. **Referencia.** `atractivos.imagenes[]` guarda la **ruta pública** de cada imagen: `/api/v1/catalogo/imagenes/{id}`. Así conviven con las rutas estáticas de la semilla (`/imagenes/catalogo/*.webp`) y el frontend no distingue unas de otras.
6. **Lectura.** `GET /api/v1/catalogo/imagenes/{id}` es público (como la ficha que la muestra). Responde con el tipo del formato guardado en `metadata.formato`, `Cache-Control: public, max-age=31536000, immutable` (una imagen no cambia: la galería no se edita en esta iteración) y `X-Content-Type-Options: nosniff` (Spring Security). Un id inválido, inexistente o de un archivo sin `metadata.formato` es 404 `IMAGEN_NO_ENCONTRADA`.
7. **Topes de transporte.**
   - **ms-catalog:** `spring.servlet.multipart.max-file-size: 6MB` y `max-request-size: 30MB`. El tope por parte queda por encima de 5 MB para que una imagen de más llegue al dominio y reciba su motivo por campo; lo que supere estos topes es 413 `CARGA_DEMASIADO_GRANDE`. `server.tomcat.max-swallow-size: 32MB` para que el cliente reciba ese 413 y no una conexión cortada.
   - **nginx (frontend):** `client_max_body_size 30m` en `location /api/`.
   - **api-gateway:** sin cambios. Reenvía el cuerpo como flujo, sin leerlo ni limitarlo (no tiene dominio, ADR-005).

## Alternativas descartadas

- **MinIO (o S3).** Un contenedor más, con sus credenciales y su red; DEC-55 lo descartó por el tamaño del prototipo.
- **Archivos en un volumen de ms-catalog.** Separa el dato de su base: las copias de seguridad de `catalog_db` no incluirían las imágenes, y otra réplica de ms-catalog no las vería.
- **Base64 dentro del documento del atractivo.** Infla el documento un 33 % y lo acerca al límite de 16 MB de MongoDB con 5 imágenes de 5 MB; cada lectura de la búsqueda arrastraría las imágenes.
- **Subir cada imagen antes del registro (endpoint propio de carga).** Deja imágenes sin atractivo cada vez que el Gestor abandona el formulario; necesitaría un recolector desde el primer día.
- **Transacción multi-documento.** Exige un *replica set* en Compose; no se justifica para el prototipo.

## Consecuencias

- **Positivas:** un solo dueño del dato; sin infraestructura nueva; las imágenes viajan en las copias de `catalog_db`; la validación por firma impide servir como imagen un archivo que no lo es.
- **Riesgo de huérfanos (registrado, no resuelto en el prototipo).** Sin transacción, si el insert falla **y** el borrado de compensación también falla (por ejemplo, MongoDB se cae entre el guardado y el borrado, o el proceso de ms-catalog muere en medio), quedan archivos en `imagenes.files` sin ningún atractivo que los referencie. No se ven (nadie conoce su id) y no afectan la consistencia, pero ocupan espacio. **Regla para identificarlos:** un archivo de `imagenes.files` con más de 24 horas cuyo `_id` no aparece en ninguna ruta de `atractivos.imagenes`. No se implementa un recolector en esta iteración; si se implementa, irá con su propia decisión.
- **Memoria.** ms-catalog lee cada imagen completa en memoria al registrarla y al servirla (≤ 5 MB cada una, ≤ 30 MB por petición). Es aceptable para el volumen del prototipo; con tráfico real convendría servirlas como flujo o desde una CDN.
- **Caché.** Como las imágenes son inmutables, la caché de un año es segura. Si en una iteración futura se edita la galería (como RF48 en servicios), cada imagen nueva tendrá un id nuevo: la regla se mantiene.
- **Servicios.** Esta decisión aplica solo a los atractivos. Las fotos de WF-08 (servicios) siguen deshabilitadas: RF48 está descartado (DEC-54).

## Cumplimiento

| Punto | Prueba |
|---|---|
| 2. Firma, 5 MB exactos y 5 MB + 1, de 1 a 5 | `PoliticaImagenTest`, `RegistroAtractivoTest`, `RegistrarAtractivoApiIntegracionTest` |
| 3. Multipart con `datos` e `imagenes`; nada se guarda si algo es inválido | `RegistrarAtractivoApiIntegracionTest`, `RegistrarAtractivoServiceTest` |
| 4. Compensación: si el insert falla, se borran las imágenes (con GridFS real) | `CompensacionImagenesApiIntegracionTest`, `RegistrarAtractivoServiceTest` (también el borrado que falla) |
| 6. Imagen servida con su tipo, caché y `nosniff`; 404 | `ImagenApiIntegracionTest` |
| 7. Topes con Tomcat real: 5 MB exactos pasan; una parte de 7 MB es 413 | `CargaMultipartServidorTest` |
| 7. Una imagen de 5 MB atraviesa el gateway byte a byte | `TransferenciaImagenTest` (api-gateway) |

## Cuándo revisarla

- Si se edita la galería (agregar o quitar imágenes) o se agregan imágenes a los servicios (RF48).
- Si MongoDB pasa a *replica set*: la compensación puede cambiarse por una transacción.
- Si aparecen huérfanos en la práctica o el volumen exige servir las imágenes desde una CDN.
