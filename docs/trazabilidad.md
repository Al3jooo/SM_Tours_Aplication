# Matriz de trazabilidad del prototipo arquitectónico

Fase de Construcción (RUP). Relaciona cada elemento de los wireframes (`docs/wireframes/`, **versión 2 del 12/10/2026**) con su caso de uso, sus requisitos (`docs/srs/`), el endpoint que lo atiende y el componente que lo implementa. Cada caso de uso nuevo agrega sus filas a esta matriz. Lo que cambió al pasar de la v1 a la v2 está en «Cambios de los wireframes v2».

**Estados:** **Implementado** (funciona de punta a punta) · **Deshabilitado** (visible en pantalla, sin funcionalidad, con la marca «Próximamente») · **Pendiente** (aún no aparece en el prototipo).

Rutas de componentes relativas a `frontend/src/`; endpoints de ms-auth bajo `/api/v1/auth`.

## Matriz principal

### WF-01 — Inicio de sesión

| Wireframe | Elemento | Caso de uso | RF/RNF | Endpoint | Componente frontend | Estado |
|---|---|---|---|---|---|---|
| WF-01 | Subtítulo «Plataforma de turismo de Santa Marta». Sin título visible (v2); el `h1` «Iniciar sesión en Samaria360» queda solo para lectores de pantalla | Iniciar sesión | RF15 | — | `features/auth/LoginPage.tsx` | Implementado |
| WF-01 | Pestaña «Iniciar sesión» | Iniciar sesión | RF15 | — | `features/auth/LoginPage.tsx` | Implementado |
| WF-01 | Pestañas «Iniciar sesión» / «Registrarse»: enlaces a `/login` y `/registro` con `aria-current` (antes, un `tablist` con «Registrarse» deshabilitado) | Registrarse | RF01, RF02 | — | `features/auth/PestanasAcceso.tsx` | Implementado |
| WF-01 | Texto introductorio | Iniciar sesión | RF15 | — | `features/auth/LoginPage.tsx` | Implementado |
| WF-01 | Campo «Correo electrónico» | Iniciar sesión | RF15 | `POST /login` | `features/auth/LoginPage.tsx` | Implementado |
| WF-01 | Campo «Contraseña» | Iniciar sesión | RF15, RNF05 | `POST /login` | `features/auth/LoginPage.tsx` | Implementado |
| WF-01 | Enlace «¿Olvidaste tu contraseña?» | Recuperar contraseña | RF34 | — | `features/auth/LoginPage.tsx` | Deshabilitado |
| WF-01 | Recuadro «3 intentos fallidos bloquearán la cuenta por 15 minutos» | Iniciar sesión | RF15 | — | `features/auth/LoginPage.tsx` | Implementado |
| WF-01 | Botón «Iniciar sesión» | Iniciar sesión | RF15 | `POST /login`, `GET /me` | `features/auth/LoginPage.tsx`, `features/auth/SesionProvider.tsx` | Implementado |
| WF-01 | Mensaje de credenciales inválidas (401) | Iniciar sesión | RF15 | `POST /login` | `features/auth/mensajesError.ts` | Implementado |
| WF-01 | Mensaje de cuenta bloqueada con hora de desbloqueo (423) | Iniciar sesión | RF15 | `POST /login` | `features/auth/mensajesError.ts` | Implementado |
| WF-01 | Mensaje de cuenta inactiva (403), solo con la contraseña correcta | Iniciar sesión | RF22, RF62 | `POST /login` | `features/auth/mensajesError.ts` | Implementado |
| WF-01 | Mensajes de prestador sin validar (403, con la contraseña correcta): «Tu cuenta está en revisión por el administrador» (`CUENTA_PENDIENTE_VALIDACION`) y cuenta rechazada (`CUENTA_RECHAZADA`, mensaje genérico: el modelo no guarda el motivo) — decisión D3 | Iniciar sesión | RF02, RF28 | `POST /login` | `features/auth/mensajesError.ts` | Implementado |
| WF-01 ← WF-02, WF-03 | Mensaje tras el registro, en `role="status"` («Registro recibido. Inicia sesión con tu correo» / «Solicitud recibida; quedará pendiente de validación») y correo prellenado; ambos llegan por el estado del router, nunca por la URL (ADR-006) | Registrarse | RF01, RF02 | — | `features/auth/LoginPage.tsx`, `features/auth/estadoAcceso.ts` | Mejora de diseño |
| WF-01 | Mensaje de servicio no disponible (502, 503 o 504, o `SERVICIO_NO_DISPONIBLE`): «El servicio no está disponible por el momento…»; un 500 muestra el mensaje genérico | Iniciar sesión | RF15, RNF01 | `POST /login` | `features/auth/mensajesError.ts`, `shared/api/httpClient.ts` | Implementado |
| WF-01 | Texto legal «Al iniciar sesión aceptas los Términos…» | Iniciar sesión | RF46 | — | `features/auth/LoginPage.tsx` | Implementado (texto; la página legal está pendiente) |
| WF-01 | Disposición v2: subtítulo, pestañas e introducción alineados a la izquierda; tarjeta centrada; sin imagen | Iniciar sesión | RF15 | — | `features/auth/LoginPage.tsx`, `features/auth/LoginPage.css` | Implementado |
| WF-01 → WF-04 | Redirección tras el login a la pantalla de inicio WF-04 (o a la ruta privada pedida) | Iniciar sesión | RF15 | `GET /me` | `features/auth/LoginPage.tsx`, `features/catalogo/InicioPage.tsx` | Implementado |

### WF-02 y WF-03 — Registrarse

CU **Registrarse** ↔ **RF01** (turista, WF-02) y **RF02** (prestador, WF-03). Pantalla `/registro` con el diseño de acceso (encabezado y pie reducidos). Decisiones D1 a D6 y ADR-006 más abajo.

| Wireframe | Elemento | RF | Endpoint | Componente frontend | Pruebas | Estado |
|---|---|---|---|---|---|---|
| WF-02, WF-03 | Pestaña «Registrarse» activa, «Selecciona el tipo de cuenta…» y radios Turista / Prestador de Servicios | RF01, RF02 | — | `features/auth/registro/RegistroPage.tsx`, `PestanasAcceso.tsx` | `registro.test.tsx` | Implementado |
| WF-02 | Primer nombre, segundo nombre (opcional), primer apellido, segundo apellido (opcional), correo, crear contraseña (mínimo 8) | RF01, RNF05 | `POST /registro/turista` | `registro/FormularioTurista.tsx` | `RegistroTest`, `ValoresRegistroTest`, `validacionRegistro.test.ts`, `registro.test.tsx` | Implementado |
| WF-02 | País de origen (lista): códigos ISO 3166-1 alfa-2 del backend, nombres traducidos y ordenados en el idioma activo (D5) | RF01, RF03 | `GET /paises` | `registro/usePaises.ts`, `registro/paises.ts` | `ValoresRegistroTest`, `RegistroApiIntegracionTest`, `validacionRegistro.test.ts`, `registro.test.tsx` | Implementado |
| WF-02 | Categorías de interés Naturaleza, Cultura, Aventura: al menos una, sin repetir (D1) | RF01, RF53 | `POST /registro/turista` | `registro/FormularioTurista.tsx` | `RegistroTest`, `MigracionesRegistroIntegracionTest` (CHECK V4), `registro.test.tsx` | Implementado |
| WF-02, WF-03 | Casilla «Acepto la Política de Privacidad y el tratamiento de mis datos» (400 `POLITICAS_NO_ACEPTADAS`, mismo código para los dos roles; D6) | RF01; derivado de E1 §3.2.7.4 | `POST /registro/*` | `CasillaPoliticas` | `RegistroTest`, `RegistroApiIntegracionTest`, `registro.test.tsx` | Implementado |
| WF-02 | Botón «Crear cuenta de turista»: cuenta Activa; vuelve al login, sin inicio de sesión automático (ADR-006) | RF01 | `POST /registro/turista` (202) | `registro/FormularioTurista.tsx` | `RegistrarServiceTest`, `RegistroApiIntegracionTest`, `registro.test.tsx` | Implementado |
| WF-03 | Nombre del representante legal, número RNT, correo corporativo (también credencial de acceso, D2), teléfono, contraseña | RF02 | `POST /registro/prestador` | `registro/FormularioPrestador.tsx` | `RegistroTest`, `RegistroApiIntegracionTest`, `registro.test.tsx` | Implementado |
| WF-03 | Aviso «Tu cuenta quedará pendiente de validación hasta que un administrador la revise», fijo (`role="note"`, sin botón de cerrar: es información obligatoria de RF02) | RF02 | — | `registro/FormularioPrestador.tsx` | `registro.test.tsx` | Implementado |
| WF-02, WF-03 | Ayudas visibles y conectadas con `aria-describedby`: «Mínimo 8 caracteres.» (contraseña, en los dos formularios) y «Elige al menos una.» (categorías); `autocomplete` (given-name, additional-name, family-name, email, new-password, country, tel, name) | RF01, RF02 | — | `registro/Campo.tsx`, formularios | `registro.test.tsx` | Mejora de diseño |
| WF-01, WF-02, WF-03 | Subtítulo, pestañas, introducción, opciones y tarjeta en una sola columna centrada, del ancho de la tarjeta, igual en `/login` y `/registro` (la v2 dibuja el encabezado a la izquierda) | RF15, RF01, RF02 | — | `LoginPage.css`, `RegistroPage.css` | capturas | Mejora de diseño |
| WF-03 | Botón «Registrar prestador»: `estado_validacion = Pendiente`; no puede iniciar sesión hasta que lo validen (D3) | RF02, RF28 | `POST /registro/prestador` (202) | `registro/FormularioPrestador.tsx` | `RegistroApiIntegracionTest`, `IniciarSesionIntegracionTest`, `registro.test.tsx` | Implementado |
| WF-02, WF-03 | Validación en el cliente con los criterios y códigos del backend; errores por campo (`aria-invalid`, `aria-describedby`), foco en el primero y resumen en `role="alert"`; ES y EN | RF01, RF02, RF03 | — | `registro/validacionRegistro.ts`, `registro/Campo.tsx`, `registro/envioRegistro.ts` | `validacionRegistro.test.ts`, `registro.test.tsx` | Implementado |
| WF-02, WF-03 | Errores del backend en su campo: `DATOS_INVALIDOS` (códigos `REQUERIDO`, `FORMATO`, `LONGITUD_MINIMA`, `LONGITUD_MAXIMA`, `NO_PERMITIDO`, `DUPLICADO` con las claves JSON), `POLITICAS_NO_ACEPTADAS`, `RNT_YA_REGISTRADO`; 502, 503 o 504, aviso general | RF01, RF02 | `POST /registro/*` | `registro/envioRegistro.ts` | `RegistroApiIntegracionTest`, `registro.test.tsx` | Implementado |
| WF-03 | Razón social: no está en el formulario (opcional en el modelo desde V5, D2) | RF02 | — | — | `MigracionesRegistroIntegracionTest` | Mejora de diseño (modelo) |

### WF-04 — Inicio y búsqueda

| Wireframe | Elemento | Caso de uso | RF/RNF | Endpoint | Componente frontend | Estado |
|---|---|---|---|---|---|---|
| WF-04 | Titular y texto de apoyo | Buscar oferta turística | RF16 | — | `features/catalogo/InicioPage.tsx` | Implementado |
| WF-04 | Barra de búsqueda «¿Qué buscas?» → `/explorar?texto=…` | Buscar oferta turística | RF16 | — | `features/catalogo/InicioPage.tsx` | Implementado |
| WF-04 | «Explora por categoría»: Alojamiento, Gastronomía, Tours, Alquiler de equipos → `/explorar?categoria=…` | Buscar oferta turística | RF16 | — | `features/catalogo/InicioPage.tsx` | Implementado |
| WF-04 | «Destacados esta semana»: 3 servicios (imagen, nombre, categoría, precio «desde») | Buscar oferta turística | RF16, RF37 | `GET /oferta?tamano=3&orden=PrecioAscendente` | `features/catalogo/InicioPage.tsx`, `componentes/TarjetaOferta.tsx` | Implementado (criterio provisional: ver Pendientes) |
| WF-04 | Calificación y disponibilidad en los destacados | Buscar oferta turística | RF17, RF11 | — | `componentes/TarjetaOferta.tsx` | Deshabilitado («—», sin datos) |
| WF-04 | «Ver detalle» (v2: sin «Reservar»; la reserva se hará desde el detalle, WF-06 → WF-10) | Consultar detalle de oferta | RF37 | — | `componentes/TarjetaOferta.tsx` | Deshabilitado |
| WF-04 | Banner de aviso del municipio (cerrable) | Publicar avisos | RF40 | — | — | Pendiente (no existe el modelo de avisos) |
| WF-04, WF-05 | Foto real de cada lugar (21, WebP 3:2, como máximo 1200×800, sin metadatos), decorativa (`alt=""`: el nombre está en el `h3`) | Buscar oferta turística | RF23, RF37 | `GET /oferta` (`imagen`) | `componentes/ImagenOferta.tsx`, `public/imagenes/catalogo/`, `scripts/optimizar-imagenes.mjs` | Implementado (sin créditos verificados; ver `docs/recursos-graficos/README.md`) |
| WF-04, WF-05 | Ilustración propia por categoría cuando falta la imagen del elemento | Buscar oferta turística | RF23 | — | `componentes/ImagenOferta.tsx`, `public/imagenes/catalogo/categorias/` | Implementado |

### WF-05 — Exploración: mapa y resultados

| Wireframe | Elemento | Caso de uso | RF/RNF | Endpoint | Componente frontend | Estado |
|---|---|---|---|---|---|---|
| WF-05 | Migas de pan «Samaria360 › Exploración interactiva» (v2: dos niveles), título y texto de apoyo | Buscar oferta turística | RF16 | — | `features/catalogo/ExplorarPage.tsx` | Implementado |
| WF-05 | Filtro de texto «Filtrar por nombre, vereda o actividad» | Buscar oferta turística | RF16 | `GET /oferta?texto=` | `componentes/FiltrosOferta.tsx` | Implementado |
| WF-05 | Precio máximo (deslizador). La v2 dibuja «Rango de precio», pero RF16 pide «precio máximo»: manda el SRS (ver «Diferencias con el SRS») | Buscar oferta turística | RF16, RF37 | `GET /oferta?precioMax=` | `componentes/FiltrosOferta.tsx` | Implementado |
| WF-05 | Distancia máxima (deslizador); sin ubicación, deshabilitado con explicación | Buscar oferta turística | RF38 | `GET /oferta?lat=&lng=&distanciaKm=` | `componentes/FiltrosOferta.tsx` | Implementado |
| WF-05 | Accesibilidad: WF-05 muestra solo «movilidad reducida»; se muestran las 3 condiciones de RF50 | Buscar oferta turística | RF50, RF27 | `GET /oferta?accesibilidad=` | `componentes/FiltrosOferta.tsx` | Mejora de diseño |
| WF-05 | Chip removible de la categoría elegida en WF-04 | Buscar oferta turística | RF16 | `GET /oferta?categoria=` | `componentes/FiltrosOferta.tsx` | Mejora de diseño |
| WF-05 | Botón «Aplicar filtros» (aplica también «Más filtros») | Buscar oferta turística | RF16 | — | `componentes/FiltrosOferta.tsx` | Implementado |
| WF-05 | «Más filtros»: botón con `aria-expanded` y panel desplegable dentro del formulario; al abrir, el foco va a la categoría; con Escape o «Cerrar» vuelve al botón; una columna a 390 px | Buscar oferta turística | RF10, RF16 | — | `componentes/MasFiltros.tsx` | Implementado |
| WF-05 | «Más filtros»: categoría en el panel (Todas, Alojamiento, Gastronomía, Tours, Alquiler de equipos). Sin categoría, invita a elegir una; al cambiarla, se limpian los filtros que ya no aplican; «Todas» quita la categoría y sus filtros, y vuelven los atractivos (decisión 2) | Buscar oferta turística | RF10, RF16 | `GET /oferta?categoria=` | `componentes/MasFiltros.tsx` | Implementado |
| WF-05 | «Más filtros» de Alojamiento: servicios incluidos (WiFi, Desayuno, Piscina, Parqueadero; el alojamiento debe tenerlos todos), huéspedes (capacidad máxima ≥ huéspedes) y tipo de habitación. Huéspedes, tipo de habitación y precio máximo los debe cumplir **una misma unidad** del alojamiento (D1 de «Reservar servicio») | Buscar oferta turística | RF10, RF16 | `GET /oferta?categoria=Alojamiento&serviciosIncluidos=&huespedes=&tipoHabitacion=` | `componentes/MasFiltros.tsx` | Implementado |
| WF-05 | «Más filtros» de Gastronomía: «Opción vegetariana» (`menuVegetariano`; ver «Diferencias con el SRS») y tipo de cocina | Buscar oferta turística | RF10, RF16 | `GET /oferta?categoria=Gastronomia&menuVegetariano=true&tipoCocina=` | `componentes/MasFiltros.tsx` | Implementado |
| WF-05 | «Más filtros» de Tours: nivel de dificultad y duración máxima (deslizador entre la menor y la mayor duración publicadas) | Buscar oferta turística | RF10, RF16 | `GET /oferta?categoria=Tour&nivelDificultad=&duracionMaxHoras=` | `componentes/MasFiltros.tsx` | Implementado |
| WF-05 | «Más filtros» de Alquiler de equipos: tipo de vehículo o equipo | Buscar oferta turística | RF10, RF16 | `GET /oferta?categoria=AlquilerEquipo&tipoVehiculoEquipo=` | `componentes/MasFiltros.tsx` | Implementado |
| WF-05 | Opciones de «Más filtros» tomadas de los servicios publicados de prestadores activos (cada opción devuelve resultados; no hay listas fijas en el frontend, salvo las etiquetas traducidas de los servicios incluidos). Si no cargan, aviso con «Reintentar» | Buscar oferta turística | RF10, RF16, RF22 | `GET /filtros?categoria=` | `features/catalogo/opcionesFiltroApi.ts`, `features/catalogo/useOpcionesFiltro.ts` | Implementado |
| WF-05 | Chips removibles de cada filtro de «Más filtros» y contador de filtros activos en el botón | Buscar oferta turística | RF10, RF16 | `GET /oferta` | `componentes/FiltrosOferta.tsx`, `componentes/MasFiltros.tsx` | Mejora de diseño |
| WF-05 | Una URL con filtros de «Más filtros» de otra categoría, sin categoría o con valores inválidos se limpia sin mostrar un error (la API respondería 400) | Buscar oferta turística | RF10, RF16 | — | `features/catalogo/criteriosUrl.ts`, `features/catalogo/ExplorarPage.tsx` | Mejora de diseño |
| WF-05 | Filtros, orden y página en la URL (Atrás, recarga y enlace compartido; la ubicación nunca va en la URL) | Buscar oferta turística | RF16 | — | `features/catalogo/criteriosUrl.ts` | Implementado |
| WF-05 | «Resultados en Santa Marta (N lugares encontrados)», con plurales, en región `aria-live` | Buscar oferta turística | RF16 | — | `features/catalogo/ExplorarPage.tsx` | Implementado |
| WF-05 | Orden «Menor distancia»: el orden por defecto con ubicación (si la URL no trae otro); sin ubicación, deshabilitado con explicación | Buscar oferta turística | RF61, RF38 | `GET /oferta?lat=&lng=` (`orden=Distancia` si se elige) | `features/catalogo/ExplorarPage.tsx` | Implementado |
| WF-05 | Orden «Menor afluencia» | Buscar oferta turística | RF39, RF51 | — | `features/catalogo/ExplorarPage.tsx` | Deshabilitado («Próximamente») |
| WF-05 | Orden «Calificación» | Buscar oferta turística | RF61 | — | `features/catalogo/ExplorarPage.tsx` | Deshabilitado («Próximamente») |
| WF-05 | Orden «Precio: menor a mayor», «Precio: mayor a menor» y «Nombre» (no están en WF-05; la API los soporta) | Buscar oferta turística | RF61 | `GET /oferta?orden=` | `features/catalogo/ExplorarPage.tsx` | Mejora de diseño |
| WF-05 | Tarjetas: imagen, nombre, descripción corta, precio en COP según el idioma, distancia, iconos de accesibilidad | Buscar oferta turística | RF16, RF37, RF38, RF50 | `GET /oferta` | `componentes/TarjetaOferta.tsx` | Implementado |
| WF-05 | Insignia de estado operativo en atractivos (Abierto, Cerrado temporalmente, En mantenimiento) — decisión 5 | Buscar oferta turística | RF33, RF08 | `GET /oferta` | `componentes/TarjetaOferta.tsx` | Implementado |
| WF-05 | Calificación y afluencia (atractivos, RF39) o disponibilidad (servicios, RF11) en las tarjetas. WF-05 dice «nivel de aforo»; se usa «afluencia», el término del SRS | Buscar oferta turística | RF17, RF39, RF11 | — | `componentes/TarjetaOferta.tsx` | Deshabilitado («—», sin datos) |
| WF-05 | «Ver detalle» | Consultar detalle de oferta | RF37 | — | `componentes/TarjetaOferta.tsx` | Deshabilitado |
| WF-05 | Paginación numerada (10 por página) | Buscar oferta turística | RF16 | `GET /oferta?pagina=` | `componentes/Paginacion.tsx` | Implementado |
| WF-05 | Estados: carga, sin resultados («Quitar filtros»), página fuera de rango («Ir a la primera página»), 502, 503 o 504 (o `SERVICIO_NO_DISPONIBLE`) «El catálogo no está disponible» con «Reintentar», 400 «Revisa los filtros»; un 500 sigue siendo «Ocurrió un error inesperado» | Buscar oferta turística | RF16, RNF01 | `GET /oferta` | `componentes/EstadoCatalogo.tsx` | Implementado |
| WF-05 | Mapa con marcadores de servicios y atractivos de la página actual (Leaflet + teselas de OpenStreetMap), encuadre de los resultados, enfocable con el teclado y control de zoom traducido | Navegar mapa | RF23 | `GET /oferta` | `componentes/MapaOferta.tsx` | Implementado |
| WF-05 | Leyenda de afluencia (WF-05: «aforo»): Bajo (< 40 %), Medio (40–75 %), Alto (> 75 %) | Buscar oferta turística | RF39 | — | `features/catalogo/ExplorarPage.tsx` | Deshabilitado («Sin datos de aforo») |
| WF-05 | «Centrar en mi ubicación»: pide la ubicación solo al pulsarlo, la guarda solo en memoria (ni URL ni almacenamiento del navegador) y la envía a la búsqueda | Navegar mapa | RF38 | `GET /oferta?lat=&lng=` | `componentes/BotonUbicacion.tsx`, `features/catalogo/useUbicacion.ts` | Implementado |
| WF-05 | Selector «Lista / Mapa» en una columna (menos de 64 rem); en escritorio lista y mapa lado a lado | Navegar mapa | RNF04 | — | `features/catalogo/ExplorarPage.tsx` | Implementado |
| WF-05 | Marcador «Tu ubicación» distinto de los de servicios y atractivos | Navegar mapa | RF38 | — | `componentes/MapaOferta.tsx` | Mejora de diseño |
| WF-05 | Ubicación lejos de Santa Marta (más de 60 km del Parque de los Novios): se usa para distancias y orden, no entra en el encuadre del mapa y se avisa «Estás lejos de Santa Marta…» | Navegar mapa | RF38 | `GET /oferta?lat=&lng=` | `features/catalogo/geo.ts`, `componentes/MapaOferta.tsx` | Mejora de diseño |
| WF-05 | Avisos de ubicación en `role="status"`: permiso denegado, ubicación no disponible, tiempo agotado; navegador sin geolocalización (botón deshabilitado con la razón) | Navegar mapa | RF38 | — | `features/catalogo/ExplorarPage.tsx`, `componentes/BotonUbicacion.tsx` | Mejora de diseño |
| WF-05 | Resaltado marcador↔tarjeta: pulsar un marcador resalta y enfoca su tarjeta; el puntero o el foco sobre una tarjeta resaltan su marcador | Navegar mapa | RF23 | — | `componentes/MapaOferta.tsx`, `componentes/TarjetaOferta.tsx` | Mejora de diseño |
| WF-05 | Rueda del mouse en el mapa solo con foco o clic, con la indicación «Haz clic en el mapa o enfócalo para usar la rueda» | Navegar mapa | RF23 | — | `componentes/MapaOferta.tsx` | Mejora de diseño |

### Elementos comunes

Pantallas de la plataforma (WF-04 a WF-10) y pantallas de acceso (WF-01 a WF-03), según «Elementos comunes» de `docs/wireframes/README.md` (v2).

| Wireframe | Elemento | Caso de uso | RF/RNF | Endpoint | Componente frontend | Estado |
|---|---|---|---|---|---|---|
| Común | Logo «Samaria360» (lleva al inicio) | — | — | — | `shared/components/Encabezado.tsx` | Implementado |
| Común | Navegación «Inicio de sesión»: solo sin sesión. Con la sesión iniciada (y mientras se valida la sesión guardada, hasta 8 s) se oculta; vuelve al cerrar sesión o si la sesión expira, sin recargar. WF-04 y WF-05 muestran el enlace y el avatar a la vez; con sesión el enlace no tiene sentido y se oculta | Iniciar sesión | RF15, RF55 | `GET /me` | `shared/components/Encabezado.tsx`, `app/DisenoPrincipal.tsx` | Implementado |
| Común | `/login` con la sesión iniciada redirige a la ruta de origen (solo rutas internas: empieza con «/» y no con «//») o al inicio, sin mostrar el formulario | Iniciar sesión | RF15 | — | `features/auth/LoginPage.tsx`, `features/auth/destinoTrasLogin.ts` | Implementado |
| Común | Navegación «Búsqueda» (lleva a `/explorar`) | Buscar oferta turística | RF16 | `GET /api/v1/catalogo/oferta` | `shared/components/Encabezado.tsx` | Implementado |
| Común | Navegación «Servicios» | Buscar oferta turística | RF16 | — | `shared/components/Encabezado.tsx` | Deshabilitado |
| Común | Navegación «Asistente IA» | Interactuar con asistente | RF04 | — | `shared/components/Encabezado.tsx` | Deshabilitado |
| Común | Selector de idioma ES / EN | Cambiar idioma | RF03, RNF08 | — | `shared/components/SelectorIdioma.tsx`, `i18n/` | Implementado |
| Común | Avatar con iniciales de `nombreVisible` | Iniciar sesión | RF15, RNF06 | `GET /me` | `shared/components/MenuUsuario.tsx` | Implementado |
| Común | Menú del avatar: «Cerrar sesión» | Cerrar sesión | RF55 | — (el token se descarta en el cliente) | `shared/components/MenuUsuario.tsx`, `features/auth/SesionProvider.tsx` | Implementado |
| Común | Restauración de la sesión al cargar la app; cierre ante 401/403 | Iniciar sesión | RF15, RF22, RF62 | `GET /me` | `features/auth/SesionProvider.tsx`, `shared/api/httpClient.ts` | Implementado |
| Común | Rutas protegidas (sin sesión → login y regreso a la ruta pedida). Hoy no hay rutas privadas: inicio y exploración son públicas | Iniciar sesión | RNF06 | — | `app/RutaProtegida.tsx` | Implementado (sin uso hasta las pantallas de prestador y gestor) |
| Común | Aviso de sesión vencida en páginas públicas: «Tu sesión expiró» (401) o «Tu cuenta no está activa» (403), con enlace al login; el usuario se queda en la página | Iniciar sesión | RF15, RF22, RF62 | `GET /me` | `features/auth/AvisoSesion.tsx` | Mejora de diseño |
| Común | Pie, columna Explorar: Alojamiento, Gastronomía, Tours, Alquiler de equipos → `/explorar?categoria=…` | Buscar oferta turística | RF16 | `GET /oferta?categoria=` | `shared/components/PiePagina.tsx` | Implementado |
| Común | Pie, columna Municipio: Noticias | Publicar avisos | RF40 | — | `shared/components/PiePagina.tsx` | Deshabilitado |
| Común | Pie, columna Prestadores: Registrar servicio, Panel del gestor | Registrar servicio / Gestionar atractivos | RF09, RF64 | — | `shared/components/PiePagina.tsx` | Deshabilitado |
| Común | Pie, columna Legal y Soporte: Términos y condiciones, Políticas de privacidad, Preguntas frecuentes | Consultar información legal / Consultar preguntas frecuentes | RF46, RF26 | — | `shared/components/PiePagina.tsx` | Deshabilitado |
| Común | Pie: «© 2026 Alcaldía de Santa Marta · Samaria360» | — | — | — | `shared/components/PiePagina.tsx` | Implementado |
| Común | Pie: aviso «Prototipo académico — Universidad del Magdalena. Las imágenes son de referencia y pueden pertenecer a terceros; no se usan con fines comerciales.» (ES/EN), en lugar de una página de créditos | — | — | — | `shared/components/PiePagina.tsx` | Mejora de diseño |
| Acceso (WF-01 a WF-03) | Encabezado reducido: logo y ES / EN, sin navegación ni avatar | — | RF03 | — | `shared/components/Encabezado.tsx` (`EncabezadoAcceso`), `app/DisenoAcceso.tsx` | Implementado |
| Acceso (WF-01 a WF-03) | Pie reducido: «© 2026 Samaria360 · Plataforma turística verificada» y el aviso académico | — | — | — | `shared/components/PiePagina.tsx` (`PieAcceso`) | Implementado |
| Acceso (WF-01 a WF-03) | Pie reducido: Términos y condiciones, Políticas de privacidad, Ayuda / Soporte | Consultar información legal | RF46, RF26 | — | `shared/components/PiePagina.tsx` (`PieAcceso`) | Deshabilitado |
| Acceso (WF-01 a WF-03) | Se conservan «Saltar al contenido» y el aviso de sesión vencida | — | — | — | `app/DisenoAcceso.tsx` | Implementado |

### Endpoints de ms-auth

| Wireframe | Elemento | Caso de uso | RF/RNF | Endpoint | Componente frontend | Estado |
|---|---|---|---|---|---|---|
| WF-01 | Inicio de sesión con correo y contraseña; bloqueo de 15 min tras 3 fallos. Orden: 423 (lo único antes de la contraseña); contraseña (401 y un intento más, sea cual sea el estado); con la contraseña correcta, 403 `CUENTA_INACTIVA` (RF22) o `CUENTA_PENDIENTE_VALIDACION` / `CUENTA_RECHAZADA` (D3), sin emitir token y con el conteo en 0; token y acceso registrado | Iniciar sesión | RF15, RF22, RF62, RF02, RF28, RNF05 | `POST /api/v1/auth/login` | `features/auth/authApi.ts` | Implementado |
| WF-02 | Registro de turista: 202 `{"estado":"RECIBIDO"}` siempre igual; 400 `DATOS_INVALIDOS` con un código por campo o `POLITICAS_NO_ACEPTADAS` | Registrarse | RF01, RNF05 | `POST /api/v1/auth/registro/turista` | `features/auth/authApi.ts` | Implementado |
| WF-03 | Registro de prestador: igual que el de turista, más 409 `RNT_YA_REGISTRADO` | Registrarse | RF02, RNF05 | `POST /api/v1/auth/registro/prestador` | `features/auth/authApi.ts` | Implementado |
| WF-02 | Países admitidos: códigos ISO 3166-1 alfa-2, públicos, con `Cache-Control: max-age=86400, public` | Registrarse | RF01 | `GET /api/v1/auth/paises` | `features/auth/authApi.ts` | Implementado |
| Común | Datos del usuario autenticado (`nombreVisible` para el avatar); 403 si la cuenta dejó de estar activa | Iniciar sesión | RF15, RF22, RF62, RNF06 | `GET /api/v1/auth/me` | `features/auth/authApi.ts` | Implementado |
| — | Clave pública para validar el JWT RS256 en el API Gateway | Iniciar sesión | RNF06 | `GET /api/v1/auth/.well-known/jwks.json` | — (lo consume el API Gateway) | Implementado |

### Endpoints de ms-catalog

| Wireframe | Elemento | Caso de uso | RF/RNF | Endpoint | Componente frontend | Estado |
|---|---|---|---|---|---|---|
| WF-04, WF-05 | Búsqueda pública combinada de servicios y atractivos: texto, categoría, «Más filtros» de la categoría, precio máximo, ubicación y distancia, accesibilidad, orden (distancia, precio, nombre) y paginación de 10. Cada filtro de «Más filtros» exige su categoría: sin ella o con otra, 400 `CRITERIOS_INVALIDOS` («tipoCocina requiere categoria=Gastronomia»); un servicio incluido desconocido, 400 con los valores aceptados y el recibido recortado a 40 caracteres. Los textos se comparan completos, sin distinguir mayúsculas pero sí tildes, igual en `PoliticaBusqueda` y en la agregación. **El `precio` de cada servicio es «desde» según los filtros activos:** con filtros de unidad (huéspedes o tipo de habitación), el menor `precioNoche` de las unidades que los cumplen (con el precio máximo, si lo hay); si no, `precioBase`. El orden por precio usa ese mismo valor (`CondicionesUnidad` arma las condiciones del `$elemMatch` y del `$filter`) | Buscar oferta turística | RF10, RF16, RF23, RF37, RF38, RF50, RF61, RNF02 | `GET /api/v1/catalogo/oferta` | `features/catalogo/` (WF-04 y WF-05) | Implementado |
| WF-05 | Opciones de «Más filtros» de una categoría (servicios publicados de prestadores activos), pública; sin categoría o con una inválida, 400 `CRITERIOS_INVALIDOS`. En Alojamiento, los tipos de habitación y el rango de huéspedes salen de todas las unidades | Buscar oferta turística | RF10, RF16, RF22 | `GET /api/v1/catalogo/filtros?categoria=` | `features/catalogo/opcionesFiltroApi.ts` | Implementado |
| WF-06, WF-10 | Detalle público de un servicio: `id`, `idPrestador`, `nombreComercial`, `categoria`, `descripcion`, `imagenes` (la primera es la portada), `precioBase` («desde»), `moneda` (COP) y `detalleCategoria`; en Alojamiento, `serviciosIncluidos` y `unidades` (`idUnidad`, `nombre`, `tipoHabitacion`, `numHabitaciones`, `capacidadMaxHuespedes`, `precioNoche`). Solo servicios Publicados de prestadores activos: si no, o con un id que no es un ObjectId, 404 `OFERTA_NO_DISPONIBLE`. Es el endpoint que consulta ms-booking antes de reservar (ADR-007) | Consultar detalle de oferta, Reservar servicio | RF10, RF16, RF22, RF37 | `GET /api/v1/catalogo/servicios/{id}` | — (lo usarán WF-06 y WF-10) | Implementado (backend) |

### Endpoints de ms-booking

Bajo `/api/v1/reservas`. Contrato completo en [`sad/api/ms-booking.md`](sad/api/ms-booking.md); decisiones en «Reservar servicio — decisiones». WF-10 se construye en la iteración siguiente: hoy no hay pantalla.

| Wireframe | Elemento | Caso de uso | RF/RNF | Endpoint | Componente frontend | Estado |
|---|---|---|---|---|---|---|
| WF-10 | Reservar: confirmación automática con descuento inmediato de cupos; una unidad por reserva (D1); precio calculado en el servidor y congelado (RF32), 409 `PRECIO_CAMBIADO` si no coincide con `precioTotalEsperado` (RF37); `Idempotency-Key` obligatoria (D6). Pruebas: `ReservaApiIntegracionTest`, `ConcurrenciaIntegracionTest`, `PoliticaReservaTest` | Reservar servicio | RF11, RF20, RF32, RF37 | `POST /api/v1/reservas` | — (WF-10, iteración siguiente) | Implementado (backend) |
| WF-11 | Mis reservas (RF12, del lado del turista): filtros opcionales por rango de fechas de la **actividad** (`desde` y `hasta` de Santa Marta, inclusivos; la reserva aparece si su actividad cruza el rango) y por uno o varios estados; 20 por página, por inicio descendente con desempate por id (índice de V2). Una reserva propia (la ajena es 404). Responden con lo guardado, sin consultar el catálogo, e incluyen `cancelableHasta`, calculado por el servidor. Pruebas: `MisReservasIntegracionTest`, `FiltroReservasTest` | Reservar servicio | RF12, RF20, RF32, RF44 | `GET /api/v1/reservas/mias?desde=&hasta=&estado=`, `GET /api/v1/reservas/{id}` | — (WF-11, siguiente parte) | Implementado (backend) |
| WF-11 | Paso automático a Completada (RF52): tarea programada al arrancar y cada 15 minutos; UPDATE condicional idempotente `estado = 'Confirmada' AND fecha_fin <= ahora` (índice parcial de V3). Si coincide con una cancelación, gana una de las dos: la cancelación tardía responde `RESERVA_NO_CANCELABLE` y los cupos no cambian. Pruebas: `CompletarReservasServiceTest`, `ConcurrenciaIntegracionTest` | Reservar servicio | RF52 | — (tarea programada `CompletarReservasProgramador`) | — | Implementado |
| WF-10 | Cancelar hasta 24 h antes del inicio (límite incluido); idempotente; devuelve los cupos a las mismas noches u horario. Pruebas: `ReservaTest`, `CancelarReservaServiceTest`, `ConcurrenciaIntegracionTest` | Reservar servicio | RF18, RF44 | `POST /api/v1/reservas/{id}/cancelacion` | — (iteración siguiente) | Implementado (backend) |
| WF-10 | Disponibilidad pública por unidad y noche (alojamiento) o por horario (tour), hasta 60 días | Reservar servicio | RF11 | `GET /api/v1/reservas/disponibilidad` | — (WF-10, iteración siguiente) | Implementado (backend) |
| WF-04, WF-05 | Resumen para las tarjetas («Disponibilidad»): `CON_CUPOS`, `AGOTADO` o `SIN_INVENTARIO`, hasta 10 servicios | Buscar oferta turística | RF11, RF16 | `GET /api/v1/reservas/disponibilidad/resumen` | — (tarjetas, iteración siguiente) | Implementado (backend) |

### API Gateway

| Wireframe | Elemento | Caso de uso | RF/RNF | Endpoint | Componente frontend | Estado |
|---|---|---|---|---|---|---|
| Todas | Único punto de entrada (`:8080`): enrutamiento de `/api/v1/auth/**`, `/api/v1/catalogo/**` y `/api/v1/reservas/**`; timeouts y circuit breaker por ruta con fallback 503 `SERVICIO_NO_DISPONIBLE` (`servicio`: ms-auth, ms-catalog o ms-booking; en reservas el circuito solo cuenta 502 y 504, así que el 503 de ms-booking con `servicio: ms-catalog` llega tal cual); CORS global (también `Idempotency-Key`, y expone `Idempotent-Replayed` y `Location`); `X-Correlation-Id`. El JWT lo validan los servicios ([ADR-005](sad/adr/ADR-005-api-gateway.md)) | Transversal (todos) | RNF01, RNF02, RNF03 | `/api/v1/**` en `:8080` | `frontend/vite.config.ts` (proxy `/api` → 8080) | Implementado |

## Pendientes

| Elemento | Wireframe | RF | Iteración prevista |
|---|---|---|---|
| «¿Olvidaste tu contraseña?» (recuperación por correo, `Token_Recuperacion`). Debe responder de forma uniforme exista o no el correo (ADR-006) | WF-01 | RF34 | Por definir |
| Envío real (SMTP) del aviso «alguien intentó registrarse con tu correo»: hoy `NotificadorCuentaLogAdapter` solo deja un log con el id (ADR-006) | WF-02, WF-03 | RF01, RF02, RF34 | Con RF34 |
| Límite de tasa en el API Gateway para el login y el registro: mitiga el riesgo residual del 423 (ADR-006) | WF-01, WF-02, WF-03 | RF15 | Construcción |
| Sin semilla no existe ningún administrador (RF28); falta definir cómo se crea el primero | — | RF28 | Por definir |
| Contenido de la política de privacidad (requisito derivado D6, Ley 1581). Hoy no hay página: «Política de Privacidad» de la casilla del registro y «Políticas de privacidad» del pie comparten la constante `RUTA_POLITICA_PRIVACIDAD` (`shared/rutasLegales.ts`, sin valor) y se muestran como «Próximamente»; al definirla, los dos pasan a ser enlaces al mismo destino | WF-02, WF-03, Común | RF46, D6 | Por definir |
| Motivo del rechazo de un prestador (RF28): no hay columna; `CUENTA_RECHAZADA` lleva un mensaje genérico. Guardar la justificación es parte del CU de RF28 | WF-01 | RF28 | Con el CU de RF28 |
| Actualizar el modelo lógico del Entregable con V4 (CHECK de categorías), V5 (razón social opcional), V8 (`pais_origen` VARCHAR(2) con CHECK), V9 (`prestador.acepto_politicas`) y V10 (catálogo de roles); `docs/sad/diagramas/modelo-logico.dbml` ya está actualizado | — (SAD) | RF01, RF02 | Construcción (siguiente entrega del SAD) |
| Navegación «Servicios» | Común, WF-05 | RF16 | Iteración actual (vista principal del catálogo, ms-catalog) |
| **Criterio real de «Destacados esta semana».** Hoy es provisional: `tamano=3&orden=PrecioAscendente`. Que salgan solo servicios depende de la decisión 4 (los resultados sin precio van al final), no de un filtro por tipo; el componente, además, descarta lo que no sea servicio. El equipo debe definir el criterio (p. ej. mejor calificación o una marca de destacado asignada por el gestor); cualquiera de los dos requiere datos que hoy no existen | WF-04 | RF16 | Por definir (equipo) |
| Banner de avisos del municipio (no existe el modelo de avisos) | WF-04 | RF40 | Por definir |
| Calificación, afluencia, disponibilidad, «Menor afluencia» y orden por calificación (datos aún no modelados) | WF-04, WF-05 | RF17, RF39, RF51, RF61 | Por definir |
| «Ver detalle» (WF-06) y la reserva (WF-10), que se hará desde el detalle. Los endpoints ya existen (`GET /api/v1/catalogo/servicios/{id}` y ms-booking); faltan las pantallas | WF-04, WF-05, WF-06, WF-10 | RF37, RF20 | Iteración siguiente |
| «Disponibilidad» en las tarjetas: el resumen de ms-booking ya existe (`/api/v1/reservas/disponibilidad/resumen`); falta pedirlo una vez por página y mostrar «Disponible / Agotado / —» (con ms-booking caído, «—») | WF-04, WF-05 | RF11 | Iteración siguiente |
| **Generador de inventario demo (D5), sustituto temporal de RF19.** Cuando el prestador pueda modificar sus cupos (RF19), hay que definir antes: qué pasa si baja los cupos por debajo de lo ya reservado (hoy el CHECK lo impide) y qué pasa con las reservas futuras de una unidad que elimina del catálogo (hoy sus filas quedan huérfanas y no se pueden reservar) | WF-08 | RF19 | Con RF19 |
| RF13, RF30 y RF63 los resuelve ms-notificaciones; ms-booking no publica eventos en esta iteración | — | RF13, RF30, RF63 | Con ms-notificaciones |
| `prestadorActivo` de catalog_db: hoy solo lo mantiene la semilla. Su sincronización desde ms-auth, cuando una cuenta pasa a Inactiva, depende de RF22 y no tiene mecanismo definido | — | RF22 | Por definir |
| Historial de reservas del lado del **prestador** (RF12): consultar las reservas de sus servicios, con los mismos filtros. El índice `(id_prestador, fecha_entrada)` ya existe | — | RF12 | Por definir |
| El esquema `servicios` no define la unidad del precio («cobra por», diferencia 5 de la v2): el prototipo cobra el alojamiento por noche y habitación, y el tour por persona, como dicen las tarjetas de WF-05 | WF-08 | RF37 | Construcción |
| ms-booking se conecta a `reservas_db` con el superusuario de PostgreSQL, como ms-auth (pendiente conocido); rol propio previsto: `booking_app`, con privilegios solo sobre `reservas_db` | — | RNF06 | Por definir |
| Editor de unidades del alojamiento en el registro de servicios: WF-08 pide hoy un solo número de habitaciones y una capacidad; con D1 debe permitir agregar varias unidades (nombre, tipo, habitaciones, capacidad y precio por noche). Rótulo «desde» en las tarjetas cuando el alojamiento tiene varias unidades | WF-08, WF-04, WF-05 | RF09, RF10, RF37 | Por definir (frontend) |
| Texto alternativo y dimensiones por imagen en catalog_db (cambio de $jsonSchema), cuando se implemente el registro de fotos del prestador | WF-04, WF-05 | RF23 | Por definir |
| Navegación «Asistente IA» | Común, WF-07 | RF04 | Por definir (chatbot real fuera de alcance) |
| Pie: columna Municipio (Noticias) | Común | RF40 | Por definir |
| Pie: columna Prestadores (Registrar servicio, Panel del gestor) | Común | RF09, RF64 | Por definir |
| Pie y texto legal: Términos y condiciones, Políticas de privacidad (RF46), Preguntas frecuentes (RF26) y Ayuda / Soporte del pie reducido | Común, WF-01 | RF46, RF26 | Por definir |
| Actualizar la sección NoSQL del SAD (Entregable 2) con el enum de serviciosIncluidos y con el esquema v2 de `servicios` (unidades de alojamiento, `precioBase` como precio «desde» y la regla `$expr`; ver «Pendientes para el SAD», punto 7) | — (SAD) | RF10, RF37 | Construcción (siguiente entrega del SAD) |

**Pendientes resueltos:**
- **Precio «desde» con filtros de unidad (resuelto).** Antes: «con filtros de unidad activos, el precio "desde" de la tarjeta debe ser el mínimo de las unidades que cumplen». Ahora `GET /api/v1/catalogo/oferta` devuelve en `precio` el menor `precioNoche` de las unidades que cumplen los filtros de unidad (y el precio máximo), con las mismas condiciones del filtro (`CondicionesUnidad`), y el orden por precio usa ese valor. Ver la fila de `/oferta` en «Endpoints de ms-catalog».

**Terminología: «afluencia», no «aforo».** Los wireframes dicen «aforo»: «Aforo Bajo / Medio / Alto» en la leyenda de WF-05, «aforo en tiempo real» (WF-04) y «Aforo en vivo» (WF-06, WF-07); el pie de la v1 decía «Aforos en vivo». La fuente es el SRS, que usa «afluencia» (RF39: «indicador visual del nivel de afluencia actual (Bajo, Medio, Alto) de cada atractivo»). Por eso todos los textos visibles dicen «afluencia» (ES) / «crowd level» (EN). En las tarjetas, la etiqueta depende del tipo de oferta: «Afluencia» en los atractivos (RF39) y «Disponibilidad» en los servicios (RF11, «disponibilidad de cupos y horarios»), tanto en el inicio como en /explorar.

## Diferencias con los wireframes (decisiones del equipo)

Elementos de los wireframes v2 que el equipo decidió no implementar.

| Elemento | Wireframe | Decisión | Componente |
|---|---|---|---|
| Botón flotante «Asistente virtual IA» (esquina inferior derecha) | WF-04 v2 (y «Elementos comunes» de la plataforma) | Se retiró por decisión del equipo; el acceso a RF04 queda en la navegación, con «Asistente IA» (deshabilitado hasta que exista el asistente) | `app/DisenoPrincipal.tsx`, `shared/components/Encabezado.tsx` |

## Diferencias con el SRS

Donde un wireframe contradice el SRS, manda el SRS (`docs/wireframes/README.md`, «Reglas»).

| Elemento | Wireframe | SRS | Prototipo |
|---|---|---|---|
| Filtro de precio | WF-05 v2 dibuja «Rango de precio» (mínimo y máximo) | RF16: «precio máximo» | Precio máximo (`precioMax`) |
| Comida vegetariana | WF-05 v2 («Más filtros»): «opción vegetariana» | RF10: «menú vegetariano/vegano» | El esquema `servicios` solo tiene `menuVegetariano` (booleano): el filtro se llama «Opción vegetariana» y no distingue vegano. Distinguirlo exige cambiar el esquema en el SAD |
| Indicador de RF39 | «aforo» (WF-04 a WF-07) | «afluencia» | «Afluencia» (ver «Terminología», arriba) |

## Cambios de los wireframes v2 (12/10/2026)

Elementos que la v2 quitó y que el prototipo ya no muestra (antes estaban deshabilitados o eran pendientes):

| Elemento (v1) | Wireframe | Componente |
|---|---|---|
| Título grande «Samaria360» y bloque «Plataforma» (Explorar destinos, Rutas y experiencias, Prestadores) | WF-01 | `features/auth/LoginPage.tsx` |
| Encabezado y pie completos en el inicio de sesión (ahora reducidos) | WF-01 | `app/App.tsx`, `app/DisenoAcceso.tsx` |
| Icono de búsqueda del encabezado | Común | `shared/components/Encabezado.tsx` |
| Pie: Emergencias, Afluencia en vivo y Contacto (Municipio); «Preguntas frecuentes» pasa de Prestadores a Legal y Soporte | Común | `shared/components/PiePagina.tsx` |
| «Reservar» en los destacados | WF-04 | `componentes/TarjetaOferta.tsx` |
| Tercer nivel de las migas («Mapa y resultados») y «¿Preguntas sobre el estado de las vías…? Preguntar al asistente» | WF-05 | `features/catalogo/ExplorarPage.tsx` |

**Diferencias de la v2 pendientes para Construcción.** `docs/wireframes/README.md` («Diferencias detectadas en la v2») registra estas diferencias, que deben resolverse antes de implementar esas pantallas; no se resolvieron en esta iteración:

| # | Diferencia | Pantallas | Iteración prevista |
|---|---|---|---|
| 1 | WF-10 muestra varias unidades por servicio; el modelo tenía un solo `tipoHabitacion` por servicio y `Inventario_Cupo` por servicio y fecha | WF-10 | **Resuelta a favor del wireframe** (D1 de «Reservar servicio»): catalog_db tiene unidades (M001) y `inventario_cupo` de ms-booking lleva cupos por unidad y noche. Sigue abierta una parte: WF-10 muestra etiquetas de servicios **por unidad**, y en el modelo los servicios incluidos son del alojamiento |
| 2 | Pagos (tarjeta, PSE, Nequi) y «Notificarme si se libera» no tienen requisito en el SRS ni componente en la arquitectura | WF-10 | Construcción |
| 3 | Vocabulario de accesibilidad: RF27 y RF50 definen tres condiciones; WF-08 y WF-09 usan otras | WF-08, WF-09 | Construcción |
| 4 | Vocabulario de servicios incluidos: WF-08 (WiFi, Desayuno, Piscina, Parqueadero) frente a WF-10 (agrega Aire acondicionado y Baño privado). El prototipo usa hoy la lista de WF-08 como vocabulario cerrado («Más filtros» y validador de MongoDB); la lista definitiva sigue por decidir | WF-08, WF-10 | Construcción |
| 5 | WF-08 frente a RF10 y al esquema `servicios`: falta «tipo de habitación» (con D1, falta el editor de unidades; ver «Pendientes»); «cobra por» e «incluye impuestos» no existen en el esquema; el RNT es del prestador | WF-08 | Construcción |

## Registrarse — decisiones (RF01, RF02)

Inconsistencias entre el SRS, los wireframes y el modelo, decididas por el equipo antes de implementar el CU Registrarse. La respuesta al correo duplicado está en [ADR-006](sad/adr/ADR-006-registro-sin-enumeracion.md).

| # | Tema | Decisión | Migración |
|---|---|---|---|
| D1 | Categorías de interés | RF01 dice «tres categorías de interés» y RF53 «actualizar sus tres categorías», pero WF-02 muestra tres casillas con dos marcadas. **Interpretación:** las tres son el **vocabulario cerrado** (Naturaleza, Cultura, Aventura) y el turista elige **al menos una**, sin repetir; repetidas son un error (400 `DUPLICADO`), no se unifican. Se valida con un enum en el dominio (`CategoriaInteres`) y con un CHECK en la base. | V4 |
| D2 | Razón social y correo del prestador | RF02 y WF-03 no piden razón social, que el modelo tenía obligatoria: pasa a ser **opcional** y el formulario queda como WF-03. **Un solo correo** en el registro, con dos significados: `usuario.correo` es la **credencial de acceso** (normalizada, única, la usa el login); `prestador.correo_corporativo` es el **contacto público** (RF43). En el registro (RF02) las dos columnas se llenan con el mismo valor normalizado; cambiar el correo de contacto (RF43) **no** cambia el de acceso. El nombre comercial **no** va en `prestador`: según RF09 es del servicio (ms-catalog). Verificado: `nombre_representante_legal` y `telefono` existen en V1 y son obligatorias. | V5 |
| D3 | Prestador pendiente o rechazado en el login | **Sustituye a la decisión anterior** («un prestador Pendiente puede iniciar sesión; la validación solo afecta el registro de servicios, RF09»), cuya prueba `unPrestadorPendienteDeValidacionPuedeIniciarSesion` se reemplazó a propósito. **Por qué:** RF02 dice que la cuenta queda «Pendiente de Validación hasta que el Administrador verifique sus credenciales», y RF28 habla de cambiar el estado de la cuenta a Activo o Rechazado. Pendiente → 403 `CUENTA_PENDIENTE_VALIDACION`; Rechazado → 403 `CUENTA_RECHAZADA`. Se evalúa **después** de la contraseña: sin ella nadie averigua si un correo es de un prestador pendiente. Contraseña incorrecta: un intento fallido más, como siempre; contraseña correcta con cuenta Pendiente o Rechazada: no es intento fallido, el conteo vuelve a 0 y no se registra un acceso (el acceso solo se registra al emitir el token). `estado_validacion` y `estado_cuenta` (RF22) son estados distintos y no se mezclan: la cuenta Inactiva también se evalúa después de la contraseña (ajuste aprobado), y antes de ella solo queda el bloqueo (423). | V6 (semilla: `prestador@` pasa a validado, porque tiene servicios publicados y según RF09 y RF21 eso exige estar Activo) |
| D4 | Correo ya registrado | Respuesta **uniforme** (202, mismo cuerpo y encabezados), BCrypt siempre, sin inicio de sesión automático; aviso al dueño por `NotificadorCuentaPort`; RNT duplicado → 409 `RNT_YA_REGISTRADO` (dato público). Ver ADR-006. | — |
| D5 | País de origen | RF01 «país de origen» se guarda como **código ISO 3166-1 alfa-2** (p. ej. «CO») en `turista.pais_origen VARCHAR(2)` con CHECK de formato. Una sola fuente: el dominio (`CodigoPais`, con `Locale.getISOCountries` del JDK) y `GET /api/v1/auth/paises`; el frontend solo pone los nombres con `Intl.DisplayNames` y no tiene listas de países. | V7 (semilla: «Colombia» → «CO»), V8 |
| D6 | Aceptación de políticas del prestador | WF-03 tiene la casilla, pero `prestador` no tenía columna. RF02 no lo menciona: es un **requisito derivado** de la restricción normativa de E1 (§3.2.7.4) y de WF-03, sin número de RF. Se agrega `prestador.acepto_politicas` NOT NULL **sin valor por defecto**; las filas de la semilla quedan en false, que es lo cierto. Misma regla y mismo código (`POLITICAS_NO_ACEPTADAS`) para turista y prestador. No interviene en el login. **Fecha de aceptación:** no hay columna propia; la aceptación es obligatoria y se guarda en la misma transacción que crea el usuario, así que `usuario.fecha_registro` registra cuándo se aceptó. Esa columna es DATE: registra el día, no la hora. | V9 |

**Otros hallazgos del CU:**
- **Catálogo de roles (V10).** Solo lo cargaba la semilla V2; sin semilla la tabla `rol` quedaba vacía y el registro fallaría por `fk_usuario_rol`. V10 inserta los cuatro roles con `ON CONFLICT DO NOTHING` y **los mismos id que V2** (1 Turista, 2 Prestador, 3 Gestor, 4 Administrador). El registro busca el rol **por nombre** (`RolJpaRepository.findByNombre`, nombre único), así que no depende del id.
- **«Aventura» no tiene equivalente en el catálogo.** Los atractivos usan Naturaleza y Cultura, y los servicios Alojamiento, Gastronomía, Tour y AlquilerEquipo. Importará cuando el chatbot (RF04) o RF51 usen las preferencias del turista para recomendar.
- **Correo normalizado.** Sin espacios y en minúsculas, igual en el registro y en el login: «Ana@X.com» y «ana@x.com» son la misma cuenta.

## Reservar servicio — decisiones (RF11, RF18, RF20, RF32, RF37, RF44)

Decisiones del equipo para el CU «Reservar servicio» (ms-booking, `reservas_db`; arquitectura en [ADR-007](sad/adr/ADR-007-reservas-y-catalogo.md); contrato en [`sad/api/ms-booking.md`](sad/api/ms-booking.md)).

| # | Tema | Decisión | Cambio |
|---|---|---|---|
| D1 | Unidades de un alojamiento | **Un alojamiento tiene una o más unidades** (tipos de habitación), cada una con `idUnidad` (ObjectId, estable aunque se renombre), `nombre`, `tipoHabitacion`, `numHabitaciones`, `capacidadMaxHuespedes` y `precioNoche`. Los servicios incluidos siguen siendo del alojamiento. **Una reserva es de una sola unidad**: para dos tipos de habitación, dos reservas. **Precio «desde» (RF37):** `precioBase` de un alojamiento es el menor `precioNoche` de sus unidades; se calcula al guardar (el dominio lo deriva y el validador lo exige con `$expr`), así que la búsqueda, el orden por precio y las tarjetas no cambian. **Filtros:** huéspedes, tipo de habitación y precio máximo los debe cumplir una misma unidad (`$elemMatch`), porque se reserva una sola. **Motivo del cambio:** la primera propuesta era una unidad por servicio (manda RF10), pero obligaba a simplificar WF-10 y a partir un hotel real en varios servicios; el equipo decidió seguir el wireframe. | Esquema v2 de `servicios` (migración M001); semilla de la «Suite familiar» (M002) |
| D2 | Categorías reservables | **Tour y Alojamiento.** Alojamiento: rango semiabierto [entrada, salida), máximo 14 noches, `cantidad` = habitaciones de la unidad, personas ≤ habitaciones × capacidad de la unidad. Tour: un horario, personas ≤ `aforoMaxGrupo`. Gastronomía y alquiler de equipos: `CATEGORIA_NO_RESERVABLE`. | `inventario_cupo` con clave (servicio, unidad, fecha, hora) `UNIQUE NULLS NOT DISTINCT` y CHECK de forma por categoría |
| D3 | Concurrencia | **UPDATE condicional + CHECK**, un día por sentencia, en orden de fecha y hora, en una transacción. Antes se comprueba que existan todas las filas (`FECHA_NO_DISPONIBLE`); si alguna no alcanza, `CUPO_AGOTADO` y no se descuenta nada. La cancelación también es un UPDATE condicional (solo si sigue Confirmada): dos cancelaciones a la vez liberan una sola vez. | V1 (`ck_inventario_cupo_disponibles`) |
| D4 | Datos del catálogo | Llamada síncrona directa al detalle público de ms-catalog, con circuit breaker, **antes** de abrir la transacción; el precio lo calcula el servidor (ADR-007). | — |
| D5 | Inventario | **Generador de inventario demo** (`samaria360.inventario-demo.habilitado`, activo en Docker Compose): 60 días, tours a las 08:00 y 14:00, una fila por unidad y noche; solo `ON CONFLICT DO NOTHING`. Sustituto temporal de RF19 (ver «Pendientes»). | — |
| D6 | Idempotencia | `Idempotency-Key` obligatoria (UUID), única por turista, con el hash SHA-256 del pedido normalizado. El reenvío responde **201** con la misma reserva e `Idempotent-Replayed: true`; la misma clave con otro pedido, 422. Solo se guardan los intentos confirmados. Si dos pedidos con la misma clave compiten, el segundo lee la reserva ganadora (nunca un 500). | V1 (`uq_reserva_turista_clave`) |
| D7 | Mensajería | **Ninguna en esta iteración:** ms-booking no publica eventos (ver «Pendientes»). | — |
| D8 | Estados y errores | Confirmada, Cancelada y Completada (sin «Pendiente»: RF20 confirma automáticamente). Códigos en `sad/api/ms-booking.md`; `OFERTA_NO_DISPONIBLE` es 409 al reservar (404 en el detalle del catálogo) y `UNIDAD_NO_DISPONIBLE`, 409. | V1 (`ck_reserva_estado`) |
| D9 | Fechas, horas y precio | Hora de Santa Marta (`America/Bogota`). Check-in 15:00 y check-out 12:00 (configurables). **RF44:** se puede cancelar si faltan 24 h o más (límite incluido); en alojamiento, la fecha programada es el check-in. Precio: alojamiento por noche y habitación de la unidad; tour por persona; COP. La reserva guarda `nombre_servicio`, `nombre_unidad`, `id_unidad` e `id_prestador` (RF32; consultar nunca llama al catálogo); su id es un uuid generado en el dominio. | V1 |
| D10 | Alcance | Solo backend, con el contrato (`sad/api/ms-booking.md`) y una demo reproducible (`docs/demo/reservas/`). WF-10 llega en la iteración siguiente. | — |

**Solicitud de cambio al SRS.** «RF10 (Alojamiento) debe admitir varias unidades, cada una con tipo, cantidad, capacidad y precio.» Hoy RF10 describe un solo tipo de habitación, número de habitaciones y capacidad por alojamiento.

**Fuente de verdad de la capacidad.**

| Dato | Base | Dueño | Significado |
|---|---|---|---|
| `unidades[].numHabitaciones`, `unidades[].capacidadMaxHuespedes` y `aforoMaxGrupo` | catalog_db | ms-catalog (RF10) | Capacidad configurada y límite por reserva |
| `unidades[].precioNoche` y `precioBase` (tour) | catalog_db | ms-catalog | Precio vigente; la reserva lo congela (RF32) |
| `inventario_cupo.cupos_totales` | reservas_db | ms-booking | Capacidad operativa de cada noche y unidad, o de cada horario; la crea el generador y la modificará RF19 |
| `inventario_cupo.cupos_disponibles` | reservas_db | ms-booking | Lo que queda: solo cambia al reservar o cancelar |

**Fuera de alcance de este CU:** RF12 del lado del prestador (el del turista ya está: «Mis reservas»), RF19 (el prestador modifica los cupos), RF13, RF30 y RF63 (auditoría y notificaciones). Los pagos y «Notificarme si se libera» de WF-10 no tienen requisito (diferencia 2 de la v2).

**Erratas del SRS** (referencias cruzadas a otro requisito):

| Requisito | Cita | Debería citar |
|---|---|---|
| RF21 | «RF08» | RF09 |
| RF29 | «RF16» | RF17 |
| RF29 | «RF12» | RF13 |
| RF30 | «RF19» | RF20 |
| RF44 | «RF17» | RF18 |
| RF52 | «RF16» | RF17 |
| RF61 | «RF15» | RF16 |

**Migraciones de catalog_db.** Hasta ahora catalog_db solo tenía los scripts de `infra/mongo/init`, que corren con el volumen vacío. Desde este cambio, esos scripts son la **línea base congelada** (formato v1) y el servicio de un solo uso `mongo-migraciones` aplica `infra/mongo/migraciones` (registradas en la colección `migraciones`), igual en un volumen nuevo que en uno existente. Cada migración lleva su propia copia del validador y es idempotente. M001 se detiene sin tocar nada si encuentra un alojamiento sin `precioBase` (no inventa precios).

## Buscar oferta turística — decisiones de búsqueda (RF16, RF23, RF33, RF38, RF50, RF61)

Decididas por el equipo; se aplican en el caso de uso de ms-catalog (`domain/service/PoliticaBusqueda` y el adaptador MongoDB; arquitectura en [`ADR-004`](sad/adr/ADR-004-busqueda-combinada.md)).

| # | Decisión | RF |
|---|---|---|
| 1 | Los atractivos aparecen en los resultados junto con los servicios, como en WF-05. | RF16, RF23 |
| 2 | El filtro de categoría aplica solo a servicios: si se elige una categoría, los atractivos quedan fuera. | RF16 |
| 3 | El filtro de precio máximo excluye los atractivos, porque no tienen precio. | RF16, RF37 |
| 4 | Al ordenar por precio, los resultados sin precio van al final. | RF61 |
| 5 | Los atractivos cerrados temporalmente o en mantenimiento sí aparecen, con su estado visible, para que el turista lo sepa y busque una alternativa. | RF33, RF08 |
| 6 | El filtro de accesibilidad aplica a servicios y atractivos. | RF50 |

## Pendientes para el SAD (modelo NoSQL)

Observaciones sobre `docs/sad/modelo-nosql/`. Los cambios al esquema son el `enum` de `serviciosIncluidos` (punto 6) y las unidades de alojamiento (punto 7).

| # | Observación | RF |
|---|---|---|
| 1 | `precioBase` es opcional en `servicios`, aunque RF37 exige precio en todo servicio. La semilla lo incluye siempre. Desde el esquema v2 es obligatorio en Alojamiento (es el precio «desde»); en las demás categorías sigue opcional. | RF37, RF16 |
| 2 | `atractivos` no tiene precio, `estadoPublicacion` ni `prestadorActivo`, y su `categoria` es texto libre (en `servicios` es un enum). Resuelto para la búsqueda con las decisiones 1–3 y 5; falta reflejarlo en el SAD. | RF16, RF06 |
| 3 | `condicionesAccesibilidad` acepta cualquier texto; no restringe a MovilidadReducida, Visual y Auditiva. El dominio sí lo restringe. | RF27, RF50 |
| 4 | No existe el dato de calificación promedio, así que el orden por calificación queda pendiente (ver «Pendientes»). | RF61 |
| 5 | La búsqueda por texto no distingue mayúsculas pero sí tildes: «cafe» no encuentra «Tour de Café», aunque es lo que un turista escribe a menudo. Solución prevista: campos normalizados sin tildes para buscar (p. ej. `nombreBusqueda` y `descripcionBusqueda`), mantenidos al registrar o editar; requiere cambiar el esquema en el SAD. Los filtros de texto de «Más filtros» siguen la misma regla, pero sus opciones salen de los datos, así que llegan con las tildes exactas. | RF16 |
| 6 | `detalleCategoria.serviciosIncluidos` (Alojamiento) pasó de texto libre a un `enum` (WiFi, Desayuno, Piscina, Parqueadero) en `infra/mongo/init/01-validadores.js` y `docs/sad/modelo-nosql/servicios.schema.json`, para «Más filtros». Falta llevarlo al Documento Entregable 2 (ver «Pendientes»). Un volumen de MongoDB creado antes recibe el validador actual con la migración M001, sin borrar datos. | RF10 |
| 7 | **Esquema v2 de `servicios` (D1 de «Reservar servicio»).** La rama Alojamiento cambia `tipoHabitacion`, `numHabitaciones` y `capacidadMaxHuespedes` por `unidades` (al menos una; `idUnidad` ObjectId; números ≥ 1; `precioNoche` > 0) y exige `precioBase`. Una condición `$expr` junto al `$jsonSchema` exige que `idUnidad` no se repita y que `precioBase` sea el menor `precioNoche`. `docs/sad/modelo-nosql/servicios.schema.json` describe la versión v2; la migración M001 lleva su propia copia. Falta llevarlo al Documento Entregable 2. | RF10, RF37 |

## Discrepancias

Las discrepancias entre wireframes, SRS y modelo de datos se registran en la sección **«Diferencias detectadas en la v2»** de [`docs/wireframes/README.md`](wireframes/README.md); aquí solo su estado (ver «Cambios de los wireframes v2») y las diferencias con el SRS que el prototipo resolvió a favor del SRS.

Resuelta en la v2: el pie dibujado en la imagen v1 de WF-01 («© 2026 Samaria360 · Plataforma turística verificada») difería del descrito en el README. La v2 lo adopta como pie reducido de las pantallas de acceso, y el prototipo lo implementa.
