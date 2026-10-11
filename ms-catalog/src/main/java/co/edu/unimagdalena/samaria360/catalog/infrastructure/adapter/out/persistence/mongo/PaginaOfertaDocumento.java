package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.out.persistence.mongo;

import java.util.List;

/** Salida del {@code $facet}: la página pedida y el total exacto de coincidencias. */
public record PaginaOfertaDocumento(List<ResultadoOfertaDocumento> elementos, List<Total> total) {

    public record Total(long n) {
    }

    public long totalElementos() {
        return total == null || total.isEmpty() ? 0 : total.get(0).n();
    }
}
