# Wireframes de Samaria360

**Versión 2 — 12/10/2026.** Reemplaza a la versión del Documento Entregable 2; la versión anterior queda en el historial de Git. Cada imagen tiene aquí su descripción escrita, que es la referencia cuando un detalle de la imagen no se lea bien.

**Reglas:**
- **Trazabilidad:** toda pantalla del prototipo corresponde a un wireframe de esta carpeta. Si un elemento queda fuera del alcance de la iteración, se muestra deshabilitado («Próximamente») y se lista como pendiente en `docs/trazabilidad.md`.
- **Guía, no plano:** los wireframes fijan la estructura, el contenido y el orden de los elementos. El diseño visual es libre dentro de la identidad «Santa Marta» (`frontend/src/app/estilos.css`).
- **Fuente de requisitos:** si un wireframe contradice el SRS (`docs/srs/especificacion-de-requisitos.pdf`), manda el SRS y la diferencia se anota en la trazabilidad. Ejemplo: los wireframes dicen «aforo» donde el SRS dice «afluencia» (RF39).

## Elementos comunes

**Pantallas de la plataforma (WF-04 a WF-10):**
- **Encabezado:** logo «Samaria360»; navegación **Búsqueda, Servicios, Asistente IA**; selector **ES / EN** (RF03, RNF08); con sesión, avatar circular con las iniciales (p. ej. «MG»). Sin sesión, el prototipo muestra además el enlace «Inicio de sesión» (los wireframes solo dibujan el estado con sesión).
- **Pie de página:** logo; columnas **Explorar** (Alojamiento, Gastronomía, Tours, Alquiler de equipos), **Municipio** (Noticias), **Prestadores** (Registrar servicio, Panel del gestor), **Legal y Soporte** (Términos y condiciones, Políticas de privacidad, Preguntas frecuentes — RF46, RF26). Texto inferior: «© 2026 Alcaldía de Santa Marta · Samaria360».

**Pantallas de acceso (WF-01 a WF-03):**
- **Encabezado reducido:** solo logo y selector ES / EN.
- **Pie reducido:** «© 2026 Samaria360 · Plataforma turística verificada», Términos y condiciones, Políticas de privacidad, Ayuda / Soporte.

## Índice

| Archivo | Pantalla | Caso de uso | Requisitos | Alcance del prototipo (Elaboración) |
|---|---|---|---|---|
| WF-01-inicio-sesion.png | Inicio de sesión | Iniciar sesión | RF15, RF03, RF34, RF46 | **Sí** (sin recuperación de contraseña) |
| WF-02-registro-turista.png | Registro de turista | Registrarse | RF01 | No |
| WF-03-registro-prestador.png | Registro de prestador | Registrarse | RF02 | No |
| WF-04-inicio-busqueda.png | Inicio y búsqueda | Buscar oferta turística | RF16, RF40, RF04 | **Sí** (sin banner de avisos) |
| WF-05-exploracion.png | Exploración: mapa y resultados | Buscar oferta, filtrar, ordenar, navegar el mapa | RF16, RF10, RF23, RF27, RF38, RF39, RF50, RF61 | **Sí** (núcleo del catálogo, incluye «Más filtros») |
| WF-06-detalle.png | Detalle de lugar o servicio | Consultar detalle de oferta | RF37, RF39, RF51, RF54, RF57, RF58 | No |
| WF-07-asistente-ia.png | Asistente virtual IA | Interactuar con el asistente | RF04, RF35, RF36, RF59 | No |
| WF-08-registrar-servicio.png | Registrar servicio comercial | Registrar servicio | RF09, RF10, RF27, RF37, RF48, RF58 | No |
| WF-09-registrar-atractivo.png | Registrar atractivo turístico | Registrar atractivo | RF06, RF07, RF24, RF27, RF33 | No |
| WF-10-disponibilidad-reserva.png | Disponibilidad y reserva **(nuevo)** | Reservar servicio | RF11, RF20, RF37, RF44 | No |

## WF-01 — Inicio de sesión

- Subtítulo «Plataforma de turismo de Santa Marta».
- Pestañas **Iniciar sesión** (activa) / **Registrarse**.
- Texto: «Accede con el correo con el que te registraste o crea una cuenta nueva.»
- Tarjeta «Ingresa con tu cuenta»: **Correo electrónico** (icono de sobre, `nombre@correo.com`), **Contraseña** (icono de candado), enlace **¿Olvidaste tu contraseña?** (RF34), aviso **«3 intentos fallidos bloquearán la cuenta por 15 minutos.»** (RF15), botón **Iniciar sesión** con icono, texto legal sobre Términos, Condiciones y Políticas de Privacidad.
- Cambio respecto a la v1: ya no tiene el bloque «Plataforma» ni el título «Samaria360» grande; encabezado y pie reducidos.

## WF-02 — Registro de turista

Pestaña **Registrarse** activa; «Selecciona el tipo de cuenta que quieres registrar.»; opción **Turista** seleccionada. «Formulario – Turista»: primer nombre, segundo nombre (opcional), primer apellido, segundo apellido (opcional), correo electrónico, crear contraseña (mínimo 8 caracteres), país de origen (lista), categorías de interés (Naturaleza, Cultura, Aventura), casilla de Política de Privacidad y botón **Crear cuenta de turista**.

## WF-03 — Registro de prestador

Pestaña **Registrarse** activa; opción **Prestador de Servicios** seleccionada. «Formulario – Prestador de Servicios»: nombre del representante legal, número RNT, correo corporativo, teléfono, contraseña; aviso **«Su cuenta quedará Pendiente de Validación.»** (RF02); casilla de Política de Privacidad (nueva en la v2); botón **Registrar prestador**.

## WF-04 — Inicio y búsqueda

- **Banner de aviso** del municipio, cerrable (RF40). Ej.: cierre del sendero Cascada El Salto por lluvias.
- Titular «Encuentra alojamiento, gastronomía y tours en Santa Marta» y texto de apoyo.
- **Barra de búsqueda** con botón **Buscar**.
- «Explora por categoría»: **Alojamiento, Gastronomía, Tours, Alquiler de equipos**, con icono.
- «Destacados esta semana»: 3 tarjetas con imagen, nombre, categoría, calificación, precio «desde», disponibilidad y **Ver detalle**. Cambio respecto a la v1: sin el enlace «Reservar».

**Prototipo:** la búsqueda y las categorías llevan a WF-05 con el filtro aplicado. Banner de avisos: pendiente (no hay fuente de datos de avisos).

**Nota:** El botón flotante del asistente se retiró por decisión del equipo; el acceso a RF04 queda en la navegación.

## WF-05 — Exploración: mapa y resultados

- Migas de pan: Samaria360 › Exploración interactiva.
- Título «Exploración de Atractivos y Servicios en Santa Marta» y texto «Monitorea la afluencia en tiempo real, filtra por cercanía y planifica tus recorridos…».
- **Barra de filtros** «Filtros de búsqueda y afluencia»: texto («Filtrar por nombre, vereda o actividad»), **rango de precio**, **distancia máxima (km)**, casilla **acceso para personas con movilidad reducida**, botón **Aplicar filtros**.
- **Más filtros (adición del equipo, no dibujada en la imagen):** botón o panel desplegable que muestra filtros propios de la categoría elegida, a partir de los campos de RF10:
  - Alojamiento: servicios incluidos (WiFi, desayuno, piscina…), capacidad de huéspedes, tipo de habitación.
  - Gastronomía: opción vegetariana, tipo de cocina.
  - Tours: nivel de dificultad, duración máxima.
  - Alquiler de equipos: tipo de vehículo o equipo.
  - Sin categoría elegida, «Más filtros» invita a elegir una. Los atractivos no tienen campos por categoría, así que estos filtros solo aplican a servicios.
- **Mapa** con marcadores; leyenda **Bajo (< 40 %)**, **Medio (40–75 %)**, **Alto (> 75 %)**; **Centrar en mi ubicación**.
- «Resultados en Santa Marta (N lugares encontrados)» y orden: **Menor distancia, Menor afluencia, Calificación** (RF61 agrega precio).
- **Tarjetas:** imagen, nombre, descripción, calificación y reseñas, precio o rango, afluencia, distancia, accesibilidad, **Ver detalle**.
- **Paginación** numerada (RF16: 10 o menos por página).

## WF-06 — Detalle de lugar o servicio

Nombre; tipo, ubicación y calificación con reseñas; indicador de accesibilidad; etiquetas (Prestador verificado, Aforo en vivo, Reserva inmediata); favorito y compartir; galería (1 grande + 4 miniaturas); alerta de afluencia; descripción; información de contacto (teléfono, WhatsApp, dirección, correo, registro RNT); **Reportar información falsa o desactualizada** (RF57); tabla de precios; **Ver disponibilidad y reservar** (lleva a WF-10); horarios de atención (RF58); **Destinos alternativos con menor afluencia** (RF51, 3 tarjetas).

## WF-07 — Asistente virtual IA

Encabezado «Asistente Virtual IA» y estado «En línea»; aviso de versión beta; conversación; panel «Itinerario sugerido» por día y hora con **Compartir** y **Descargar PDF**; «Prestadores mencionados»; **Guardar itinerario en perfil** (RF35) y **Ver en el mapa**; chips rápidos (Aforo en vivo, Rutas alternas, Tours mañana, Alquiler de equipos); campo de texto y enviar; sección «El asistente también puede ayudarte con» (Aforos en vivo, Rutas y clima, Prestadores verificados).

## WF-08 — Registrar servicio comercial

Migas: Panel del Prestador › Mis Servicios › Registrar Nuevo Servicio. Aviso de verificación (hasta 2 días hábiles). Secciones: **Datos del servicio** (nombre, categoría, RNT, descripción); **Datos de contacto** (teléfono/WhatsApp, correo, dirección, horario de atención); **Precio** («desde» en COP, «cobra por», casilla «el precio incluye impuestos»); **Campos según la categoría** (con Alojamiento: número de habitaciones, capacidad máxima, servicios incluidos WiFi / Desayuno / Piscina / Parqueadero); **Fotos** (1 a 5, la primera es la portada — RF48); **Condiciones de accesibilidad**; casilla de veracidad; **Publicar servicio**.

## WF-09 — Registrar atractivo turístico

Migas: Inicio y búsqueda › Panel del gestor › Registrar atractivo. Secciones: **Datos del atractivo** (nombre, categoría, latitud, longitud); **Condiciones de accesibilidad**; **Aforo y estado operativo** (límite de capacidad de carga — RF07; estado Abierto / Cerrado temporalmente / En mantenimiento — RF33); **Fotos** (JPEG/PNG); **Normas y regulaciones ambientales** (máximo 1000 caracteres — RF24); casilla de veracidad; **Registrar atractivo**; consejos «Calcula el aforo», «Normas claras», «Actualiza el estado».

## WF-10 — Disponibilidad y reserva (nuevo)

Título «<Servicio> – Disponibilidad y reserva» con datos de verificación y RNT. **Buscar disponibilidad**: check-in, check-out, número de huéspedes y **Verificar disponibilidad en tiempo real** (RF11). Lista de unidades disponibles: imagen, tipo de alojamiento, disponibilidad, capacidad, etiquetas de servicios, estado de cupos, precio por noche y **Seleccionar habitación**; sin cupos, **Notificarme si se libera**. Panel **Resumen de reserva** (fechas, noches, huéspedes, precio × noches, impuestos, total), aviso de cancelación gratuita con 24 horas (RF44), **Confirmar reserva** (RF20) y «Información importante».

## Diferencias detectadas en la v2 (pendientes de resolver antes de implementar esas pantallas)

1. **WF-10 frente al modelo de datos (crítico):** la pantalla muestra varias unidades por servicio (p. ej. «Cabaña Campestre Familiar», «Cabaña Mirador del Mar»), cada una con su precio, capacidad, servicios y cupos. El modelo actual tiene un solo `tipoHabitacion` por servicio en `catalog_db` e `Inventario_Cupo` por servicio y fecha en `reservas_db`, sin entidad de unidad o tipo de habitación. Hay que extender el modelo o simplificar la pantalla a una unidad por servicio.
2. **Pagos en WF-10:** «Pago seguro con tarjeta, PSE o Nequi» no tiene requisito en el SRS ni componente en la arquitectura. «Notificarme si se libera» tampoco tiene requisito.
3. **Vocabulario de accesibilidad:** RF27 y RF50 definen tres condiciones (movilidad reducida, visual, auditiva). WF-08 usa otras cuatro (rampas, silla de ruedas, braille, baños adaptados) y WF-09 otras dos. Hay que unificarlas en un solo vocabulario.
4. **Vocabulario de servicios incluidos:** WF-08 ofrece WiFi, Desayuno, Piscina y Parqueadero; WF-10 muestra además Aire acondicionado y Baño privado. Debe existir una sola lista cerrada.
5. **WF-08 frente a RF10 y al esquema `servicios`:** falta el campo obligatorio «tipo de habitación»; «cobra por» e «incluye impuestos» no existen en el esquema; el RNT pertenece al prestador (`auth_db`), no a cada servicio.
6. **«Aforo» frente a «afluencia»:** los wireframes usan «aforo» para el indicador de RF39; el prototipo usa «afluencia», como el SRS.
