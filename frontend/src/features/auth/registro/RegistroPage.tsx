import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Navigate, useLocation, useNavigate } from 'react-router-dom'
import { destinoTrasLogin } from '../destinoTrasLogin'
import type { EstadoTrasRegistro } from '../estadoAcceso'
import { PestanasAcceso } from '../PestanasAcceso'
import { useSesion } from '../useSesion'
import { FormularioPrestador } from './FormularioPrestador'
import { FormularioTurista } from './FormularioTurista'
import '../LoginPage.css'
import './RegistroPage.css'

type TipoCuenta = EstadoTrasRegistro['registro']

/**
 * WF-02 y WF-03: CU Registrarse (RF01, RF02). Elección Turista / Prestador de Servicios y su formulario. Al terminar se
 * vuelve al login con un mensaje y el correo escrito: el registro no inicia sesión (ADR-006).
 */
export function RegistroPage() {
  const { t } = useTranslation()
  const { sesion } = useSesion()
  const ubicacion = useLocation()
  const navegar = useNavigate()
  const [tipo, setTipo] = useState<TipoCuenta>('turista')

  if (sesion.estado === 'cargando') {
    return (
      <p className="estado-carga" role="status" aria-busy="true">
        {t('comun.cargando')}
      </p>
    )
  }
  if (sesion.estado === 'autenticado') {
    return <Navigate to={destinoTrasLogin((ubicacion.state as { desde?: unknown } | null)?.desde)} replace />
  }

  const alRegistrar = (correo: string) => {
    const estado: EstadoTrasRegistro = { registro: tipo, correo }
    navegar('/login', { state: estado })
  }

  return (
    <div className="login registro">
      <h1 className="solo-lector">{t('registro.titulo')}</h1>
      <PestanasAcceso activa="registro" />

      <div className="login__cuerpo">
        <fieldset className="registro__tipo">
          <legend className="login__introduccion">{t('registro.introduccion')}</legend>
          {(['turista', 'prestador'] as const).map((opcion) => (
            <label key={opcion} className="casilla">
              <input type="radio" name="tipo-cuenta" checked={tipo === opcion} onChange={() => setTipo(opcion)} />
              {t(`registro.tipo.${opcion}`)}
            </label>
          ))}
        </fieldset>

        <section className="tarjeta" aria-labelledby="titulo-formulario-registro">
          <h2 id="titulo-formulario-registro" className="tarjeta__titulo">
            {t(`registro.formulario.${tipo}`)}
          </h2>
          {tipo === 'turista' ? (
            <FormularioTurista key="turista" onRegistrado={alRegistrar} />
          ) : (
            <FormularioPrestador key="prestador" onRegistrado={alRegistrar} />
          )}
        </section>
      </div>
    </div>
  )
}
