import i18n from 'i18next'
import { describe, expect, it } from 'vitest'
import { erroresAnidados, idDeCampo, rutaSinIndices, textoErrorServicio } from '../rutasCampos'

describe('Rutas de los campos del registro de servicios (DEC-166)', () => {
  it('la ruta del contrato da el id del control', () => {
    expect(idDeCampo('detalleCategoria.unidades[2].precioNoche')).toBe('servicio-detalleCategoria-unidades-2-precioNoche')
    expect(idDeCampo('oferta.salidas[1]')).toBe('servicio-oferta-salidas-1')
    expect(idDeCampo('contacto.correo')).toBe('servicio-contacto-correo')
    expect(rutaSinIndices('detalleCategoria.unidades[12].nombre')).toBe('detalleCategoria.unidades.nombre')
  })

  it('solo se aceptan los motivos conocidos: nunca un texto del servidor', () => {
    expect(
      erroresAnidados({
        'contacto.correo': 'FORMATO',
        'oferta.salidas[1]': 'DUPLICADO',
        nombreComercial: '<b>texto libre</b>',
        precioBase: 7,
      }),
    ).toEqual({ 'contacto.correo': 'FORMATO', 'oferta.salidas[1]': 'DUPLICADO' })
    expect(erroresAnidados(null)).toEqual({})
    expect(erroresAnidados('DATOS_INVALIDOS')).toEqual({})
  })

  it('el texto de LONGITUD_MAXIMA lleva el máximo de la ruta, también con índice', () => {
    const t = i18n.getFixedT('es')
    expect(textoErrorServicio(t, 'detalleCategoria.unidades[3].nombre', 'LONGITUD_MAXIMA')).toBe(
      'Usa como máximo 100 caracteres.',
    )
    expect(textoErrorServicio(t, 'descripcion', 'LONGITUD_MAXIMA')).toBe('Usa como máximo 2000 caracteres.')
    expect(textoErrorServicio(t, 'detalleCategoria.aforoMaxGrupo', 'VALOR_MAXIMO')).toBe('El valor es mayor que el permitido.')
    expect(textoErrorServicio(t, 'descripcion', undefined)).toBeUndefined()
  })
})
