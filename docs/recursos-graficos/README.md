# Recursos gráficos

Fotos de los lugares del catálogo (WF-04 y WF-05), la foto de la portada del inicio, el logo y los favicons, y cómo se
preparan para la web.

## Origen y créditos

- Los originales están en `_entrada/imagenes/`, fuera del repositorio (`.gitignore`), y **no se modifican**.
- **No hay créditos verificados.** En `creditos.xlsx`, las columnas autor, enlace de origen y licencia están vacías
  en las 21 filas, y no hay columna de fuente. Según CLAUDE.md («Imágenes del catálogo»), las fotos se integran igual
  porque el prototipo solo se ejecuta en local con fines académicos, y **no se inventan ni se deducen créditos**.
- En lugar de una página de créditos, el pie de página muestra, en ES y EN: «Prototipo académico — Universidad del
  Magdalena. Las imágenes son de referencia y pueden pertenecer a terceros; no se usan con fines comerciales.».
- `categorias/` llegó vacía: las tarjetas usan `frontend/public/imagenes/catalogo/categorias/*.svg` como respaldo
  cuando una foto no carga.
- El SVG propio `MarcaSamaria` (`frontend/src/shared/components/Ilustraciones.tsx`) queda como respaldo del logo.

## Cómo se generan

```bash
cd frontend
node scripts/optimizar-imagenes.mjs ../_entrada/imagenes   # requiere ffmpeg y ffprobe en el PATH
```

`frontend/scripts/optimizar-imagenes.mjs` (con ffmpeg 8.1.1 y libwebp) procesa cada foto así:

- recorta a **3:2 exacto**, centrado salvo el desplazamiento indicado en su manifiesto;
- la reduce a 1200×800 solo si es más grande, **nunca la amplía**;
- **quita los metadatos** (EXIF, cámara, GPS);
- la guarda en WebP con calidad 82, que baja de 4 en 4 hasta pesar 200 KB o menos, **con piso de 60**;
- un WebP que ya cumple y no lleva metadatos se copia sin recomprimir.

El nombre es el **slug de la semilla** de MongoDB (`infra/mongo/init/03-semilla-catalogo.js`), en kebab-case. El
resultado queda versionado en `frontend/public/imagenes/catalogo/`, así que el resto del equipo no necesita ffmpeg.

Después de cambiar la semilla hay que recrear el volumen de MongoDB, porque los scripts de `init/` solo corren con el
volumen vacío: `docker compose down -v` y `docker compose up -d --build`. Ojo: `down -v` también recrea PostgreSQL
desde la semilla `dev`.

En el sitio, la tarjeta declara `width="600" height="400"` y `aspect-ratio: 3 / 2`. nginx sirve `/imagenes/` con
`Cache-Control: public, max-age=3600` más `ETag` (no `immutable`, porque los nombres no llevan hash) y responde 404
si el archivo no existe.

## Ambiente: portada del inicio

| Archivo publicado | Original | Formato real del original | Tamaño original | Tamaño final | Peso | Calidad |
|---|---|---|---|---|---|---|
| `imagenes/ambiente/portada-inicio.webp` | `portada-inicio.webp` | JPEG | 1920×1080 | 1920×1080 | 241 KB (límite 250 KB) | 82 |

- El original trae extensión `.webp` pero es un JPEG. El script detecta el formato por el contenido.
- **Marca de agua**: se revisó la esquina superior izquierda del original y la zona que se ve en el sitio (con
  `object-position: 50% 60%`). No hay ninguna marca, solo nubes, así que no se recorta. Lo que parecía un ícono era
  la esquina redondeada del velo con bordes difuminados que se usaba antes, más una nube en sombra.
- **Contraste**: la foto se oscurece de forma **uniforme**, de borde a borde, con mar-900 al `--opacidad-velo-texto`
  (**0,55**).
  - Con esa opacidad, el texto blanco sobre el peor fondo posible (un píxel blanco) da **3,24:1**: alcanza para texto
    grande WCAG (3:1), y no para texto normal (4,5:1).
  - Por eso el título (32–44 px) y el texto de apoyo (19 px en negrita 700, por encima de los 18,67 px de 14 pt) son
    texto grande y blanco.
  - `frontend/src/app/tokens.test.ts` comprueba ese peor caso.
  - **Medido sobre la foto real**, sin el texto ni su sombra (WCAG no cuenta la sombra):

    | Texto | ES, 1366 px | ES, 390 px | EN, 1366 px | EN, 390 px |
    |---|---|---|---|---|
    | Título | 3,32 | 3,36 | 3,53 | 3,88 |
    | Texto de apoyo | 3,28 | 3,38 | 3,28 | 3,38 |

    Siempre es mayor o igual que el peor caso. Las sombras del texto y la del buscador solo ayudan a leer.
- **Panel del login: descartado.** WF-01 no tiene imagen, así que se quitaron la foto (`panel-login`, que era un JPEG
  de 399×501) y la ilustración `EscenaSantaMarta`. El archivo ya no se publica ni lo genera el script.

## Marca: logo y favicons

Se generan a partir de `marca/logo-samaria360.png` (512×512, fondo transparente; `icono-512.png` es el mismo
archivo). La línea vertical blanca que parte el logo es intencional.

| Archivo publicado | Tamaño | Uso |
|---|---|---|
| `imagenes/marca/samaria360-icono.webp` | 80×80, sin pérdida, con transparencia | Encabezado (40 px) y pie (36 px, sobre un círculo de arena para que se vea sobre mar-900) |
| `favicon-32.png`, `favicon-16.png` | 32×32, 16×16, con transparencia | Favicon |
| `apple-touch-icon.png` | 180×180, **opaco** sobre `--color-arena` | iOS rellena la transparencia con negro |

El logo horizontal (`logo-samaria360-horizontal.png`) no se usa en la interfaz: su texto marrón no se lee sobre el pie
oscuro, y el nombre ya va como texto HTML junto al ícono.

## Resultado (21 lugares)

| Archivo publicado | Original | Tamaño original | Tamaño final | Peso | Calidad | Observación |
|---|---|---|---|---|---|---|
| `hotel-casa-tayrona.webp` | `hotelcasatayrona.jpg` | 734×418 | 627×418 | 109 KB | 82 | bajo 1200×800 |
| `cabanas-el-roble.webp` | `cabanaselroble.jpg` | 678×452 | 678×452 | 60 KB | 82 | bajo 1200×800 |
| `hostal-bahia-taganga.webp` | `hostalbahiataganga.webp` | 1600×900 | 1200×800 | 161 KB | 82 | — |
| `ecocabanas-canaveral.webp` | `ecocabanascanaveral.webp` | 1200×900 | 1200×800 | 192 KB | 78 | — |
| `restaurante-ouzo.webp` | `restauranteouzosantamarta.jpg` | 547×365 | 546×364 | 39 KB | 82 | bajo 1200×800 |
| `mercado-la-plaza.webp` | `mercadogastronomicolaplaza.jpg` | 800×800 | 798×532 | 77 KB | 82 | bajo 1200×800 |
| `mirador-gourmet-taganga.webp` | `miradorgourmettaganga.jpg` | 387×516 | 387×258 | 12 KB | 82 | bajo 1200×800 |
| `cocina-caribe-el-rodadero.webp` | `cocinacaribeelrodadero.jpg` | 1280×900 | 1200×800 | 127 KB | 82 | — |
| `tour-cafe-minca.webp` | `tourdecafeenminca.webp` | 1200×800 | 1200×800 | 160 KB | — (copiada) | — |
| `tour-cafe-samaria-alta.webp` | `tourdecafesamariaalta.avif` | 780×521 | 780×520 | 66 KB | 82 | bajo 1200×800 |
| `caminata-pueblito-tayrona.webp` | `caminataapueblitotayrona.webp` | 1280×800 | 1200×800 | 118 KB | 82 | — |
| `snorkel-bahia-concha.webp` | `snorkelenbahiaconcha.jpg` | 2048×1365 | 1200×800 | 161 KB | 82 | — |
| `kayaks-taganga.webp` | `alquilerdekayakstaganga.jpg` | 1400×821 | 1200×800 | 113 KB | 82 | — |
| `bicicletas-centro-historico.webp` | `bicicletascentrohistorico.webp` | 1440×1080 | 1200×800 | 169 KB | 82 | — |
| `cascada-quebrada-valencia.webp` | `cascadaquebradavalencia.jpg` | 1600×1200 | 1200×800 | 273 KB | 60 | **> 200 KB con calidad 60** |
| `cascada-el-salto.webp` | `cascadaelsaltominca.jpg` | 1920×1280 | 1200×800 | 181 KB | 74 | — |
| `cabo-san-juan.webp` | `cabosanjuandelguia.jpg` | 547×365 | 546×364 | 28 KB | 82 | bajo 1200×800 |
| `playa-cristal.webp` | `playacristal.webp` | 1200×800 | 1200×800 | 105 KB | — (copiada) | — |
| `quinta-san-pedro-alejandrino.webp` | `quintadesanpedroalejandrino.jpg` | 3925×2874 | 1200×800 | 182 KB | 82 | — |
| `catedral-santa-marta.webp` | `catedralbasilicadesantamarta.jpg` | 677×453 | 675×450 | 33 KB | 82 | bajo 1200×800 |
| `mirador-taganga.webp` | `miradordetaganga.jpg` | 1024×768 | 1023×682 | 135 KB | 82 | bajo 1200×800 |

### Por debajo de 1200×800

No había fotos de reemplazo. Estas se publican a su tamaño real, recortadas a 3:2 y sin ampliar:

- `hotel-casa-tayrona`: 627×418 (original 734×418)
- `cabanas-el-roble`: 678×452 (original 678×452)
- `restaurante-ouzo`: 546×364 (original 547×365)
- `mercado-la-plaza`: 798×532 (original 800×800)
- `mirador-gourmet-taganga`: 387×258 (original 387×516)
- `tour-cafe-samaria-alta`: 780×520 (original 780×521)
- `cabo-san-juan`: 546×364 (original 547×365)
- `catedral-santa-marta`: 675×450 (original 677×453)
- `mirador-taganga`: 1023×682 (original 1024×768)

### Recortes elegidos a mano

- `mirador-gourmet-taganga`: el original es vertical (387×516). El recorte va desde y = 200, para que entren el
  horizonte con el atardecer, el corazón de neón y la mesa.
- `mercado-la-plaza`: el recorte centrado cortaba el letrero «LA PLAZA». Va desde y = 90.

### Duplicados que no se usan

- `cabosanjuandelguia.webp`: se usa el `.jpg`, que es el que figura en el Excel.
- `tourdecafesamariaalta.webp`: se usa el `.avif`, que es el que figura en el Excel.

### Pendiente

Texto alternativo y dimensiones por imagen en `catalog_db` (cambio de `$jsonSchema`), cuando se implemente el registro
de fotos del prestador. Está en `docs/trazabilidad.md`.
