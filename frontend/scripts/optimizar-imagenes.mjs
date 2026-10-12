#!/usr/bin/env node
/**
 * Optimiza las imágenes del sitio a partir de _entrada/imagenes/ (requiere ffmpeg y ffprobe en el PATH). Los originales no
 * se modifican; el formato se detecta por el contenido, no por la extensión.
 *
 *   node scripts/optimizar-imagenes.mjs ../_entrada/imagenes [--informe informe.json]
 *
 * lugares/ → public/imagenes/catalogo/<slug>.webp, con el slug de la semilla de MongoDB
 * (infra/mongo/init/03-semilla-catalogo.js). Por cada foto:
 *   - recorte a 3:2 exacto (centrado, salvo el desplazamiento indicado en LUGARES);
 *   - reducción a 1200×800 con lanczos solo si es más grande: nunca se amplía;
 *   - sin metadatos (EXIF, cámara, GPS);
 *   - WebP con calidad 82, que baja de 4 en 4 hasta pesar ≤ 200 KB, con piso de 60.
 * Un WebP que ya cumple todo y no lleva metadatos (formato simple, sin bloque VP8X) se copia sin recomprimir.
 *
 * ambiente/ → public/imagenes/ambiente/portada-inicio.webp: sin recorte, ancho máximo 1920, nunca se amplía, mismo
 * bucle de calidad con límite de 250 KB.
 *
 * marca/ → el ícono del logo (public/imagenes/marca/samaria360-icono.webp, WebP sin pérdida con transparencia) y los
 * favicons (public/favicon-16.png, favicon-32.png y apple-touch-icon.png, este último opaco sobre --color-arena,
 * porque iOS rellena la transparencia con negro).
 * El resultado queda versionado: el equipo no necesita ffmpeg para trabajar. Detalle en docs/recursos-graficos/README.md.
 */
import { execFileSync } from 'node:child_process'
import { copyFileSync, mkdirSync, mkdtempSync, openSync, readSync, closeSync, rmSync, statSync, writeFileSync } from 'node:fs'
import { tmpdir } from 'node:os'
import { dirname, join, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const ANCHO_MAXIMO = 1200
const ALTO_MAXIMO = 800
const PESO_MAXIMO = 200 * 1024
const CALIDAD_INICIAL = 82
const PASO_CALIDAD = 4
const CALIDAD_MINIMA = 60

/** Archivo de _entrada/imagenes/lugares → slug de la semilla. `y`: desplazamiento vertical del recorte (px del original). */
const LUGARES = [
  { origen: 'hotelcasatayrona.jpg', slug: 'hotel-casa-tayrona' },
  { origen: 'cabanaselroble.jpg', slug: 'cabanas-el-roble' },
  { origen: 'hostalbahiataganga.webp', slug: 'hostal-bahia-taganga' },
  { origen: 'ecocabanascanaveral.webp', slug: 'ecocabanas-canaveral' },
  { origen: 'restauranteouzosantamarta.jpg', slug: 'restaurante-ouzo' },
  // El centrado cortaba el letrero «LA PLAZA»: desde y = 90 entran el letrero y el bar.
  { origen: 'mercadogastronomicolaplaza.jpg', slug: 'mercado-la-plaza', y: 90 },
  // Vertical (387×516): el recorte deja dentro el horizonte, el corazón de neón y la mesa.
  { origen: 'miradorgourmettaganga.jpg', slug: 'mirador-gourmet-taganga', y: 200 },
  { origen: 'cocinacaribeelrodadero.jpg', slug: 'cocina-caribe-el-rodadero' },
  { origen: 'tourdecafeenminca.webp', slug: 'tour-cafe-minca' },
  // El .avif es el que figura en creditos.xlsx; el .webp de la misma foto no se usa.
  { origen: 'tourdecafesamariaalta.avif', slug: 'tour-cafe-samaria-alta' },
  { origen: 'caminataapueblitotayrona.webp', slug: 'caminata-pueblito-tayrona' },
  { origen: 'snorkelenbahiaconcha.jpg', slug: 'snorkel-bahia-concha' },
  { origen: 'alquilerdekayakstaganga.jpg', slug: 'kayaks-taganga' },
  { origen: 'bicicletascentrohistorico.webp', slug: 'bicicletas-centro-historico' },
  { origen: 'cascadaquebradavalencia.jpg', slug: 'cascada-quebrada-valencia' },
  { origen: 'cascadaelsaltominca.jpg', slug: 'cascada-el-salto' },
  // El .jpg es el que figura en creditos.xlsx; el .webp de la misma foto no se usa.
  { origen: 'cabosanjuandelguia.jpg', slug: 'cabo-san-juan' },
  { origen: 'playacristal.webp', slug: 'playa-cristal' },
  { origen: 'quintadesanpedroalejandrino.jpg', slug: 'quinta-san-pedro-alejandrino' },
  { origen: 'catedralbasilicadesantamarta.jpg', slug: 'catedral-santa-marta' },
  { origen: 'miradordetaganga.jpg', slug: 'mirador-taganga' },
]

function dimensiones(archivo) {
  const salida = execFileSync('ffprobe', ['-v', 'error', '-select_streams', 'v:0', '-show_entries',
    'stream=width,height', '-of', 'csv=p=0', archivo], { encoding: 'utf8' })
  const [ancho, alto] = salida.trim().split('\n')[0].split(',').map(Number)
  return { ancho, alto }
}

/** WebP «simple» (VP8 o VP8L, sin el bloque extendido VP8X que puede llevar EXIF o XMP). */
function esWebpSimple(archivo) {
  const cabecera = Buffer.alloc(16)
  const fd = openSync(archivo, 'r')
  readSync(fd, cabecera, 0, 16, 0)
  closeSync(fd)
  return cabecera.toString('ascii', 0, 4) === 'RIFF' && cabecera.toString('ascii', 8, 12) === 'WEBP'
    && cabecera.toString('ascii', 12, 16) !== 'VP8X'
}

/** Mayor recorte 3:2 exacto que cabe en la imagen (ancho = 3k, alto = 2k). */
function recorte({ ancho, alto }, y) {
  const k = Math.min(Math.floor(ancho / 3), Math.floor(alto / 2))
  const w = 3 * k
  const h = 2 * k
  const x = Math.floor((ancho - w) / 2)
  const yFinal = y === undefined ? Math.floor((alto - h) / 2) : Math.min(Math.max(0, y), alto - h)
  return { w, h, x, y: yFinal }
}

/** Arena de estilos.css (--color-arena): fondo del apple-touch-icon. */
const COLOR_ARENA = '0xfbf6ee'

/** ambiente/: foto de la portada del inicio. (El panel del login se descartó: WF-01 no tiene imagen.) */
const AMBIENTE = [
  // El archivo trae extensión .webp, pero es un JPEG: ffmpeg lo detecta por el contenido.
  { origen: 'portada-inicio.webp', destino: 'ambiente/portada-inicio.webp', anchoMaximo: 1920, pesoMaximo: 250 * 1024 },
]

/** marca/: el logo cuadrado (512×512, fondo transparente) da el ícono y los favicons. */
const LOGO = 'logo-samaria360.png'
const MARCA = [
  // Se muestra a 40 px CSS: 80 px para pantallas de densidad 2.
  { destino: 'imagenes/marca/samaria360-icono.webp', lado: 80, formato: 'webp' },
  { destino: 'favicon-32.png', lado: 32, formato: 'png' },
  { destino: 'favicon-16.png', lado: 16, formato: 'png' },
  { destino: 'apple-touch-icon.png', lado: 180, formato: 'png', fondo: COLOR_ARENA },
]

function formatoReal(archivo) {
  return execFileSync('ffprobe', ['-v', 'error', '-select_streams', 'v:0', '-show_entries', 'stream=codec_name',
    '-of', 'csv=p=0', archivo], { encoding: 'utf8' }).trim().split('\n')[0]
}

/** WebP con calidad 82 que baja de 4 en 4 hasta pesar ≤ pesoMaximo, con piso de 60. Devuelve la calidad usada. */
function codificarConLimite(origen, destino, filtro, pesoMaximo, temporal) {
  const prueba = join(temporal, 'prueba.webp')
  let calidad
  for (calidad = CALIDAD_INICIAL; ; calidad = Math.max(CALIDAD_MINIMA, calidad - PASO_CALIDAD)) {
    const peso = codificar(origen, prueba, filtro, calidad)
    if (peso <= pesoMaximo || calidad === CALIDAD_MINIMA) break
  }
  copyFileSync(prueba, destino)
  return calidad
}

function codificar(origen, destino, filtro, calidad) {
  execFileSync('ffmpeg', ['-v', 'error', '-y', '-i', origen, ...(filtro ? ['-vf', filtro] : []), '-frames:v', '1',
    '-map_metadata', '-1',
    '-c:v', 'libwebp', '-preset', 'photo', '-compression_level', '6', '-quality', String(calidad), destino])
  return statSync(destino).size
}

function main() {
  const [entradaArg, ...resto] = process.argv.slice(2)
  if (!entradaArg) {
    console.error('Uso: node scripts/optimizar-imagenes.mjs <ruta a _entrada/imagenes> [--informe archivo.json]')
    process.exit(1)
  }
  const indiceInforme = resto.indexOf('--informe')
  const informeArchivo = indiceInforme >= 0 ? resto[indiceInforme + 1] : null
  const entrada = resolve(entradaArg)
  const carpetaLugares = join(entrada, 'lugares')
  const raizFrontend = resolve(dirname(fileURLToPath(import.meta.url)), '..')
  const publico = join(raizFrontend, 'public')
  const salida = join(publico, 'imagenes', 'catalogo')
  mkdirSync(salida, { recursive: true })
  const temporal = mkdtempSync(join(tmpdir(), 'samaria360-imagenes-'))

  const informe = { lugares: [], ambiente: [], marca: [] }
  try {
    for (const lugar of LUGARES) {
      const origen = join(carpetaLugares, lugar.origen)
      const destino = join(salida, `${lugar.slug}.webp`)
      const original = dimensiones(origen)
      const pesoOriginal = statSync(origen).size
      const corte = recorte(original, lugar.y)
      const reducir = corte.w > ANCHO_MAXIMO
      const final = reducir ? { ancho: ANCHO_MAXIMO, alto: ALTO_MAXIMO } : { ancho: corte.w, alto: corte.h }

      let calidad = null
      let accion
      const yaCumple = lugar.origen.endsWith('.webp') && !reducir && corte.w === original.ancho
        && corte.h === original.alto && pesoOriginal <= PESO_MAXIMO && esWebpSimple(origen)
      if (yaCumple) {
        copyFileSync(origen, destino)
        accion = 'copiada sin recomprimir'
      } else {
        // exact=1: sin él, ffmpeg redondea a par el recorte de las imágenes con croma 4:2:0 y se pierde el 3:2 exacto.
        const filtro = `crop=${corte.w}:${corte.h}:${corte.x}:${corte.y}:exact=1` +
          (reducir ? `,scale=${ANCHO_MAXIMO}:${ALTO_MAXIMO}:flags=lanczos` : '')
        const prueba = join(temporal, `${lugar.slug}.webp`)
        for (calidad = CALIDAD_INICIAL; ; calidad = Math.max(CALIDAD_MINIMA, calidad - PASO_CALIDAD)) {
          const peso = codificar(origen, prueba, filtro, calidad)
          if (peso <= PESO_MAXIMO || calidad === CALIDAD_MINIMA) break
        }
        copyFileSync(prueba, destino)
        accion = reducir ? 'recortada y reducida' : 'recortada (sin ampliar)'
      }
      const peso = statSync(destino).size
      const fila = {
        slug: lugar.slug, origen: lugar.origen, original: `${original.ancho}×${original.alto}`,
        recorte: `${corte.w}×${corte.h}+${corte.x}+${corte.y}`, final: `${final.ancho}×${final.alto}`,
        pesoKB: Math.round(peso / 1024), calidad, accion,
        bajo1200x800: final.ancho < ANCHO_MAXIMO, sobre200KB: peso > PESO_MAXIMO,
      }
      informe.lugares.push(fila)
      console.log(`${fila.slug.padEnd(30)} ${fila.final.padEnd(10)} ${String(fila.pesoKB).padStart(4)} KB  ` +
        `q=${calidad ?? '—'}  ${accion}${fila.sobre200KB ? '  ⚠ > 200 KB con calidad 60' : ''}`)
    }

    for (const foto of AMBIENTE) {
      const origen = join(entrada, 'ambiente', foto.origen)
      const destino = join(publico, 'imagenes', foto.destino)
      mkdirSync(dirname(destino), { recursive: true })
      const original = dimensiones(origen)
      // Sin recorte. Solo se reduce si pasa del ancho máximo (-2: alto par y proporcional); nunca se amplía.
      const filtro = original.ancho > foto.anchoMaximo ? `scale=${foto.anchoMaximo}:-2:flags=lanczos` : null
      const calidad = codificarConLimite(origen, destino, filtro, foto.pesoMaximo, temporal)
      const final = dimensiones(destino)
      const peso = statSync(destino).size
      const fila = {
        destino: foto.destino, origen: foto.origen, formatoReal: formatoReal(origen),
        original: `${original.ancho}×${original.alto}`, final: `${final.ancho}×${final.alto}`,
        pesoKB: Math.round(peso / 1024), calidad, anchoMaximo: foto.anchoMaximo, pesoMaximoKB: foto.pesoMaximo / 1024,
        bajoAnchoMaximo: final.ancho < foto.anchoMaximo, sobrePeso: peso > foto.pesoMaximo,
      }
      informe.ambiente.push(fila)
      console.log(`${fila.destino.padEnd(30)} ${fila.final.padEnd(10)} ${String(fila.pesoKB).padStart(4)} KB  q=${calidad}  ` +
        `(origen ${fila.formatoReal}${fila.bajoAnchoMaximo ? `; por debajo de ${foto.anchoMaximo} px, no se amplía` : ''})`)
    }

    const logo = join(entrada, 'marca', LOGO)
    for (const icono of MARCA) {
      const destino = join(publico, icono.destino)
      mkdirSync(dirname(destino), { recursive: true })
      const escala = `scale=${icono.lado}:${icono.lado}:flags=lanczos`
      const filtros = icono.fondo
        // Opaco: el logo sobre un fondo liso del color de la arena.
        ? ['-f', 'lavfi', '-i', `color=c=${icono.fondo}:s=${icono.lado}x${icono.lado}`, '-filter_complex',
          `[0:v]${escala}[logo];[1:v][logo]overlay=format=auto,format=rgb24`]
        : ['-vf', `${escala},format=rgba`]
      const codec = icono.formato === 'webp' ? ['-c:v', 'libwebp', '-lossless', '1', '-compression_level', '6'] : []
      execFileSync('ffmpeg', ['-v', 'error', '-y', '-i', logo, ...filtros, '-frames:v', '1', '-map_metadata', '-1',
        ...codec, destino])
      const fila = {
        destino: icono.destino, lado: icono.lado, pesoKB: Math.round((statSync(destino).size / 1024) * 10) / 10,
        opaco: Boolean(icono.fondo),
      }
      informe.marca.push(fila)
      console.log(`${fila.destino.padEnd(38)} ${icono.lado}×${icono.lado}  ${fila.pesoKB} KB` +
        (fila.opaco ? '  (opaco, fondo arena)' : ''))
    }
  } finally {
    rmSync(temporal, { recursive: true, force: true })
  }
  if (informeArchivo) writeFileSync(informeArchivo, JSON.stringify(informe, null, 2))
}

main()
