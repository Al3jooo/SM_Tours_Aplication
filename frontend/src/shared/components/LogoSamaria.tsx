/**
 * Ícono del logo de Samaria360 (sol, faro, isla y olas, con fondo transparente). Lo genera
 * scripts/optimizar-imagenes.mjs a partir de _entrada/imagenes/marca/logo-samaria360.png, a 80×80 para densidad 2.
 * Es decorativo (alt vacío): el nombre «Samaria360» va como texto junto al ícono. La ilustración propia
 * MarcaSamaria (Ilustraciones.tsx) queda como respaldo.
 */
export function LogoSamaria({ tamano = 40 }: { tamano?: number }) {
  return (
    <img
      className="logo-samaria"
      src="/imagenes/marca/samaria360-icono.webp"
      alt=""
      width={tamano}
      height={tamano}
      decoding="async"
    />
  )
}
