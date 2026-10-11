/**
 * Iniciales del avatar a partir de nombreVisible: primera y última palabra ("Ana Pérez" → "AP");
 * con una sola palabra, su primera letra ("gestor" → "G").
 */
export function iniciales(nombreVisible: string): string {
  const palabras = nombreVisible.trim().split(/\s+/).filter(Boolean)
  if (palabras.length === 0) {
    return '?'
  }
  const primera = Array.from(palabras[0])[0]
  if (palabras.length === 1) {
    return primera.toLocaleUpperCase()
  }
  const ultima = Array.from(palabras[palabras.length - 1])[0]
  return (primera + ultima).toLocaleUpperCase()
}
