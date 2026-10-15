package co.edu.unimagdalena.samaria360.booking.application.port.in;

import co.edu.unimagdalena.samaria360.booking.domain.model.PaginaReservas;

import java.util.UUID;

/**
 * Las reservas del turista (RF12, de su lado): filtros opcionales por rango de fechas de la actividad y por estado
 * ({@code FiltroReservas}); de la que empieza más tarde a la que empieza más temprano.
 */
public interface ConsultarMisReservasUseCase {

    int TAMANO_PAGINA = 20;

    /**
     * @param desde   fecha de Santa Marta (aaaa-mm-dd), opcional
     * @param hasta   fecha de Santa Marta, opcional; no anterior a {@code desde} y como máximo 366 días
     * @param estados uno o varios de Confirmada, Cancelada y Completada, separados por coma; opcional
     */
    PaginaReservas consultar(UUID idTurista, String desde, String hasta, String estados, int pagina);
}
