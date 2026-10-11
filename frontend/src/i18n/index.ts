import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import es from './es.json'
import en from './en.json'

export const IDIOMAS = ['es', 'en'] as const
export type Idioma = (typeof IDIOMAS)[number]

const CLAVE_IDIOMA = 'samaria360.idioma'

function idiomaGuardado(): Idioma {
  try {
    const guardado = localStorage.getItem(CLAVE_IDIOMA)
    return guardado === 'en' ? 'en' : 'es'
  } catch {
    return 'es'
  }
}

/** Cambia el idioma sin recargar: los componentes conservan su estado (RNF08). */
export async function cambiarIdioma(idioma: Idioma): Promise<void> {
  await i18n.changeLanguage(idioma)
  try {
    localStorage.setItem(CLAVE_IDIOMA, idioma)
  } catch {
    // Preferencia no persistida; el cambio aplica igual.
  }
}

i18n.on('languageChanged', (idioma) => {
  document.documentElement.lang = idioma
})

void i18n.use(initReactI18next).init({
  resources: { es: { translation: es }, en: { translation: en } },
  lng: idiomaGuardado(),
  fallbackLng: 'es',
  supportedLngs: IDIOMAS,
  interpolation: { escapeValue: false },
})

document.documentElement.lang = i18n.language

export default i18n
