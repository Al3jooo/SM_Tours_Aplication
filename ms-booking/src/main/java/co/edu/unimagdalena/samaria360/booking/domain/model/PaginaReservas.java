package co.edu.unimagdalena.samaria360.booking.domain.model;

import java.util.List;

/** Una página de las reservas de un turista, de la más reciente a la más antigua. */
public record PaginaReservas(List<Reserva> elementos, int pagina, int tamano, long totalElementos) {

    public PaginaReservas {
        elementos = List.copyOf(elementos);
    }

    public int totalPaginas() {
        return (int) ((totalElementos + tamano - 1) / tamano);
    }
}
