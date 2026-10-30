package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.booking.domain.model.PaginaReservas;

import java.util.List;

/** Historial del Prestador (RF12, DEC-192): páginas de 20, como «Mis reservas». */
public record RespuestaPaginaReservasPrestador(List<RespuestaReservaPrestador> elementos, int pagina, int tamano,
                                               long totalElementos, int totalPaginas) {

    public static RespuestaPaginaReservasPrestador de(PaginaReservas pagina) {
        return new RespuestaPaginaReservasPrestador(
                pagina.elementos().stream().map(RespuestaReservaPrestador::de).toList(),
                pagina.pagina(), pagina.tamano(), pagina.totalElementos(), pagina.totalPaginas());
    }
}
