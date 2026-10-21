package co.edu.unimagdalena.samaria360.auth.application.port.in;

import java.util.List;

/** Página de un listado del Administrador; {@code pagina} cuenta desde 0, como «Mis reservas». */
public record PaginaCuentas<T>(List<T> elementos, int pagina, int tamano, long totalElementos) {

    public PaginaCuentas {
        elementos = List.copyOf(elementos);
    }

    public int totalPaginas() {
        return (int) ((totalElementos + tamano - 1) / tamano);
    }
}
