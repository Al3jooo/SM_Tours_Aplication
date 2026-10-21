package co.edu.unimagdalena.samaria360.auth.application.port.in;

import java.util.UUID;

/**
 * Listados y detalle del Administrador (RF28). {@code estado} y {@code texto} pueden ser nulos; un estado que no
 * corresponde al rol o un texto de más de 100 caracteres es un 400 DATOS_INVALIDOS.
 */
public interface ConsultarCuentasUseCase {

    PaginaCuentas<PrestadorResumen> prestadores(String estado, String texto, int pagina);

    PrestadorDetalle prestador(UUID idPrestador);

    PaginaCuentas<TuristaResumen> turistas(String estado, String texto, int pagina);
}
