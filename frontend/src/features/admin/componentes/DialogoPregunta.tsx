import { useId, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { DatosPregunta } from '../contenidoAdminApi'
import { longitudTexto, MAXIMO_CUERPO, MAXIMO_TITULO, validarTexto } from '../textoAcotado'
import { DialogoAdmin } from './DialogoAdmin'

export type CampoPregunta = 'titulo' | 'cuerpo'

/** Códigos de `campos` del último 400 (DATOS_INVALIDOS). */
export type ErroresPregunta = Partial<Record<CampoPregunta, string>>

const MAXIMOS: Record<CampoPregunta, number> = { titulo: MAXIMO_TITULO, cuerpo: MAXIMO_CUERPO }

/**
 * «Nueva pregunta» / «Editar pregunta» (WF-13, RF26; DEC-73): título y cuerpo obligatorios, con contadores de 150 y
 * 2000 en puntos de código sin los espacios de los extremos (DEC-61). «Guardar» queda deshabilitado (aria-disabled,
 * sigue enfocable) si un campo está vacío o se pasa. Pasarse se marca mientras se escribe; el 400 del servidor se
 * muestra en su campo hasta que se vuelve a escribir en él. El foco inicial va al título.
 */
export function DialogoPregunta({
  inicial,
  enviando,
  errores,
  error,
  onGuardar,
  onCerrar,
}: {
  /** null = nueva pregunta. */
  inicial: DatosPregunta | null
  enviando: boolean
  errores: ErroresPregunta
  /** Código de otro error del último intento (red, 503…). */
  error: string | null
  onGuardar: (datos: DatosPregunta) => void
  onCerrar: () => void
}) {
  const { t } = useTranslation()
  const id = useId()
  const primerCampo = useRef<HTMLInputElement>(null)
  const [datos, setDatos] = useState<DatosPregunta>(inicial ?? { titulo: '', cuerpo: '' })
  // El error del servidor vale hasta que se vuelve a escribir en ese campo.
  const [erroresServidor, setErroresServidor] = useState(errores)
  const [ultimosErrores, setUltimosErrores] = useState(errores)
  if (errores !== ultimosErrores) {
    setUltimosErrores(errores)
    setErroresServidor(errores)
  }

  const invalidos: Record<CampoPregunta, string | null> = {
    titulo: validarTexto(datos.titulo, MAXIMO_TITULO),
    cuerpo: validarTexto(datos.cuerpo, MAXIMO_CUERPO),
  }
  const bloqueado = enviando || invalidos.titulo !== null || invalidos.cuerpo !== null
  const errorDe = (campo: CampoPregunta) =>
    erroresServidor[campo] ?? (invalidos[campo] === 'LONGITUD_MAXIMA' ? invalidos[campo] : null)

  const control = (campo: CampoPregunta) => {
    const errorCampo = errorDe(campo)
    const longitud = longitudTexto(datos[campo])
    const idCampo = `${id}-${campo}`
    const propiedades = {
      id: idCampo,
      value: datos[campo],
      'aria-invalid': errorCampo ? true : undefined,
      'aria-describedby': [`${idCampo}-contador`, errorCampo ? `${idCampo}-error` : null].filter(Boolean).join(' '),
      onChange: (evento: { target: { value: string } }) => {
        setDatos((actuales) => ({ ...actuales, [campo]: evento.target.value }))
        setErroresServidor((actuales) => ({ ...actuales, [campo]: undefined }))
      },
    }
    return (
      <div className="campo">
        <label htmlFor={idCampo}>{t(`admin.preguntas.formulario.${campo}`)}</label>
        {campo === 'titulo' ? (
          <div className="campo__control">
            <input ref={primerCampo} type="text" {...propiedades} />
          </div>
        ) : (
          <textarea className="dialogo-cuentas__texto" rows={7} {...propiedades} />
        )}
        <p
          id={`${idCampo}-contador`}
          className={`dialogo-cuentas__contador${longitud > MAXIMOS[campo] ? ' dialogo-cuentas__contador--excedido' : ''}`}
        >
          {t('admin.contenido.contador', { n: longitud, maximo: MAXIMOS[campo] })}
        </p>
        {errorCampo && (
          <p id={`${idCampo}-error`} className="campo__error" role="alert">
            {t(`admin.contenido.errores.${errorCampo}`, {
              maximo: MAXIMOS[campo],
              defaultValue: t('admin.contenido.errores.REQUERIDO'),
            })}
          </p>
        )}
      </div>
    )
  }

  return (
    <DialogoAdmin
      titulo={t(inicial ? 'admin.preguntas.formulario.tituloEditar' : 'admin.preguntas.formulario.tituloNueva')}
      bloqueado={enviando}
      focoInicial={primerCampo}
      onCerrar={onCerrar}
    >
      {(idTexto) => (
        <form
          className="dialogo-contenido"
          noValidate
          onSubmit={(evento) => {
            evento.preventDefault()
            if (!bloqueado) onGuardar(datos)
          }}
        >
          <p id={idTexto}>{t('admin.preguntas.formulario.ayuda')}</p>
          {control('titulo')}
          {control('cuerpo')}
          {error && (
            <p className="dialogo__error" role="alert">
              {t(`admin.errores.${error}`, { defaultValue: t('admin.errores.GENERICO') })}
            </p>
          )}
          <div className="dialogo__acciones">
            <button
              type="button"
              className="boton-secundario"
              onClick={() => {
                if (!enviando) onCerrar()
              }}
            >
              {t('admin.dialogo.cancelar')}
            </button>
            <button
              type="submit"
              className={`boton-principal${bloqueado ? ' deshabilitado' : ''}`}
              aria-disabled={bloqueado ? true : undefined}
              aria-busy={enviando ? true : undefined}
            >
              {enviando ? t('admin.dialogo.enviando') : t('admin.preguntas.formulario.guardar')}
            </button>
          </div>
        </form>
      )}
    </DialogoAdmin>
  )
}
