package co.edu.unimagdalena.samaria360.booking.application.port.in;

import co.edu.unimagdalena.samaria360.booking.domain.model.PaginaReservas;

import java.util.UUID;

/**
 * CU «Consultar historial» del lado del Prestador (RF12; DEC-191, DEC-192): las reservas de sus servicios, con los
 * mismos filtros que «Mis reservas» más el servicio, y el mismo orden y tamaño de página
 * ({@link ConsultarMisReservasUseCase#TAMANO_PAGINA}). Sin datos del Turista.
 */
public interface ConsultarReservasPrestadorUseCase {

    /**
     * @param desde    fecha de Santa Marta (aaaa-mm-dd), opcional
     * @param hasta    fecha de Santa Marta, opcional; no anterior a {@code desde} y como máximo 366 días
     * @param estados  uno o varios de Confirmada, Cancelada y Completada, separados por coma; opcional
     * @param servicio un servicio del Prestador (ObjectId en texto); opcional
     */
    PaginaReservas consultarDelPrestador(UUID idPrestador, String desde, String hasta, String estados, String servicio,
                                         int pagina);
}
