package co.edu.unimagdalena.samaria360.auth.application.port.in;

/**
 * Listado de Gestores y Administradores (RF45), para modificar o revocar su rol. {@code rol} y {@code texto} pueden ser
 * nulos; otro rol o un texto de más de 100 caracteres es un 400 DATOS_INVALIDOS.
 */
public interface ConsultarPersonalUseCase {

    PaginaCuentas<PersonalResumen> personal(String rol, String texto, int pagina);
}
