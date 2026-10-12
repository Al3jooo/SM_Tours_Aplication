import { useState, type FormEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Navigate, useLocation } from 'react-router-dom'
import { IconoCandado, IconoEntrar, IconoRayo, IconoSobre } from '../../shared/components/Iconos'
import { BotonProximamente } from '../../shared/components/Proximamente'
import { destinoTrasLogin } from './destinoTrasLogin'
import { aErrorInicioSesion, textoError, type ErrorInicioSesion } from './mensajesError'
import { useSesion } from './useSesion'
import './LoginPage.css'

type Campo = 'correo' | 'contrasena'

/** WF-01: Inicio de sesión (RF15). Registrarse y recuperar contraseña quedan visibles pero deshabilitados. */
export function LoginPage() {
  const { t, i18n } = useTranslation()
  const { sesion, iniciarSesion } = useSesion()
  const ubicacion = useLocation()

  // El estado del formulario vive aquí y cambiar de idioma no desmonta el componente (RNF08).
  const [correo, setCorreo] = useState('')
  const [contrasena, setContrasena] = useState('')
  const [camposVacios, setCamposVacios] = useState<Set<Campo>>(new Set())
  const [error, setError] = useState<ErrorInicioSesion | null>(null)
  const [enviando, setEnviando] = useState(false)

  if (sesion.estado === 'cargando') {
    return (
      <p className="estado-carga" role="status" aria-busy="true">
        {t('comun.cargando')}
      </p>
    )
  }
  // Con la sesión iniciada (por URL, con «Atrás» o justo después de iniciar sesión) nunca se muestra el formulario.
  if (sesion.estado === 'autenticado') {
    return <Navigate to={destinoTrasLogin((ubicacion.state as { desde?: unknown } | null)?.desde)} replace />
  }

  const enviar = async (evento: FormEvent<HTMLFormElement>) => {
    evento.preventDefault()
    const vacios = new Set<Campo>()
    if (!correo.trim()) vacios.add('correo')
    if (!contrasena) vacios.add('contrasena')
    setCamposVacios(vacios)
    if (vacios.size > 0) {
      setError(null)
      document.getElementById(vacios.has('correo') ? 'correo' : 'contrasena')?.focus()
      return
    }
    setError(null)
    setEnviando(true)
    try {
      await iniciarSesion(correo.trim(), contrasena)
    } catch (excepcion) {
      setError(aErrorInicioSesion(excepcion))
      setEnviando(false)
    }
  }

  const errorDeCampo = (campo: Campo) =>
    camposVacios.has(campo) ? (
      <p id={`${campo}-error`} className="campo__error">
        {t('login.campoObligatorio')}
      </p>
    ) : null

  return (
    // Disposición de WF-01: título, subtítulo y pestañas arriba a la izquierda; debajo, en una columna centrada, la
    // introducción, la tarjeta del formulario y «Plataforma». Sin imagen.
    <div className="login">
      <div className="login__encabezado">
        <h1 className="login__titulo">{t('login.titulo')}</h1>
        <p className="login__subtitulo">{t('login.subtitulo')}</p>

        <div className="pestanas" role="tablist" aria-label={t('login.pestanas')}>
          <button type="button" role="tab" id="pestana-iniciar" aria-selected="true" aria-controls="panel-iniciar">
            {t('login.pestanaIniciar')}
          </button>
          <BotonProximamente className="pestana-deshabilitada">{t('login.pestanaRegistrarse')}</BotonProximamente>
        </div>
      </div>

      <div className="login__cuerpo" role="tabpanel" id="panel-iniciar" aria-labelledby="pestana-iniciar">
        <p className="login__introduccion">{t('login.introduccion')}</p>

        <section className="tarjeta" aria-labelledby="titulo-tarjeta">
          <h2 id="titulo-tarjeta" className="tarjeta__titulo">
            {t('login.tarjeta')}
          </h2>

          <form className="tarjeta__cuerpo" noValidate onSubmit={enviar}>
            <div className="campo">
              <label htmlFor="correo">{t('login.correo')}</label>
              <div className="campo__control">
                <IconoSobre />
                <input
                  id="correo"
                  name="correo"
                  type="email"
                  inputMode="email"
                  autoComplete="username"
                  placeholder={t('login.correoPlaceholder')}
                  value={correo}
                  onChange={(evento) => setCorreo(evento.target.value)}
                  aria-invalid={camposVacios.has('correo') || undefined}
                  aria-describedby={camposVacios.has('correo') ? 'correo-error' : undefined}
                />
              </div>
              {errorDeCampo('correo')}
            </div>

            <div className="campo">
              <label htmlFor="contrasena">{t('login.contrasena')}</label>
              <div className="campo__control">
                <IconoCandado />
                <input
                  id="contrasena"
                  name="contrasena"
                  type="password"
                  autoComplete="current-password"
                  placeholder={t('login.contrasenaPlaceholder')}
                  value={contrasena}
                  onChange={(evento) => setContrasena(evento.target.value)}
                  aria-invalid={camposVacios.has('contrasena') || undefined}
                  aria-describedby={camposVacios.has('contrasena') ? 'contrasena-error' : undefined}
                />
              </div>
              {errorDeCampo('contrasena')}
            </div>

            <BotonProximamente className="enlace-olvido">{t('login.olvido')}</BotonProximamente>

            <p className="aviso-intentos">
              <IconoRayo />
              <span>{t('login.avisoIntentos')}</span>
            </p>

            {/* Siempre presente para que el lector de pantalla anuncie cada error nuevo. */}
            <div className="login__error" role="alert" aria-live="assertive" aria-atomic="true">
              {error && <p>{textoError(error, t, i18n.language)}</p>}
            </div>

            <button type="submit" className="boton-principal" disabled={enviando} aria-busy={enviando}>
              <IconoEntrar />
              <span>{enviando ? t('login.enviando') : t('login.boton')}</span>
            </button>

            <p className="login__legal">{t('login.legal')}</p>
          </form>
        </section>

        <section className="login__plataforma" aria-labelledby="titulo-plataforma">
          <h2 id="titulo-plataforma">{t('login.plataforma')}</h2>
          <ul>
            <li>
              <BotonProximamente className="enlace">{t('login.explorarDestinos')}</BotonProximamente>
            </li>
            <li>
              <BotonProximamente className="enlace">{t('login.rutasExperiencias')}</BotonProximamente>
            </li>
            <li>
              <BotonProximamente className="enlace">{t('login.prestadores')}</BotonProximamente>
            </li>
          </ul>
        </section>
      </div>
    </div>
  )
}
