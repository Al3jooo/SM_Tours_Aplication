import { useEffect, useId, useRef } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { BotonNoDisponible } from '../../shared/components/BotonNoDisponible'
import { IconoCompletada, IconoInfo } from '../../shared/components/Iconos'
import { ImagenOferta } from '../catalogo/componentes/ImagenOferta'
import { etiquetaCategoria } from '../catalogo/componentes/etiquetas'
import { rutaDetalle } from '../catalogo/detalle/navegacionDetalle'
import { AvisoFavoritos } from '../favoritos/AvisoFavoritos'
import { claveFavorito } from '../favoritos/FavoritosContext'
import { LIMITE_FAVORITOS, type Favorito } from '../favoritos/favoritosApi'
import { useFavoritos } from '../favoritos/useFavoritos'
import './perfil.css'

/**
 * WF-17 «Mis favoritos» (RF54), dentro del perfil y solo para el Turista (DEC-92). Usa la lista compartida de la sesión
 * (DEC-93): lo que se quita aquí también se desmarca en los corazones de WF-05 y WF-06. Del más reciente al más antiguo,
 * con «n de 100» (DEC-86). «Ver detalle» solo en los servicios disponibles; en un atractivo queda deshabilitado hasta el
 * detalle de atractivo de la iteración E (DEC-87); lo no disponible no tiene enlace. «Quitar» no pide confirmación
 * (DEC-99) y el foco pasa al «Quitar» siguiente o, si no hay, al título.
 */
export function MisFavoritosPage() {
  const { t } = useTranslation()
  const id = useId()
  const favoritos = useFavoritos()
  const titulo = useRef<HTMLHeadingElement>(null)
  const botones = useRef(new Map<string, HTMLButtonElement>())
  const enfocar = useRef<string | null>(null)

  // Después de quitar, el foco va al botón elegido (o al título) cuando la lista ya se actualizó.
  useEffect(() => {
    if (enfocar.current === null) return
    const destino = enfocar.current
    enfocar.current = null
    ;(destino ? botones.current.get(destino) : titulo.current)?.focus()
  })

  if (!favoritos) return null
  const { estado, elementos } = favoritos

  const quitar = (favorito: Favorito, indice: number) => {
    const siguiente = elementos[indice + 1] ?? elementos[indice - 1]
    enfocar.current = siguiente ? claveFavorito(siguiente.tipo, siguiente.id) : ''
    void favoritos.quitar(favorito.tipo, favorito.id)
  }

  return (
    <div className="mis-favoritos">
      <p className="perfil__volver">
        <Link to="/perfil">
          <span aria-hidden="true">← </span>
          {t('favoritos.volverPerfil')}
        </Link>
      </p>
      <h1 ref={titulo} tabIndex={-1}>
        {t('favoritos.titulo')}
      </h1>
      {estado === 'ok' && elementos.length > 0 && (
        <p className="mis-favoritos__contador">
          {t('favoritos.contador', { n: elementos.length, limite: LIMITE_FAVORITOS })}
          <span aria-hidden="true"> · </span>
          {t('favoritos.orden')}
        </p>
      )}
      <AvisoFavoritos />

      {(estado === 'cargando' || estado === 'sinCargar') && (
        <p className="estado-carga" role="status" aria-busy="true">
          {t('favoritos.cargando')}
        </p>
      )}
      {estado === 'error' && (
        <div className="estado-catalogo estado-catalogo--error" role="alert">
          <p className="estado-catalogo__mensaje">{t('favoritos.errorCarga')}</p>
          <button type="button" className="boton-secundario" onClick={favoritos.asegurarCarga}>
            {t('catalogo.busqueda.reintentar')}
          </button>
        </div>
      )}
      {estado === 'ok' &&
        (elementos.length === 0 ? (
          <div className="mis-favoritos__vacio">
            <p className="estado-catalogo__mensaje">{t('favoritos.vacio')}</p>
            <p className="estado-catalogo__ayuda">{t('favoritos.vacioAyuda')}</p>
            <Link className="boton-principal" to="/explorar">
              {t('favoritos.explorar')}
            </Link>
          </div>
        ) : (
          <ul className="mis-favoritos__lista">
            {elementos.map((favorito, indice) => {
              const clave = claveFavorito(favorito.tipo, favorito.id)
              const idTitulo = `${id}-${indice}`
              const nombre = favorito.nombre ?? t('favoritos.eliminado')
              return (
                <li key={clave}>
                  <article className="tarjeta-favorito" aria-labelledby={idTitulo}>
                    <ImagenOferta resultado={favorito} />
                    <div className="tarjeta-favorito__cuerpo">
                      <h2 id={idTitulo} className="tarjeta-favorito__nombre">
                        {nombre}
                      </h2>
                      <p className="tarjeta-favorito__categoria">
                        {favorito.tipo === 'Atractivo' && t('catalogo.tipos.Atractivo')}
                        {favorito.tipo === 'Atractivo' && favorito.categoria && ' · '}
                        {etiquetaCategoria(t, favorito.categoria)}
                      </p>
                      <p
                        className={`insignia-favorito insignia-favorito--${favorito.disponible ? 'disponible' : 'no-disponible'}`}
                      >
                        {favorito.disponible ? <IconoCompletada /> : <IconoInfo />}
                        {t(favorito.disponible ? 'favoritos.disponible' : 'favoritos.noDisponible')}
                      </p>
                      {!favorito.disponible && <p className="tarjeta-favorito__ayuda">{t('favoritos.noDisponibleAyuda')}</p>}
                    </div>
                    <div className="tarjeta-favorito__acciones">
                      {favorito.disponible &&
                        (favorito.tipo === 'Servicio' ? (
                          <Link className="boton-secundario" to={rutaDetalle(favorito.id)} aria-describedby={idTitulo}>
                            {t('catalogo.tarjeta.verDetalle')}
                          </Link>
                        ) : (
                          <BotonNoDisponible>{t('catalogo.tarjeta.verDetalle')}</BotonNoDisponible>
                        ))}
                      <button
                        ref={(elemento) => {
                          if (elemento) botones.current.set(clave, elemento)
                          else botones.current.delete(clave)
                        }}
                        type="button"
                        className="boton-secundario"
                        aria-label={t('favoritos.quitar', { nombre })}
                        aria-disabled={favoritos.pendiente(favorito.tipo, favorito.id) || undefined}
                        onClick={() => {
                          if (!favoritos.pendiente(favorito.tipo, favorito.id)) quitar(favorito, indice)
                        }}
                      >
                        {t('favoritos.quitarVisible')}
                      </button>
                    </div>
                  </article>
                </li>
              )
            })}
          </ul>
        ))}
    </div>
  )
}
