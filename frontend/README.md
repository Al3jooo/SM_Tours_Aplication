# Samaria360 — Frontend

React + Vite + TypeScript. Prototipo arquitectónico de la fase de Elaboración (RUP): caso de uso **Iniciar sesión** (RF15, RF03, RF55, RNF08). Las pantallas siguen la estructura de los wireframes de `docs/wireframes/`; la trazabilidad está en `docs/trazabilidad.md`.

## Comandos

```bash
npm install
npm run dev      # http://localhost:5173 — /api se redirige al API Gateway (http://localhost:8080)
npm test         # Vitest + Testing Library (incluye la guardia de contraste)
npm run build    # tsc -b + build de producción en dist/
```

Para usar la app de punta a punta: `docker compose up -d` en la raíz y `./mvnw spring-boot:run` en `ms-auth/`.

## Estructura

| Carpeta | Contenido |
|---|---|
| `src/app/` | Rutas, diseño principal (encabezado, pie, botón flotante), ruta protegida, **`estilos.css` (tokens)** |
| `src/features/<caso de uso>/` | Páginas, estado y llamadas de cada caso de uso (p. ej. `auth/`) |
| `src/shared/api/` | Cliente HTTP (`fetch` + token Bearer) y almacenamiento del token |
| `src/shared/components/` | Componentes reutilizables, iconos e ilustraciones |
| `src/i18n/` | Textos en `es.json` y `en.json` (RF03, RNF08) |

## Dependencias

Versiones instaladas (`package-lock.json`) y licencias leídas de `node_modules`. Toda dependencia nueva se agrega aquí con su licencia y su razón.

| Dependencia | Versión | Licencia | Para qué |
|---|---|---|---|
| `react`, `react-dom` | 19.3.0 | MIT | Interfaz |
| `react-router-dom` | 7.18.4 | MIT | Rutas; los filtros de WF-05 viven en la URL |
| `i18next`, `react-i18next` | 26.4.3, 17.0.16 | MIT | Textos ES/EN y plurales (RF03, RNF08) |
| `@fontsource-variable/fraunces`, `@fontsource-variable/figtree` | 5.3.0 | OFL-1.1 | Tipografías empaquetadas (sin cargarlas desde internet) |
| `leaflet` | 1.9.4 | BSD-2-Clause | Mapa de WF-05 (`MapaOferta.tsx`), usado directamente |
| **Desarrollo** | | | |
| `vite`, `@vitejs/plugin-react` | 8.3.4, 6.1.2 | MIT | Servidor de desarrollo, proxy `/api` y build |
| `typescript` | 6.0.3 | Apache-2.0 | Tipos (`tsc -b` en el build) |
| `vitest`, `jsdom` | 5.0.3, 30.1.2 | MIT | Pruebas |
| `@testing-library/react`, `/dom`, `/jest-dom`, `/user-event` | 16.3.3, 10.4.2, 7.0.1, 14.6.7 | MIT | Pruebas por roles y nombres accesibles |
| `@types/react`, `@types/react-dom`, `@types/node`, `@types/leaflet` | 19.3.0, 19.3.0, 24.19.2, 1.9.22 | MIT | Tipos |

**Excluida: `react-leaflet`.** Su licencia es Hippocratic-2.1, que restringe usos por criterios éticos y no es una licencia de código abierto aprobada por la OSI; se descartó para no condicionar el uso del software de la Alcaldía. El mapa usa Leaflet directamente: se crea en un `useEffect`, se actualiza por capas (`layerGroup`) y la limpieza llama a `map.remove()`.

**Carga diferida.** `/explorar` se carga con `React.lazy` (`src/app/App.tsx`), así que Leaflet y su CSS solo se descargan al entrar ahí. En el build, Leaflet queda en su propio archivo (`leaflet-[hash].js` y `.css`, grupo `codeSplitting` en `vite.config.ts`). Mientras llega la página se ve el estado de carga del catálogo; si la descarga falla, el límite de errores (`shared/components/LimiteErrores.tsx`) muestra «No pudimos cargar esta sección. Revisa tu conexión.» con «Reintentar», que recarga la página. Como `leaflet.css` llega después del CSS principal, los ajustes de `catalogo.css` sobre clases de Leaflet llevan el prefijo `.mapa-oferta` para no depender del orden.

**Teselas de OpenStreetMap: el único recurso externo.** Se piden a `tile.openstreetmap.org` según su [política de uso](https://operations.osmfoundation.org/policies/tiles/):
- la atribución «© OpenStreetMap contributors» está siempre visible;
- el zoom máximo es 19;
- no hay descarga masiva, prefetching ni caché offline: solo se piden las teselas del área visible;
- se envía el `Referer` por defecto del navegador.

Es adecuado para el prototipo; para un despliegue con tráfico real hace falta un proveedor de teselas propio o contratado. En las pruebas, Leaflet se reemplaza por `src/test/leafletSimulado.ts` (jsdom no dibuja mapas), así que no se pide ninguna tesela.

## Pendientes de pruebas

Las pruebas no hacen peticiones de red reales. Una guardia en `src/test/setup.ts` hace fallar cualquier prueba que
llame a `fetch` sin `simularFetch`.

El límite por defecto de Vitest es de 5 s por prueba. Las que tardaron **más de 2 s** en alguna ejecución tienen
15 s, con un comentario junto al `it` (el tercer argumento). Mediciones: máximo de 12 ejecuciones de `npm test`
con el stack de Docker detenido, en una PC Windows del equipo (octubre de 2026).

| Prueba | Máximo medido | Causa probable |
|---|---|---|
| `explorar.test.tsx` › los filtros arman la consulta a la API… | 4,8 s | Primera del archivo: paga la carga diferida de `/explorar` (Vitest transforma `ExplorarPage` dentro de la prueba). Además: tecleo, dos clics y «Aplicar», que vuelve a montar `FiltrosOferta` y repite la consulta. |
| `inicioSesion.test.tsx` › un login exitoso redirige al inicio… | 4,2 s | Primer render de la app en el archivo, 34 pulsaciones de `usuario.type` y navegación a `/` (monta `InicioPage` con su consulta de destacados). |
| `explorarDiferido.test.tsx` › mientras llega la página… | 3,8 s | Única del archivo: primer render de la app y transformación del módulo diferido. |
| `inicioSesion.test.tsx` › un 423 muestra la hora de desbloqueo… | 3,5 s | 33 pulsaciones de `usuario.type`. |
| `idioma.test.tsx` › traduce los textos y conserva el correo… | 3,2 s | Primer render de la app en el archivo, 23 pulsaciones y cambio de idioma (re-renderiza todo el árbol). |
| `idioma.test.tsx` › el mensaje de error mostrado también se traduce | 3,2 s | 33 pulsaciones, envío con `findBy` y cambio de idioma. |
| `rutaProtegida.test.tsx` › después de iniciar sesión vuelve a la ruta privada | 2,9 s | 34 pulsaciones de `usuario.type` y regreso a la ruta privada. |
| `explorarMapa.test.tsx` › no pide la ubicación al cargar la página | 2,7 s | Primera del archivo: paga la carga diferida de `/explorar`. |
| `inicio.test.tsx` › la búsqueda lleva a /explorar con el texto | 2,6 s | Primera del archivo: primer render de la app y 15 pulsaciones. |
| `inicioSesion.test.tsx` › un 401 muestra el mensaje de credenciales inválidas | 2,5 s | 33 pulsaciones de `usuario.type`. |
| `inicioSesion.test.tsx` › un 400 muestra el mensaje de datos inválidos | 2,5 s | 23 pulsaciones de `usuario.type`. |
| `inicioSesion.test.tsx` › un 403 muestra el mensaje de cuenta inactiva | 2,2 s | 34 pulsaciones de `usuario.type`. |

En la app no hay temporizadores ni debounce. El costo es del tecleo: cada pulsación de `usuario.type` espera un
`setTimeout(0)` de user-event (`delay: 0` por defecto) y re-renderiza el formulario. A eso se suma el primer render
y la carga diferida de la primera prueba de cada archivo. Las pruebas de `inicioSesion` que no son la primera, con el
mismo tecleo, tardan de 2,0 a 2,5 s: el tecleo pesa unos 2 s, y el primer render del archivo, entre 1 y 2 s.

**Ideas para la rama de optimización (no aplicadas):**
1. `userEvent.setup({ delay: null })` en `src/test/renderApp.tsx`: quita la espera entre pulsaciones.
2. `usuario.paste(...)` (con el campo enfocado) en los campos que la prueba no está evaluando, por ejemplo el correo
   y la contraseña cuando lo que se prueba es la respuesta del servidor. `usuario.type` se deja solo donde importa
   el tecleo: validación mientras se escribe, o conservar el valor al cambiar de idioma.

Después de optimizar, volver a medir y quitar los 15 s de las pruebas que bajen de 2 s.

---

## Guía visual

Los wireframes fijan la **estructura** (qué elementos hay y en qué región). El **diseño** es libre, pero coherente en todas las pantallas: toda pantalla nueva usa los tokens de `src/app/estilos.css` y no define colores, tamaños ni sombras propios.

**Identidad «Santa Marta»:** el mar Caribe (azules verdosos profundos), el atardecer de El Rodadero (coral y sol), la Sierra Nevada (verde y picos nevados), el café de la Sierra y la arena de la playa. Se busca una apariencia cálida y de póster de viaje, con mucho aire y formas redondeadas.

### Color

Siempre a través de los tokens `var(--color-…)`; nunca hex sueltos en los componentes.

| Token | Hex | Úsalo para | No lo uses para |
|---|---|---|---|
| `--color-tinta` | `#14272D` | Texto principal y títulos | — |
| `--color-tinta-suave` | `#4A5E63` | Subtítulos, ayudas, texto legal, correo en el menú | Texto sobre fondos oscuros |
| `--color-arena` | `#FBF6EE` | Fondo de la página | Tarjetas (usa superficie) |
| `--color-arena-200` | `#F3EADB` | Superficies tenues: avisos informativos | Fondo de la página |
| `--color-superficie` | `#FFFFFF` | Tarjetas, menús, campos; texto sobre mar-700, mar-900 y atardecer | — |
| `--color-borde` | `#E2D9CA` | Separadores y contornos decorativos de tarjetas | Borde de campos (no llega a 3:1) |
| `--color-borde-campo` | `#7C8D91` | Borde de campos de formulario | Texto |
| `--color-deshabilitado` | `#5C6D71` | Elementos «Próximamente», placeholder | Elementos activos |
| `--color-mar-900` | `#073B44` | Fondo del pie, avatar, títulos destacados | Botones de acción |
| `--color-mar-700` | `#0B6574` | Enlaces, pestaña e idioma activos, navegación activa, **foco** | Botón principal (es del atardecer) |
| `--color-mar-500` | `#1497A6` | **Solo decorativo**: olas, ilustraciones | Texto o bordes con significado |
| `--color-mar-100` | `#DCF1F2` | Hover de opciones, fondos suaves, halo del foco en campos | Texto |
| `--color-atardecer-600` | `#C2461F` | **Botón principal (CTA)**: una acción principal por pantalla; acentos de avisos | Texto largo, enlaces |
| `--color-atardecer-700` | `#A83A17` | Hover y estado activo del CTA | — |
| `--color-atardecer-500` | `#E2603A` | **Solo decorativo**: sol, degradados, anillo del avatar abierto | Texto |
| `--color-sol-400` | `#F4B740` | Anillo del avatar, **foco sobre fondos oscuros**, ilustraciones | Texto sobre fondos claros |
| `--color-sol-100` | `#FDF1D6` | Fondo de la insignia «Próximamente», selección de texto | — |
| `--color-cafe-700` | `#6A3E1E` | Texto de la insignia «Próximamente» | — |
| `--color-sierra-700` | `#2E6A4E` | Ilustraciones; reservado para estados de éxito futuros | — |
| `--color-error` | `#B42318` | Mensajes y bordes de error | Decoración |
| `--color-error-fondo` | `#FDECEA` | Fondo del mensaje de error | — |
| `--color-pie-texto` | `#E8F3F3` | Títulos y texto del pie (sobre mar-900) | Fondos claros |
| `--color-pie-suave` | `#A9CED2` | Enlaces y derechos del pie (sobre mar-900) | Fondos claros |

**Contraste (WCAG 2.x AA).** Texto ≥ 4,5:1; bordes de campos, foco e iconos con significado ≥ 3:1. Los pares en uso están verificados en `src/app/tokens.test.ts`, que lee los tokens reales de `estilos.css`.

| Par | Ratio |
|---|---|
| tinta / arena · superficie | 14,37 · 15,47 |
| tinta-suave / arena · superficie · mar-100 · sol-100 | 6,35 · 6,84 · 5,83 · 6,10 |
| mar-900 / mar-100 · sol-100 (saludo del inicio) | 10,42 · 10,90 |
| atardecer-600 / arena-200 (icono y borde del aviso, no textual) | 4,20 |
| deshabilitado / superficie · arena | 5,41 · 5,03 |
| superficie / atardecer-600 · atardecer-700 (CTA) | 5,01 · 6,40 |
| superficie / mar-700 · mar-900 | 6,71 · 12,22 |
| mar-700 / superficie · arena · mar-100 | 6,71 · 6,24 · 5,73 |
| error / superficie · error-fondo | 6,57 · 5,75 |
| café-700 / sol-100 | 8,07 |
| pie-texto · pie-suave / mar-900 | 10,79 · 7,24 |
| borde-campo / superficie · arena (no textual) | 3,46 · 3,21 |
| sol-400 / mar-900 (foco en el pie) | 6,81 |

**Al agregar un color o un par nuevo:** añádelo como token en `:root` y agrega el par a `PARES` en `tokens.test.ts`. Si la prueba falla, el color no se usa.

### Tipografía

Fuentes empaquetadas con `@fontsource-variable` (licencia OFL-1.1), importadas en `src/main.tsx`; no se cargan desde internet.

| Token | Fuente | Uso |
|---|---|---|
| `--fuente-titulos` | **Fraunces** (serif variable, cálida, de póster de viaje) | h1, h2, logo, saludo, iniciales del avatar, nombre en el menú |
| `--fuente-texto` | **Figtree** (sans geométrica variable) | Todo lo demás: texto, campos, botones, navegación |

| Token | Tamaño | Uso típico |
|---|---|---|
| `--texto-xs` | 0,75 rem | Insignias, texto legal, derechos del pie |
| `--texto-sm` | 0,875 rem | Navegación, etiquetas, avisos, pie |
| `--texto-base` | 1 rem | Texto y botones |
| `--texto-lg` | 1,125 rem | Subtítulos, h2 de secciones pequeñas |
| `--texto-xl` | 1,375 rem | Título de tarjeta, logo |
| `--texto-2xl` | 1,75 rem | h2 de sección |
| `--texto-3xl` | `clamp(2rem, 4vw, 2.75rem)` | h1 de página |

Interlineado: `--interlineado` (1,55) para texto y `--interlineado-titulos` (1,15) para títulos. Pesos: 400 texto, 500–600 etiquetas y botones secundarios, 650 títulos y CTA.

### Espaciado

Escala con base de 4 px. Usa solo estos valores en `margin`, `padding` y `gap`.

| Token | Valor | Uso típico |
|---|---|---|
| `--espacio-1` | 0,25 rem | Ajustes finos, padding de insignias |
| `--espacio-2` | 0,5 rem | Entre icono y texto, entre etiqueta y campo |
| `--espacio-3` | 0,75 rem | Padding de avisos y de opciones de menú |
| `--espacio-4` | 1 rem | Separación entre campos; margen lateral en móvil |
| `--espacio-5` | 1,5 rem | Padding de tarjetas; separación entre bloques |
| `--espacio-6` | 2 rem | Separación entre secciones |
| `--espacio-7` | 3 rem | Padding de banners y del pie |
| `--espacio-8` | 4 rem | Separación entre columnas en escritorio |
| `--espacio-9` | 6 rem | Aire inferior de la página (deja sitio al botón flotante) |

### Radios

| Token | Valor | Uso |
|---|---|---|
| `--radio-sm` | 8 px | Campos, avisos, mensajes de error, opciones de menú |
| `--radio-md` | 14 px | Tarjetas y menús |
| `--radio-lg` | 24 px | Paneles ilustrados y banners |
| `--radio-pill` | 999 px | Botones, selector de idioma, insignias, botón flotante |

### Sombras

Con tinte del mar (`rgb(7 59 68 / …)`), nunca gris puro.

| Token | Uso |
|---|---|
| `--sombra-1` | Reposo: botón principal, marca del logo |
| `--sombra-2` | Tarjetas, panel ilustrado, banners; hover del CTA |
| `--sombra-3` | Elementos flotantes: menú del avatar, botón del asistente |

### Movimiento

- `--transicion` (160 ms, `ease-out`) para hover, foco y cambios de estado. Animaciones de entrada: `aparecer` (menús, mensajes) y `subir` (contenido de página).
- Movimiento sutil y funcional: elevar 1 px el CTA en hover, deslizar el subrayado de la navegación. Nada de animaciones en bucle salvo el indicador de carga.
- `prefers-reduced-motion: reduce` desactiva animaciones y transiciones (regla global en `estilos.css`).

### Foco

- `--foco`: `3px solid var(--color-mar-700)` con `outline-offset: 2px` mediante `:focus-visible`, para todos los elementos interactivos.
- Campos: borde mar-700, halo `mar-100` y `outline` de 2 px en `:focus-within` del contenedor.
- Sobre fondos oscuros (pie), se redefine `--foco` con `--color-sol-400`. Hay que redefinir `--foco` y no solo el color, porque las variables se resuelven donde se declaran.
- Nunca `outline: none` sin un reemplazo visible.

### Iconos e ilustraciones

- **Iconos** (`src/shared/components/Iconos.tsx`): de línea, cuadrícula de 24, trazo 1,75 redondeado y `currentColor`. Siempre `aria-hidden`: el nombre accesible lo da el texto o el `aria-label` del control.
- **Ilustraciones** (`src/shared/components/Ilustraciones.tsx`): SVG propios, sin fotos de terceros. Usan los tokens (`var(--color-…)`), así que siguen la paleta.
  - `MarcaSamaria`: sol poniéndose sobre olas; es el logo.
  - `EscenaSantaMarta`: atardecer, Sierra Nevada nevada y mar Caribe (panel del login).
  - `Ola`: borde ondulado (`currentColor`) para pies y banners.
- Todo lo decorativo lleva `aria-hidden="true"` y `focusable="false"`, y no agrega texto. Si un elemento nuevo tiene **función**, se registra en `docs/trazabilidad.md` como «Mejora de diseño».

### Patrones de componente

| Patrón | Clases | Reglas |
|---|---|---|
| Botón principal | `.boton-principal` | Píldora atardecer-600, texto blanco, alto mínimo 48 px. Uno por pantalla. |
| Enlace o navegación | `.navegacion a` | Texto tinta; hover y activo en mar-700 con subrayado deslizante. |
| Campo | `.campo`, `.campo__control` | `<label for>` visible, icono `aria-hidden`, alto 48 px, borde `--color-borde-campo`; error con `aria-invalid`, `aria-describedby` y `.campo__error`. |
| Tarjeta | `.tarjeta`, `.tarjeta__titulo`, `.tarjeta__cuerpo` | Superficie, `--radio-md`, `--sombra-2`, título en Fraunces. |
| Aviso informativo | `.aviso-intentos` | arena-200 con borde izquierdo atardecer-600 e icono. |
| Mensaje de error | `.login__error` | Región `aria-live` siempre presente; el contenido es error sobre error-fondo con borde izquierdo. |
| Fuera de alcance | `BotonProximamente` + `.insignia-proximamente` | `aria-disabled` (sigue siendo enfocable), color deshabilitado e insignia sol-100 / café-700. Nunca `disabled` ni texto sin la insignia o el tooltip. |
| Carga | `.estado-carga` | `role="status"` y `aria-busy`, con indicador giratorio decorativo. |
| Botón secundario | `.boton-secundario` (en `estilos.css`, como `.boton-principal`) | Píldora con borde mar-700 sobre superficie: acciones de apoyo (Reintentar, Quitar filtros). |
| Chip de filtro | `.chip` | mar-900 sobre mar-100, removible; `aria-label` que diga qué filtro quita. |
| Aviso de sesión | `.aviso-sesion` | tinta sobre sol-100 con borde sol-400, `role="status"` y botón para cerrarlo. |
| Tarjeta de oferta | `.tarjeta-oferta` (`features/catalogo/catalogo.css`) | Imagen con `alt=""` y respaldo por categoría, nombre h3, datos en `<dl>`; «—» con texto «sin datos» para lectores. |
| Insignia de estado | `.insignia-estado--<EstadoOperativo>` | Abierto: mar-700/mar-100 · Cerrado temporalmente: error/error-fondo · En mantenimiento: café-700/sol-100. |
| Estado de búsqueda | `EstadoCatalogo` | Carga, vacío, fuera de rango y errores (503, 400, red); el error solo afecta a su región. |
| Marcador del mapa | `.marcador--servicio`, `--atractivo`, `--ubicacion` | `L.divIcon` con SVG propio que toma el color de la clase: pin atardecer-600 (servicio) o mar-700 (atractivo) con borde blanco; «Tu ubicación» es un círculo sol-400 con borde mar-900. Enfocable, con `title` y `aria-label`; la ventana emergente se arma con `textContent`. |
| Resaltado marcador↔tarjeta | `.marcador--resaltado`, `.tarjeta-oferta--resaltada` | Marcador 1,3× y al frente; tarjeta con anillo mar-700 y `--sombra-2`. |
| Selector de vista | `.vista-selector` | `role="group"` con dos botones `aria-pressed` (mar-700); solo por debajo de 64 rem. |
| Aviso de ubicación | `.aviso-ubicacion` | `role="status"` siempre presente; mar-900 sobre mar-100 con borde izquierdo mar-700 cuando tiene texto. |

### Maquetación y responsivo

- Contenido centrado con `--ancho-maximo` (76 rem) y márgenes laterales de `--espacio-4`.
- Funciona desde **360 px** sin scroll horizontal. En menos de 640 px (`40rem`), la navegación pasa a una segunda fila desplazable y el botón flotante muestra solo el icono.
- Desde 960 px (`60rem`), las pantallas con panel ilustrado usan dos columnas (ilustración y contenido).
- WF-05: desde 1024 px (`64rem`), mapa y lista lado a lado; por debajo, el selector «Lista / Mapa» muestra uno a la vez. El mapa ocupa el 100 % de su columna, sin ancho fijo.

### Lista de verificación para cada pantalla nueva

1. Solo tokens: ningún hex, tamaño o sombra sueltos.
2. Los pares de color nuevos están en `tokens.test.ts` y pasan AA.
3. Todo control tiene nombre accesible; todo campo, su `<label>`; los errores se anuncian con `aria-live`.
4. Recorrido completo con teclado y foco visible en cada elemento.
5. Decoración con `aria-hidden`; sin fotos sin licencia.
6. Sin scroll horizontal a 360 px; animaciones respetan `prefers-reduced-motion`.
7. Los elementos del wireframe fuera de alcance usan `BotonProximamente` y figuran como pendientes en `docs/trazabilidad.md`.
