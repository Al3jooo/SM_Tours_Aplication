import { Component, type ReactNode } from 'react'

/**
 * Límite de errores de una región (p. ej. una ruta cargada de forma diferida): si algo dentro falla al renderizar o
 * no se puede descargar, se muestra `alFallar` en su lugar y el resto de la app (encabezado, pie, sesión) sigue.
 * React solo ofrece esta API en componentes de clase.
 */
export class LimiteErrores extends Component<{ alFallar: ReactNode; children: ReactNode }, { fallo: boolean }> {
  state = { fallo: false }

  static getDerivedStateFromError() {
    return { fallo: true }
  }

  render() {
    return this.state.fallo ? this.props.alFallar : this.props.children
  }
}
