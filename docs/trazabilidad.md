# Matriz de trazabilidad del prototipo arquitectónico

Fase de Elaboración (RUP). Relaciona cada elemento de los wireframes (`docs/wireframes/`) con su caso de uso, sus requisitos (`docs/srs/`), el endpoint que lo atiende y el componente que lo implementa. Cada caso de uso nuevo agrega sus filas a esta matriz.

**Estados:** **Implementado** (funciona de punta a punta) · **Deshabilitado** (visible en pantalla, sin funcionalidad, con la marca «Próximamente») · **Pendiente** (aún no aparece en el prototipo).

Rutas de componentes relativas a `frontend/src/`; endpoints de ms-auth bajo `/api/v1/auth`.

## Matriz principal

### WF-01 — Inicio de sesión

| Wireframe | Elemento | Caso de uso | RF/RNF | Endpoint | Componente frontend | Estado |
|---|---|---|---|---|---|---|
| WF-01 | Título «Samaria360» y subtítulo | Iniciar sesión | RF15 | — | `features/auth/LoginPage.tsx` | Implementado |
| WF-01 | Pestaña «Iniciar sesión» | Iniciar sesión | RF15 | — | `features/auth/LoginPage.tsx` | Implementado |
| WF-01 | Pestaña «Registrarse» | Registrarse | RF01, RF02 | — | `features/auth/LoginPage.tsx` | Deshabilitado |
| WF-01 | Texto introductorio | Iniciar sesión | RF15 | — | `features/auth/LoginPage.tsx` | Implementado |
| WF-01 | Campo «Correo electrónico» | Iniciar sesión | RF15 | `POST /login` | `features/auth/LoginPage.tsx` | Implementado |
| WF-01 | Campo «Contraseña» | Iniciar sesión | RF15, RNF05 | `POST /login` | `features/auth/LoginPage.tsx` | Implementado |
| WF-01 | Enlace «¿Olvidaste tu contraseña?» | Recuperar contraseña | RF34 | — | `features/auth/LoginPage.tsx` | Deshabilitado |
| WF-01 | Recuadro «3 intentos fallidos bloquearán la cuenta por 15 minutos» | Iniciar sesión | RF15 | — | `features/auth/LoginPage.tsx` | Implementado |
| WF-01 | Botón «Iniciar sesión» | Iniciar sesión | RF15 | `POST /login`, `GET /me` | `features/auth/LoginPage.tsx`, `features/auth/SesionProvider.tsx` | Implementado |
| WF-01 | Mensaje de credenciales inválidas (401) | Iniciar sesión | RF15 | `POST /login` | `features/auth/mensajesError.ts` | Implementado |
| WF-01 | Mensaje de cuenta bloqueada con hora de desbloqueo (423) | Iniciar sesión | RF15 | `POST /login` | `features/auth/mensajesError.ts` | Implementado |
| WF-01 | Mensaje de cuenta inactiva (403) | Iniciar sesión | RF22, RF62 | `POST /login` | `features/auth/mensajesError.ts` | Implementado |
| WF-01 | Mensaje de servicio no disponible (502, 503 o 504, o `SERVICIO_NO_DISPONIBLE`): «El servicio no está disponible por el momento…»; un 500 muestra el mensaje genérico | Iniciar sesión | RF15, RNF01 | `POST /login` | `features/auth/mensajesError.ts`, `shared/api/httpClient.ts` | Implementado |
| WF-01 | Texto legal «Al iniciar sesión aceptas los Términos…» | Iniciar sesión | RF46 | — | `features/auth/LoginPage.tsx` | Implementado (texto; la página legal está pendiente) |
| WF-01 | Bloque «Plataforma»: Explorar destinos, Rutas y experiencias, Prestadores | Buscar oferta turística | RF16 | — | `features/auth/LoginPage.tsx` | Deshabilitado |
| WF-01 → WF-04 | Redirección tras el login a la pantalla de inicio WF-04 (o a la ruta privada pedida) | Iniciar sesión | RF15 | `GET /me` | `features/auth/LoginPage.tsx`, `features/catalogo/InicioPage.tsx` | Implementado |

### WF-04 — Inicio y búsqueda

| Wireframe | Elemento | Caso de uso | RF/RNF | Endpoint | Componente frontend | Estado |
|---|---|---|---|---|---|---|
| WF-04 | Titular y texto de apoyo | Buscar oferta turística | RF16 | — | `features/catalogo/InicioPage.tsx` | Implementado |
| WF-04 | Barra de búsqueda «¿Qué buscas?» → `/explorar?texto=…` | Buscar oferta turística | RF16 | — | `features/catalogo/InicioPage.tsx` | Implementado |
| WF-04 | «Explora por categoría»: Alojamiento, Gastronomía, Tours, Alquiler de equipos → `/explorar?categoria=…` | Buscar oferta turística | RF16 | — | `features/catalogo/InicioPage.tsx` | Implementado |
| WF-04 | «Destacados esta semana»: 3 servicios (imagen, nombre, categoría, precio «desde») | Buscar oferta turística | RF16, RF37 | `GET /oferta?tamano=3&orden=PrecioAscendente` | `features/catalogo/InicioPage.tsx`, `componentes/TarjetaOferta.tsx` | Implementado (criterio provisional: ver Pendientes) |
| WF-04 | Calificación y disponibilidad en los destacados | Buscar oferta turística | RF17, RF11 | — | `componentes/TarjetaOferta.tsx` | Deshabilitado («—», sin datos) |
| WF-04 | «Ver detalle» y «Reservar» | Consultar detalle / Reservar | RF37, RF20 | — | `componentes/TarjetaOferta.tsx` | Deshabilitado |
| WF-04 | Banner de aviso del municipio (cerrable) | Publicar avisos | RF40 | — | — | Pendiente (no existe el modelo de avisos) |
| WF-04, WF-05 | Foto real de cada lugar (21, WebP 3:2, como máximo 1200×800, sin metadatos), decorativa (`alt=""`: el nombre está en el `h3`) | Buscar oferta turística | RF23, RF37 | `GET /oferta` (`imagen`) | `componentes/ImagenOferta.tsx`, `public/imagenes/catalogo/`, `scripts/optimizar-imagenes.mjs` | Implementado (sin créditos verificados; ver `docs/recursos-graficos/README.md`) |
| WF-04, WF-05 | Ilustración propia por categoría cuando falta la imagen del elemento | Buscar oferta turística | RF23 | — | `componentes/ImagenOferta.tsx`, `public/imagenes/catalogo/categorias/` | Implementado |

### WF-05 — Exploración: mapa y resultados

| Wireframe | Elemento | Caso de uso | RF/RNF | Endpoint | Componente frontend | Estado |
|---|---|---|---|---|---|---|
| WF-05 | Migas de pan y título | Buscar oferta turística | RF16 | — | `features/catalogo/ExplorarPage.tsx` | Implementado |
| WF-05 | Filtro de texto «Filtrar por nombre, vereda o actividad» | Buscar oferta turística | RF16 | `GET /oferta?texto=` | `componentes/FiltrosOferta.tsx` | Implementado |
| WF-05 | Precio máximo (deslizador) | Buscar oferta turística | RF16, RF37 | `GET /oferta?precioMax=` | `componentes/FiltrosOferta.tsx` | Implementado |
| WF-05 | Distancia máxima (deslizador); sin ubicación, deshabilitado con explicación | Buscar oferta turística | RF38 | `GET /oferta?lat=&lng=&distanciaKm=` | `componentes/FiltrosOferta.tsx` | Implementado |
| WF-05 | Accesibilidad: WF-05 muestra solo «movilidad reducida»; se muestran las 3 condiciones de RF50 | Buscar oferta turística | RF50, RF27 | `GET /oferta?accesibilidad=` | `componentes/FiltrosOferta.tsx` | Mejora de diseño |
| WF-05 | Chip removible de la categoría elegida en WF-04 | Buscar oferta turística | RF16 | `GET /oferta?categoria=` | `componentes/FiltrosOferta.tsx` | Mejora de diseño |
| WF-05 | Botón «Aplicar filtros» | Buscar oferta turística | RF16 | — | `componentes/FiltrosOferta.tsx` | Implementado |
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
| WF-05 | «Preguntar al asistente» | Interactuar con asistente | RF04 | — | `features/catalogo/ExplorarPage.tsx` | Deshabilitado |

### Elementos comunes a todas las pantallas

| Wireframe | Elemento | Caso de uso | RF/RNF | Endpoint | Componente frontend | Estado |
|---|---|---|---|---|---|---|
| Común | Logo «Samaria360» | — | — | — | `shared/components/Encabezado.tsx` | Implementado |
| Común | Navegación «Inicio de sesión» | Iniciar sesión | RF15 | — | `shared/components/Encabezado.tsx` | Implementado |
| Común | Navegación «Búsqueda» (lleva a `/explorar`) | Buscar oferta turística | RF16 | `GET /api/v1/catalogo/oferta` | `shared/components/Encabezado.tsx` | Implementado |
| Común | Navegación «Servicios» | Buscar oferta turística | RF16 | — | `shared/components/Encabezado.tsx` | Deshabilitado |
| Común | Navegación «Asistente IA» | Interactuar con asistente | RF04 | — | `shared/components/Encabezado.tsx` | Deshabilitado |
| Común | Selector de idioma ES / EN | Cambiar idioma | RF03, RNF08 | — | `shared/components/SelectorIdioma.tsx`, `i18n/` | Implementado |
| Común | Icono de búsqueda | Buscar oferta turística | RF16 | — | `shared/components/Encabezado.tsx` | Deshabilitado |
| Común | Avatar con iniciales de `nombreVisible` | Iniciar sesión | RF15, RNF06 | `GET /me` | `shared/components/MenuUsuario.tsx` | Implementado |
| Común | Menú del avatar: «Cerrar sesión» | Cerrar sesión | RF55 | — (el token se descarta en el cliente) | `shared/components/MenuUsuario.tsx`, `features/auth/SesionProvider.tsx` | Implementado |
| Común | Restauración de la sesión al cargar la app; cierre ante 401/403 | Iniciar sesión | RF15, RF22, RF62 | `GET /me` | `features/auth/SesionProvider.tsx`, `shared/api/httpClient.ts` | Implementado |
| Común | Rutas protegidas (sin sesión → login y regreso a la ruta pedida). Hoy no hay rutas privadas: inicio y exploración son públicas | Iniciar sesión | RNF06 | — | `app/RutaProtegida.tsx` | Implementado (sin uso hasta las pantallas de prestador y gestor) |
| Común | Aviso de sesión vencida en páginas públicas: «Tu sesión expiró» (401) o «Tu cuenta no está activa» (403), con enlace al login; el usuario se queda en la página | Iniciar sesión | RF15, RF22, RF62 | `GET /me` | `features/auth/AvisoSesion.tsx` | Mejora de diseño |
| Común | Pie: columnas Explorar, Municipio, Prestadores y Legal | Varios | RF16, RF26, RF46 | — | `shared/components/PiePagina.tsx` | Deshabilitado |
| Común | Pie: «© 2026 Alcaldía de Santa Marta · Samaria360» | — | — | — | `shared/components/PiePagina.tsx` | Implementado |
| Común | Pie: aviso «Prototipo académico — Universidad del Magdalena. Las imágenes son de referencia y pueden pertenecer a terceros; no se usan con fines comerciales.» (ES/EN), en lugar de una página de créditos | — | — | — | `shared/components/PiePagina.tsx` | Mejora de diseño |
| Común | Botón flotante «Asistente virtual IA» | Interactuar con asistente | RF04 | — | `shared/components/BotonAsistente.tsx` | Deshabilitado |

### Endpoints de ms-auth

| Wireframe | Elemento | Caso de uso | RF/RNF | Endpoint | Componente frontend | Estado |
|---|---|---|---|---|---|---|
| WF-01 | Inicio de sesión con correo y contraseña; bloqueo de 15 min tras 3 fallos; rechazo de cuentas no activas | Iniciar sesión | RF15, RF22, RF62, RNF05 | `POST /api/v1/auth/login` | `features/auth/authApi.ts` | Implementado |
| Común | Datos del usuario autenticado (`nombreVisible` para el avatar); 403 si la cuenta dejó de estar activa | Iniciar sesión | RF15, RF22, RF62, RNF06 | `GET /api/v1/auth/me` | `features/auth/authApi.ts` | Implementado |
| — | Clave pública para validar el JWT RS256 en el API Gateway | Iniciar sesión | RNF06 | `GET /api/v1/auth/.well-known/jwks.json` | — (lo consume el API Gateway) | Implementado |

### Endpoints de ms-catalog

| Wireframe | Elemento | Caso de uso | RF/RNF | Endpoint | Componente frontend | Estado |
|---|---|---|---|---|---|---|
| WF-04, WF-05 | Búsqueda pública combinada de servicios y atractivos: texto, categoría, precio máximo, ubicación y distancia, accesibilidad, orden (distancia, precio, nombre) y paginación de 10 | Buscar oferta turística | RF16, RF23, RF38, RF50, RF61, RNF02 | `GET /api/v1/catalogo/oferta` | `features/catalogo/` (WF-04 y WF-05) | Implementado |

### API Gateway

| Wireframe | Elemento | Caso de uso | RF/RNF | Endpoint | Componente frontend | Estado |
|---|---|---|---|---|---|---|
| Todas | Único punto de entrada (`:8080`): enrutamiento de `/api/v1/auth/**` y `/api/v1/catalogo/**`; timeouts y circuit breaker por ruta con fallback 503 `SERVICIO_NO_DISPONIBLE`; CORS global; `X-Correlation-Id`. El JWT lo validan los servicios ([ADR-005](sad/adr/ADR-005-api-gateway.md)) | Transversal (todos) | RNF01, RNF02, RNF03 | `/api/v1/**` en `:8080` | `frontend/vite.config.ts` (proxy `/api` → 8080) | Implementado |

## Pendientes

| Elemento | Wireframe | RF | Iteración prevista |
|---|---|---|---|
| Pestaña «Registrarse» (registro de turista y de prestador) | WF-01, WF-02, WF-03 | RF01, RF02 | Por definir (fuera del alcance de esta iteración) |
| «¿Olvidaste tu contraseña?» (recuperación por correo, `Token_Recuperacion`) | WF-01 | RF34 | Por definir |
| Icono de búsqueda del encabezado (la navegación «Búsqueda» ya lleva a `/explorar`) | Común | RF16 | Por definir |
| Navegación «Servicios» | Común, WF-05 | RF16 | Iteración actual (vista principal del catálogo, ms-catalog) |
| Bloque «Plataforma» de WF-01 (Explorar destinos, Rutas y experiencias, Prestadores) | WF-01 | RF16 | Iteración actual (vista principal del catálogo); destino exacto por definir |
| **Criterio real de «Destacados esta semana».** Hoy es provisional: `tamano=3&orden=PrecioAscendente`. Que salgan solo servicios depende de la decisión 4 (los resultados sin precio van al final), no de un filtro por tipo; el componente, además, descarta lo que no sea servicio. El equipo debe definir el criterio (p. ej. mejor calificación o una marca de destacado asignada por el gestor); cualquiera de los dos requiere datos que hoy no existen | WF-04 | RF16 | Por definir (equipo) |
| Banner de avisos del municipio (no existe el modelo de avisos) | WF-04 | RF40 | Por definir |
| Calificación, afluencia, disponibilidad, «Menor afluencia» y orden por calificación (datos aún no modelados) | WF-04, WF-05 | RF17, RF39, RF51, RF61 | Por definir |
| «Ver detalle» (WF-06) y «Reservar» | WF-04, WF-05 | RF37, RF20 | Iteración siguiente (detalle) / Por definir (reservas) |
| Texto alternativo y dimensiones por imagen en catalog_db (cambio de $jsonSchema), cuando se implemente el registro de fotos del prestador | WF-04, WF-05 | RF23 | Por definir |
| Navegación «Asistente IA» y botón flotante | Común, WF-07 | RF04 | Por definir (chatbot real fuera de alcance) |
| Pie: columna Explorar (Alojamiento, Gastronomía, Tours, Alquiler de equipos) | Común | RF16 | Iteración actual (filtros por categoría del catálogo) |
| Pie: columna Municipio (Noticias, Emergencias, Afluencia en vivo —en el wireframe, «Aforos en vivo»—, Contacto) | Común | RF40, RF39 | Por definir |
| Pie: columna Prestadores (Registrar servicio, Panel del gestor, Preguntas frecuentes) | Común | RF09, RF64, RF26 | Por definir |
| Pie y texto legal: página de Términos, Condiciones y Políticas de Privacidad | Común, WF-01 | RF46 | Por definir |

**Terminología: «afluencia», no «aforo».** Los wireframes dicen «nivel de aforo» (WF-05), «aforo en tiempo real» (WF-04) y «Aforos en vivo» (pie). La fuente es el SRS, que usa «afluencia» (RF39: «indicador visual del nivel de afluencia actual (Bajo, Medio, Alto) de cada atractivo»). Por eso todos los textos visibles dicen «afluencia» (ES) / «crowd level» (EN). En las tarjetas, la etiqueta depende del tipo de oferta: «Afluencia» en los atractivos (RF39) y «Disponibilidad» en los servicios (RF11, «disponibilidad de cupos y horarios»), tanto en el inicio como en /explorar.

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

Observaciones sobre `docs/sad/modelo-nosql/`; el esquema no se modificó.

| # | Observación | RF |
|---|---|---|
| 1 | `precioBase` es opcional en `servicios`, aunque RF37 exige precio en todo servicio. La semilla lo incluye siempre. | RF37, RF16 |
| 2 | `atractivos` no tiene precio, `estadoPublicacion` ni `prestadorActivo`, y su `categoria` es texto libre (en `servicios` es un enum). Resuelto para la búsqueda con las decisiones 1–3 y 5; falta reflejarlo en el SAD. | RF16, RF06 |
| 3 | `condicionesAccesibilidad` acepta cualquier texto; no restringe a MovilidadReducida, Visual y Auditiva. El dominio sí lo restringe. | RF27, RF50 |
| 4 | No existe el dato de calificación promedio, así que el orden por calificación queda pendiente (ver «Discrepancias detectadas», punto 4, de `docs/wireframes/README.md`). | RF61 |
| 5 | La búsqueda por texto no distingue mayúsculas pero sí tildes: «cafe» no encuentra «Tour de Café», aunque es lo que un turista escribe a menudo. Solución prevista: campos normalizados sin tildes para buscar (p. ej. `nombreBusqueda` y `descripcionBusqueda`), mantenidos al registrar o editar; requiere cambiar el esquema en el SAD. | RF16 |

## Discrepancias

Las discrepancias entre wireframes, SRS y modelo de datos se registran en la sección **«Discrepancias detectadas con el SRS y el modelo»** de [`docs/wireframes/README.md`](wireframes/README.md); no se duplican aquí.

Observación adicional, aún no registrada allí: el pie de página dibujado en la imagen `WF-01-inicio-sesion.png` (columnas Plataforma, Anfitriones y Legal; «© 2026 Samaria360 · Plataforma turística verificada») difiere del descrito en «Elementos comunes a todas las pantallas» del mismo README y del que aparece en WF-04. El prototipo sigue la descripción del README.
