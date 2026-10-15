package co.edu.unimagdalena.samaria360.booking.application.port.out;

import java.util.List;

/** Una página de servicios publicados de una categoría (búsqueda pública de ms-catalog): solo los ids. */
public record PaginaCatalogo(List<String> idsServicios, int totalPaginas) {

    public PaginaCatalogo {
        idsServicios = List.copyOf(idsServicios);
    }
}
