package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.booking.domain.model.PaginaReservas;

import java.util.List;

/** «Mis reservas»: páginas de 20, de la más reciente a la más antigua. */
public record RespuestaPaginaReservas(List<RespuestaReserva> elementos, int pagina, int tamano, long totalElementos,
                                      int totalPaginas) {

    public static RespuestaPaginaReservas de(PaginaReservas pagina) {
        return new RespuestaPaginaReservas(pagina.elementos().stream().map(RespuestaReserva::de).toList(),
                pagina.pagina(), pagina.tamano(), pagina.totalElementos(), pagina.totalPaginas());
    }
}
