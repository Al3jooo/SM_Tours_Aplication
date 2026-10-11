package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.util.List;

/** Una página de resultados (RF16) con el total exacto de coincidencias. */
public record PaginaResultados<T>(List<T> elementos, int pagina, int tamano, long totalElementos) {

    public PaginaResultados {
        elementos = List.copyOf(elementos);
        if (pagina < 0 || tamano < 1 || totalElementos < 0) {
            throw new IllegalArgumentException("Página inválida");
        }
    }

    public int totalPaginas() {
        return (int) ((totalElementos + tamano - 1) / tamano);
    }
}
