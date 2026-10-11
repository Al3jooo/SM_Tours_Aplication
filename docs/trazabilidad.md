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
| WF-01 | Texto legal «Al iniciar sesión aceptas los Términos…» | Iniciar sesión | RF46 | — | `features/auth/LoginPage.tsx` | Implementado (texto; la página legal está pendiente) |
| WF-01 | Bloque «Plataforma»: Explorar destinos, Rutas y experiencias, Prestadores | Buscar oferta turística | RF16 | — | `features/auth/LoginPage.tsx` | Deshabilitado |
| WF-01 → WF-04 | Redirección tras el login (inicio provisional con saludo) | Iniciar sesión | RF15 | `GET /me` | `app/InicioProvisionalPage.tsx` | Implementado (provisional hasta WF-04) |

### Elementos comunes a todas las pantallas

| Wireframe | Elemento | Caso de uso | RF/RNF | Endpoint | Componente frontend | Estado |
|---|---|---|---|---|---|---|
| Común | Logo «Samaria360» | — | — | — | `shared/components/Encabezado.tsx` | Implementado |
| Común | Navegación «Inicio de sesión» | Iniciar sesión | RF15 | — | `shared/components/Encabezado.tsx` | Implementado |
| Común | Navegación «Búsqueda» | Buscar oferta turística | RF16 | — | `shared/components/Encabezado.tsx` | Deshabilitado |
| Común | Navegación «Servicios» | Buscar oferta turística | RF16 | — | `shared/components/Encabezado.tsx` | Deshabilitado |
| Común | Navegación «Asistente IA» | Interactuar con asistente | RF04 | — | `shared/components/Encabezado.tsx` | Deshabilitado |
| Común | Selector de idioma ES / EN | Cambiar idioma | RF03, RNF08 | — | `shared/components/SelectorIdioma.tsx`, `i18n/` | Implementado |
| Común | Icono de búsqueda | Buscar oferta turística | RF16 | — | `shared/components/Encabezado.tsx` | Deshabilitado |
| Común | Avatar con iniciales de `nombreVisible` | Iniciar sesión | RF15, RNF06 | `GET /me` | `shared/components/MenuUsuario.tsx` | Implementado |
| Común | Menú del avatar: «Cerrar sesión» | Cerrar sesión | RF55 | — (el token se descarta en el cliente) | `shared/components/MenuUsuario.tsx`, `features/auth/SesionProvider.tsx` | Implementado |
| Común | Restauración de la sesión al cargar la app; cierre ante 401/403 | Iniciar sesión | RF15, RF22, RF62 | `GET /me` | `features/auth/SesionProvider.tsx`, `shared/api/httpClient.ts` | Implementado |
| Común | Rutas protegidas (sin sesión → login) | Iniciar sesión | RNF06 | — | `app/RutaProtegida.tsx` | Implementado |
| Común | Pie: columnas Explorar, Municipio, Prestadores y Legal | Varios | RF16, RF26, RF46 | — | `shared/components/PiePagina.tsx` | Deshabilitado |
| Común | Pie: «© 2026 Alcaldía de Santa Marta · Samaria360» | — | — | — | `shared/components/PiePagina.tsx` | Implementado |
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
| WF-04, WF-05 | Búsqueda pública combinada de servicios y atractivos: texto, categoría, precio máximo, ubicación y distancia, accesibilidad, orden (distancia, precio, nombre) y paginación de 10 | Buscar oferta turística | RF16, RF23, RF38, RF50, RF61, RNF02 | `GET /api/v1/catalogo/oferta` | Pendiente (parte de frontend del caso de uso) | Implementado (backend) |

### API Gateway

| Wireframe | Elemento | Caso de uso | RF/RNF | Endpoint | Componente frontend | Estado |
|---|---|---|---|---|---|---|
| Todas | Único punto de entrada (`:8080`): enrutamiento de `/api/v1/auth/**` y `/api/v1/catalogo/**`; timeouts y circuit breaker por ruta con fallback 503 `SERVICIO_NO_DISPONIBLE`; CORS global; `X-Correlation-Id`. El JWT lo validan los servicios ([ADR-005](sad/adr/ADR-005-api-gateway.md)) | Transversal (todos) | RNF01, RNF02, RNF03 | `/api/v1/**` en `:8080` | `frontend/vite.config.ts` (proxy `/api` → 8080) | Implementado |

## Pendientes

| Elemento | Wireframe | RF | Iteración prevista |
|---|---|---|---|
| Pestaña «Registrarse» (registro de turista y de prestador) | WF-01, WF-02, WF-03 | RF01, RF02 | Por definir (fuera del alcance de esta iteración) |
| «¿Olvidaste tu contraseña?» (recuperación por correo, `Token_Recuperacion`) | WF-01 | RF34 | Por definir |
| Navegación «Búsqueda» e icono de búsqueda | Común, WF-04, WF-05 | RF16 | Iteración actual (vista principal del catálogo, ms-catalog) |
| Navegación «Servicios» | Común, WF-05 | RF16 | Iteración actual (vista principal del catálogo, ms-catalog) |
| Bloque «Plataforma» de WF-01 (Explorar destinos, Rutas y experiencias, Prestadores) | WF-01 | RF16 | Iteración actual (vista principal del catálogo); destino exacto por definir |
| Pantalla de inicio real tras el login (reemplaza el inicio provisional) | WF-04 | RF16, RF40, RF04 | Iteración actual (vista principal del catálogo) |
| Navegación «Asistente IA» y botón flotante | Común, WF-07 | RF04 | Por definir (chatbot real fuera de alcance) |
| Pie: columna Explorar (Alojamiento, Gastronomía, Tours, Alquiler de equipos) | Común | RF16 | Iteración actual (filtros por categoría del catálogo) |
| Pie: columna Municipio (Noticias, Emergencias, Aforos en vivo, Contacto) | Común | RF40, RF39 | Por definir |
| Pie: columna Prestadores (Registrar servicio, Panel del gestor, Preguntas frecuentes) | Común | RF09, RF64, RF26 | Por definir |
| Pie y texto legal: página de Términos, Condiciones y Políticas de Privacidad | Común, WF-01 | RF46 | Por definir |

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
