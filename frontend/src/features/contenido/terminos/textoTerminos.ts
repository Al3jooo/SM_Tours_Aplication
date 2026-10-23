import { ANCLA_PRIVACIDAD } from '../../../shared/rutasLegales'

/**
 * Texto de los términos y condiciones (WF-15, RF46) en ES y EN, con el índice de DEC-68. Vive aquí y no en `es.json` y
 * `en.json` porque es texto largo (DEC-75); `i18n/paridad.test.ts` verifica que los dos idiomas tengan las mismas
 * secciones. Solo describe lo que el prototipo ya hace; lo que el equipo no ha definido se marca como tal.
 */
export interface SeccionTerminos {
  /** Ancla de la sección (`/terminos#<id>`). */
  id: string
  titulo: string
  parrafos: string[]
}

const POR_DEFINIR_ES = 'por definir por el equipo del proyecto'
const POR_DEFINIR_EN = 'to be defined by the project team'

export const TERMINOS: Record<'es' | 'en', SeccionTerminos[]> = {
  es: [
    {
      id: 'objeto',
      titulo: '1. Objeto y aceptación',
      parrafos: [
        'Samaria360 es una plataforma de turismo de Santa Marta para consultar la oferta turística, reservar servicios y recibir información del municipio. Es un prototipo académico del curso de Arquitectura de Software de la Universidad del Magdalena.',
        'Al usar la plataforma o crear una cuenta aceptas estos términos y condiciones.',
      ],
    },
    {
      id: 'uso',
      titulo: '2. Uso permitido y prohibido',
      parrafos: [
        'Puedes consultar la oferta publicada, crear una cuenta y usar las funciones que corresponden a tu rol.',
        'No está permitido suplantar a otra persona, registrar datos falsos, intentar acceder a cuentas ajenas o a funciones de otro rol, ni interferir con el funcionamiento de la plataforma.',
      ],
    },
    {
      id: 'cuentas',
      titulo: '3. Cuentas y roles',
      parrafos: [
        'Turista: se registra desde la plataforma y puede reservar servicios y consultar sus reservas.',
        'Prestador de servicios: se registra con su Registro Nacional de Turismo (RNT). Su cuenta queda pendiente de validación y no puede iniciar sesión hasta que el Administrador la apruebe. Si el Administrador la rechaza, registra una justificación y la decisión no se puede revertir desde el sistema.',
        'Gestor turístico y Administrador: son cuentas que asigna la plataforma; no se registran desde la página.',
        'Desactivación de turistas: el Administrador puede desactivar la cuenta de un turista y reactivarla. Mientras está desactivada no se puede iniciar sesión y las sesiones abiertas se cierran.',
        'Inactivación de prestadores: el Administrador puede inactivar la cuenta de un prestador. Sus servicios dejan de publicarse, no puede iniciar sesión y la cuenta no se reactiva desde el sistema.',
      ],
    },
    {
      id: ANCLA_PRIVACIDAD,
      titulo: '4. Datos personales (Ley 1581 de 2012)',
      parrafos: [
        'Los datos personales se tratan conforme a la Ley 1581 de 2012 de protección de datos personales.',
        'Datos que se guardan del turista: nombres, apellidos, correo electrónico, país de origen y categorías de interés. Del prestador: nombre del representante legal, razón social (opcional), número RNT, correo corporativo y teléfono. De los dos: la aceptación de esta política con su fecha y, en su caso, las reservas (servicio, fechas, cantidades y estado).',
        'La contraseña se guarda protegida mediante una función de resumen criptográfico (no se almacena en texto legible).',
        'Finalidad: crear y administrar tu cuenta, validar a los prestadores y gestionar las reservas.',
        'Como titular de los datos tienes derecho a conocerlos, actualizarlos y rectificarlos; a solicitar prueba de la autorización; a ser informado sobre su uso; a presentar quejas ante la Superintendencia de Industria y Comercio, y a revocar la autorización o solicitar la supresión de los datos.',
        `Responsable del tratamiento: ${POR_DEFINIR_ES}. Canales para ejercer tus derechos: ${POR_DEFINIR_ES}. Plazo de conservación de los datos: ${POR_DEFINIR_ES}.`,
      ],
    },
    {
      id: 'reservas',
      titulo: '5. Reservas y cancelaciones',
      parrafos: [
        'Para confirmar una reserva hay que iniciar sesión como turista.',
        'Una reserva confirmada se puede cancelar hasta 24 horas antes de su inicio; en el límite exacto de 24 horas todavía se puede cancelar.',
        'Cuando termina la actividad o la estadía, la reserva confirmada pasa automáticamente a «Completada».',
      ],
    },
    {
      id: 'terceros',
      titulo: '6. Contenido de terceros e imágenes de referencia',
      parrafos: [
        'La información de cada servicio la publica su prestador.',
        'Las imágenes son de referencia y pueden pertenecer a terceros; no se usan con fines comerciales.',
      ],
    },
    {
      id: 'avisos-faq',
      titulo: '7. Avisos y preguntas frecuentes',
      parrafos: [
        'El Administrador publica avisos generales del municipio (alertas de clima, cierres temporales o comunicados) que aparecen en la parte superior de la plataforma. Puedes cerrarlos durante tu sesión del navegador.',
        'Las preguntas frecuentes resuelven dudas comunes. Los avisos y las preguntas se muestran en el idioma en que se redactaron.',
      ],
    },
    {
      id: 'contacto',
      titulo: '8. Contacto',
      parrafos: [
        'Para consultas sobre la plataforma, comunícate con el equipo del proyecto Samaria360 de la Universidad del Magdalena.',
        `Canales de contacto: ${POR_DEFINIR_ES}.`,
      ],
    },
    {
      id: 'cambios',
      titulo: '9. Cambios a los términos',
      parrafos: [
        'El equipo del proyecto puede actualizar estos términos. La versión vigente es la publicada en esta página.',
      ],
    },
  ],
  en: [
    {
      id: 'objeto',
      titulo: '1. Purpose and acceptance',
      parrafos: [
        'Samaria360 is a Santa Marta tourism platform for browsing the tourism offer, booking services and receiving information from the municipality. It is an academic prototype of the Software Architecture course at Universidad del Magdalena.',
        'By using the platform or creating an account you accept these terms and conditions.',
      ],
    },
    {
      id: 'uso',
      titulo: '2. Permitted and prohibited use',
      parrafos: [
        'You may browse the published offer, create an account and use the features of your role.',
        'You may not impersonate another person, register false data, try to access other people’s accounts or the features of another role, or interfere with the operation of the platform.',
      ],
    },
    {
      id: 'cuentas',
      titulo: '3. Accounts and roles',
      parrafos: [
        'Tourist: signs up on the platform and can book services and view their bookings.',
        'Service provider: signs up with their National Tourism Registry number (RNT). The account remains pending validation and cannot sign in until the Administrator approves it. If the Administrator rejects it, a justification is recorded and the decision cannot be undone from the system.',
        'Tourism manager and Administrator: these accounts are assigned by the platform; they cannot be created from the website.',
        'Deactivation of tourists: the Administrator can deactivate a tourist’s account and reactivate it. While it is deactivated it cannot sign in and open sessions are closed.',
        'Deactivation of providers: the Administrator can deactivate a provider’s account. Its services are no longer published, it cannot sign in and the account cannot be reactivated from the system.',
      ],
    },
    {
      id: ANCLA_PRIVACIDAD,
      titulo: '4. Personal data (Colombian Law 1581 of 2012)',
      parrafos: [
        'Personal data is processed in accordance with Colombian Law 1581 of 2012 on personal data protection.',
        'Data stored for tourists: first names, last names, email, country of origin and categories of interest. For providers: name of the legal representative, company name (optional), RNT number, business email and phone. For both: the acceptance of this policy with its date and, where applicable, their bookings (service, dates, quantities and status).',
        'The password is stored protected by a cryptographic hash function (it is not stored in readable form).',
        'Purpose: creating and managing your account, validating providers and managing bookings.',
        'As the data subject you have the right to know, update and correct your data; to request proof of your authorization; to be informed about its use; to file complaints with the Superintendence of Industry and Commerce; and to revoke your authorization or request the deletion of your data.',
        `Data controller: ${POR_DEFINIR_EN}. Channels to exercise your rights: ${POR_DEFINIR_EN}. Data retention period: ${POR_DEFINIR_EN}.`,
      ],
    },
    {
      id: 'reservas',
      titulo: '5. Bookings and cancellations',
      parrafos: [
        'You must sign in as a tourist to confirm a booking.',
        'A confirmed booking can be cancelled up to 24 hours before it starts; at exactly 24 hours it can still be cancelled.',
        'When the activity or the stay ends, the confirmed booking automatically changes to “Completed”.',
      ],
    },
    {
      id: 'terceros',
      titulo: '6. Third-party content and reference images',
      parrafos: [
        'The information about each service is published by its provider.',
        'Images are for reference only and may belong to third parties; they are not used for commercial purposes.',
      ],
    },
    {
      id: 'avisos-faq',
      titulo: '7. Notices and frequently asked questions',
      parrafos: [
        'The Administrator publishes general notices from the municipality (weather alerts, temporary closures or announcements) that appear at the top of the platform. You can dismiss them for your browser session.',
        'The frequently asked questions answer common doubts. Notices and questions are shown in the language in which they were written.',
      ],
    },
    {
      id: 'contacto',
      titulo: '8. Contact',
      parrafos: [
        'For questions about the platform, contact the Samaria360 project team at Universidad del Magdalena.',
        `Contact channels: ${POR_DEFINIR_EN}.`,
      ],
    },
    {
      id: 'cambios',
      titulo: '9. Changes to the terms',
      parrafos: ['The project team may update these terms. The current version is the one published on this page.'],
    },
  ],
}

/** Secciones del idioma activo (español si el idioma no es inglés). */
export function terminosDe(idioma: string): SeccionTerminos[] {
  return idioma.startsWith('en') ? TERMINOS.en : TERMINOS.es
}
