package co.edu.unimagdalena.samaria360.booking.application.port.in;

import co.edu.unimagdalena.samaria360.booking.domain.model.PaginaReservas;

import java.util.UUID;

/** Las reservas del turista, sin filtros (los filtros son RF12, fuera de alcance). */
public interface ConsultarMisReservasUseCase {

    int TAMANO_PAGINA = 20;

    PaginaReservas consultar(UUID idTurista, int pagina);
}
