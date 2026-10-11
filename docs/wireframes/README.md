# Wireframes de Samaria360

Fuente: Documento Entregable 2 (Diseño de interfaces). Cada imagen tiene aquí su descripción escrita, que es la referencia cuando un detalle de la imagen no se lea bien.

**Regla de trazabilidad:** toda pantalla del prototipo corresponde a un wireframe de esta carpeta. Si un elemento queda fuera del alcance de la iteración, se muestra deshabilitado (o como enlace sin destino) y se lista como pendiente en `docs/trazabilidad.md`.

## Elementos comunes a todas las pantallas

- **Encabezado:** logo "Samaria360"; navegación: Inicio de sesión, Búsqueda, Servicios, Asistente IA; selector de idioma **ES / EN** (RF03, RNF08); icono de búsqueda. Con sesión iniciada aparece un **avatar circular con las iniciales** del usuario (p. ej. "MG").
- **Pie de página:** columnas Explorar / Plataforma (Alojamiento, Gastronomía, Tours, Alquiler de equipos), Municipio (Noticias, Emergencias, Aforos en vivo, Contacto), Prestadores (Registrar servicio, Panel del gestor, Preguntas frecuentes), Legal (Términos, Condiciones y Políticas de Privacidad — RF46). Texto inferior: "© 2026 Alcaldía de Santa Marta · Samaria360".
- **Botón flotante** "Asistente virtual IA" en la esquina inferior derecha (RF04).

## Índice

| Archivo | Pantalla | Caso de uso | Requisitos | Alcance del prototipo (Elaboración) |
|---|---|---|---|---|
| WF-01-inicio-sesion.png | Inicio de sesión | Iniciar sesión | RF15, RF03, RF34, RF46 | **Sí** (sin recuperación de contraseña) |
| WF-02-registro-turista.png | Registro de turista | Registrarse | RF01 | No |
| WF-03-registro-prestador.png | Registro de prestador | Registrarse | RF02 | No |
| WF-04-inicio-busqueda.png | Inicio y búsqueda | Buscar oferta turística | RF16, RF40, RF04 | **Sí** (parcial) |
| WF-05-exploracion.png | Exploración: mapa y resultados | Buscar oferta, Filtrar, Ordenar, Navegar mapa | RF16, RF23, RF38, RF39, RF50, RF61 | **Sí** (núcleo del catálogo) |
| WF-06-detalle.png | Detalle de lugar o servicio | Consultar detalle de oferta | RF37, RF39, RF51, RF54, RF57, RF58 | No (iteración siguiente) |
| WF-07-asistente-ia.png | Asistente virtual IA | Interactuar con asistente | RF04, RF35, RF36, RF59 | No (solo esqueleto técnico del servicio) |
| WF-08-registrar-servicio.png | Registrar servicio comercial | Registrar servicio | RF09, RF10, RF27, RF37, RF48, RF58 | No |
| WF-09-registrar-atractivo.png | Registrar atractivo turístico | Registrar atractivo | RF06, RF07, RF24, RF27, RF33 | No |

## WF-01 — Inicio de sesión

- Título "Samaria360" y subtítulo "Plataforma de turismo de Santa Marta".
- Pestañas **Iniciar sesión** (activa) / **Registrarse**.
- Texto: "Accede con el correo con el que te registraste o crea una cuenta nueva."
- Tarjeta "Ingresa con tu cuenta":
  - Campo **Correo electrónico** (icono de sobre, placeholder `nombre@correo.com`).
  - Campo **Contraseña** (icono de candado, placeholder "Mínimo 8 caracteres").
  - Enlace **¿Olvidaste tu contraseña?** (RF34).
  - Recuadro informativo con icono: **"3 intentos fallidos bloquearán la cuenta por 15 minutos."** (RF15).
  - Botón principal **Iniciar sesión**.
  - Texto legal: "Al iniciar sesión aceptas los Términos, Condiciones y Políticas de Privacidad y el tratamiento de datos de Samaria360."
- Bloque "Plataforma" con enlaces: Explorar destinos, Rutas y experiencias, Prestadores.

**Prototipo:** login completo contra `POST /api/v1/auth/login`, con mensajes para 401 (credenciales inválidas), 423 (cuenta bloqueada, con la hora de desbloqueo) y 403 (cuenta inactiva). "¿Olvidaste tu contraseña?" y "Registrarse" visibles pero sin funcionalidad. Tras el login, redirige a WF-04 y muestra el avatar con iniciales (de `GET /api/v1/auth/me`, campo `nombreVisible`).

## WF-02 — Registro de turista

Selector de tipo de cuenta (Turista / Prestador de servicios). Formulario: primer nombre, segundo nombre (opcional), primer apellido, segundo apellido (opcional), correo electrónico, crear contraseña (mínimo 8 caracteres), país de origen (lista desplegable), categorías de interés (casillas Naturaleza, Cultura, Aventura), casilla "Acepto la Política de Privacidad y el tratamiento de mis datos", botón **Crear cuenta de turista**.

## WF-03 — Registro de prestador

Selector de tipo de cuenta con Prestador seleccionado; enlace "← Volver al inicio". Formulario: nombre del representante legal, número RNT, correo corporativo, teléfono, contraseña (mínimo 8 caracteres). Aviso: **"Su cuenta quedará Pendiente de Validación."** Botón **Registrar prestador**.

## WF-04 — Inicio y búsqueda

- **Banner de aviso** del municipio, cerrable (RF40). Ej.: cierre temporal de un sendero por lluvias.
- Titular: "Encuentra alojamiento, gastronomía y tours en Santa Marta" y texto de apoyo sobre prestadores verificados, aforo en tiempo real y asistente virtual.
- **Barra de búsqueda** ("¿Qué buscas? Ej: cabañas, restaurantes, tour de café") con botón **Buscar**.
- "Explora por categoría": botones **Alojamiento, Gastronomía, Tours, Alquiler de equipos**.
- "Destacados esta semana": 3 tarjetas con imagen, nombre, categoría, calificación, precio "desde", disponibilidad y enlace **Ver detalle** / **Reservar**.

**Prototipo:** la búsqueda y los botones de categoría llevan a WF-05 con el filtro aplicado. Destacados: tres servicios publicados del catálogo. Banner de avisos y "Reservar": fuera de alcance.

## WF-05 — Exploración: mapa y resultados

- Migas de pan: Samaria360 › Exploración interactiva › Mapa y resultados.
- Título "Exploración de Atractivos y Servicios en Santa Marta".
- **Filtros:** texto ("Filtrar por nombre, vereda o actividad"), **rango de precio** (deslizador), **distancia máxima en km** (deslizador), casilla **"Acceso para personas con movilidad reducida"**, botón **Aplicar filtros**.
- **Mapa** con marcadores; leyenda de aforo: **Bajo (< 40 %)**, **Medio (40–75 %)**, **Alto (> 75 %)**; botón **Centrar en mi ubicación**.
- Encabezado de resultados: "Resultados en Santa Marta (N lugares encontrados)" y pestañas de orden: **Menor distancia, Menor afluencia, Calificación**.
- **Tarjetas de resultado:** imagen, nombre, descripción corta, calificación y número de reseñas, precio o rango, nivel de aforo, distancia, indicador de accesibilidad, enlace **Ver detalle**.
- **Paginación** numerada.
- Botón "¿Preguntas sobre el estado de las vías o aforos? **Preguntar al asistente**".

**Prototipo:** filtros de texto, categoría, precio, ubicación/distancia y accesibilidad; mapa con marcadores (Leaflet + OpenStreetMap); orden por distancia; paginación de 10. Aforo, calificación y su orden dependen de datos aún no modelados: se muestran deshabilitados o con valor "—" y quedan como pendientes. "Ver detalle" y "Preguntar al asistente": fuera de alcance.

## WF-06 — Detalle de lugar o servicio

Nombre, tipo y ubicación, calificación; indicador de accesibilidad; etiquetas (Prestador verificado, Aforo en vivo, Reserva inmediata); iconos de favorito y compartir; galería de imágenes; alerta de aforo; descripción; tabla de contacto (teléfono, WhatsApp, dirección, correo, registro RNT); botón **Reportar información falsa o desactualizada**; tabla de precios; botón **Ver disponibilidad y reservar**; horarios de atención; sección **Destinos alternativos con menor afluencia** (3 tarjetas).

## WF-07 — Asistente virtual IA

Aviso de versión beta; conversación con el asistente; panel lateral "Itinerario sugerido" por día y hora, con **Compartir** y **Descargar PDF**; lista "Prestadores mencionados"; botones **Guardar itinerario en perfil** y **Ver en el mapa**; chips de consulta rápida (Aforos en vivo, Rutas alternas, Tours mañana, Alquiler de equipos); campo de texto y botón enviar; sección "El asistente también puede ayudarte con" (Aforos en vivo, Rutas y clima, Prestadores verificados).

## WF-08 — Registrar servicio comercial

Datos del servicio (nombre, categoría, número de RNT, descripción); datos de contacto (teléfono/WhatsApp, correo, sitio web o redes, dirección, horario de atención); precio ("Precio desde COP", "Cobro por: Noche", casilla "El precio incluye impuestos"); campos según categoría (Alojamiento: número de habitaciones, capacidad máxima por habitación, servicios incluidos WiFi/Desayuno/Piscina/Parqueadero); fotos (1 a 5, JPG o PNG, máx. 5 MB); condiciones de accesibilidad; confirmación de veracidad; botones **Publicar servicio** y **Cancelar**; aviso de revisión en 1 a 2 días hábiles.

## WF-09 — Registrar atractivo turístico

Datos del atractivo (nombre, categoría, latitud y longitud en grados decimales); condiciones de accesibilidad (movilidad reducida, señalización visual/auditiva); aforo y estado operativo (límite máximo de capacidad de carga, estado operativo: Abierto); fotos (JPG/PNG, máx. 5 MB); normas y regulaciones ambientales (máximo 1000 caracteres); confirmación; botones **Registrar atractivo** y **Cancelar**.

## Discrepancias detectadas con el SRS y el modelo (para resolver en el SAD)

1. **Avatar con iniciales:** Gestor y Administrador no tienen nombre en el modelo de datos. Solución temporal en el prototipo: `nombreVisible` usa la parte del correo antes de la @.
2. **Accesibilidad:** WF-08 muestra cuatro opciones (rampa de acceso, acceso para silla de ruedas, señalización en braille, baños adaptados), mientras RF27/RF50 definen tres condiciones (movilidad reducida, visual, auditiva).
3. **WF-08** incluye "Sitio web o redes" y "El precio incluye impuestos", que no están en RF09/RF37 ni en el esquema `servicios`.
4. **WF-05:** el nivel de aforo y la calificación requieren datos que el esquema MongoDB todavía no tiene (registro de visitas/ocupación y calificación promedio desnormalizada).
5. **WF-01 / WF-02 / WF-03:** "Mínimo 8 caracteres" es una política de contraseñas que el SRS no define explícitamente; se valida solo al registrar, nunca al iniciar sesión.
