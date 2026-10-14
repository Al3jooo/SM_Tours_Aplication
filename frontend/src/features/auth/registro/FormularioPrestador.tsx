import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { IconoCandado, IconoEscudo, IconoInfo, IconoSobre, IconoTelefono } from '../../../shared/components/Iconos'
import { registrarPrestador, type DatosRegistroPrestador } from '../authApi'
import { Campo } from './Campo'
import { aRechazoRegistro, enfocarPrimerError, textoErrorCampo } from './envioRegistro'
import { CasillaPoliticas } from './FormularioTurista'
import { normalizarCorreo, validarPrestador, type ErroresCampos } from './validacionRegistro'

const ID = (campo: string) => `prestador-${campo}`
const ORDEN = ['nombreRepresentanteLegal', 'rnt', 'correoCorporativo', 'telefono', 'contrasena', 'aceptoPoliticas'] as const

/**
 * WF-03: Formulario – Prestador de Servicios (RF02). Sin razón social: es opcional (decisión D2) y WF-03 no la pide.
 * Los nombres no llevan icono, igual que en el formulario de turista.
 * Al terminar vuelve al login; la cuenta queda Pendiente de Validación y no puede entrar hasta que la validen (D3).
 */
export function FormularioPrestador({ onRegistrado }: { onRegistrado: (correo: string) => void }) {
  const { t } = useTranslation()
  const [datos, setDatos] = useState<DatosRegistroPrestador>({
    nombreRepresentanteLegal: '',
    rnt: '',
    correoCorporativo: '',
    telefono: '',
    contrasena: '',
    aceptoPoliticas: false,
  })
  const [errores, setErrores] = useState<ErroresCampos>({})
  const [general, setGeneral] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  const cambiar = <K extends keyof DatosRegistroPrestador>(campo: K, valor: DatosRegistroPrestador[K]) =>
    setDatos((actual) => ({ ...actual, [campo]: valor }))

  const enviar = async (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault()
    setGeneral(null)
    const encontrados = validarPrestador(datos)
    setErrores(encontrados)
    if (Object.keys(encontrados).length > 0) {
      enfocarPrimerError(ORDEN, encontrados, ID)
      return
    }
    setEnviando(true)
    try {
      const correo = normalizarCorreo(datos.correoCorporativo)
      await registrarPrestador({ ...datos, correoCorporativo: correo })
      onRegistrado(correo)
    } catch (error) {
      const rechazo = aRechazoRegistro(error)
      setErrores(rechazo.campos)
      setGeneral(rechazo.general)
      enfocarPrimerError(ORDEN, rechazo.campos, ID)
      setEnviando(false)
    }
  }

  const error = (campo: string) => textoErrorCampo(t, campo, errores[campo])
  const cantidad = Object.keys(errores).length

  return (
    <form className="tarjeta__cuerpo" noValidate onSubmit={enviar}>
      <div className="registro__resumen" role="alert" aria-atomic="true">
        {cantidad > 0 && <p>{t('registro.resumen', { count: cantidad })}</p>}
        {general && <p>{t([`registro.errorGeneral.${general}`, `errores.${general}`])}</p>}
      </div>

      <Campo id={ID('nombreRepresentanteLegal')} etiqueta={t('registro.nombreRepresentanteLegal')}
        autoComplete="name" placeholder={t('registro.nombreRepresentanteLegalPlaceholder')}
        value={datos.nombreRepresentanteLegal} onChange={(e) => cambiar('nombreRepresentanteLegal', e.target.value)}
        error={error('nombreRepresentanteLegal')} />
      <Campo id={ID('rnt')} etiqueta={t('registro.rnt')} icono={<IconoEscudo />}
        placeholder={t('registro.rntPlaceholder')} value={datos.rnt} onChange={(e) => cambiar('rnt', e.target.value)}
        error={error('rnt')} />
      <Campo id={ID('correoCorporativo')} etiqueta={t('registro.correoCorporativo')} icono={<IconoSobre />}
        type="email" inputMode="email" autoComplete="email" placeholder={t('registro.correoCorporativoPlaceholder')}
        value={datos.correoCorporativo} onChange={(e) => cambiar('correoCorporativo', e.target.value)}
        error={error('correoCorporativo')} />
      <Campo id={ID('telefono')} etiqueta={t('registro.telefono')} icono={<IconoTelefono />} type="tel"
        inputMode="tel" autoComplete="tel" placeholder={t('registro.telefonoPlaceholder')} value={datos.telefono}
        onChange={(e) => cambiar('telefono', e.target.value)} error={error('telefono')} />
      <Campo id={ID('contrasena')} etiqueta={t('registro.crearContrasena')} icono={<IconoCandado />} type="password"
        autoComplete="new-password" ayuda={t('registro.ayudaContrasena')} value={datos.contrasena}
        onChange={(e) => cambiar('contrasena', e.target.value)} error={error('contrasena')} />

      {/* Información obligatoria de RF02: fija, sin botón de cerrar. */}
      <div className="aviso-intentos registro__aviso-pendiente" role="note">
        <IconoInfo />
        <span>{t('registro.avisoPendiente')}</span>
      </div>

      <CasillaPoliticas id={ID('aceptoPoliticas')} marcada={datos.aceptoPoliticas}
        onCambiar={(valor) => cambiar('aceptoPoliticas', valor)} error={error('aceptoPoliticas')} />

      <button type="submit" className="boton-principal" disabled={enviando} aria-busy={enviando}>
        {enviando ? t('registro.enviando') : t('registro.botonPrestador')}
      </button>
    </form>
  )
}
