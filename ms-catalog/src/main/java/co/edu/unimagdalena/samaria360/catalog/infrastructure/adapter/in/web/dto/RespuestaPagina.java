package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;

import java.util.List;
import java.util.function.Function;

/** Página de un listado del Administrador, con la misma forma que en ms-auth: {@code pagina} desde 0 (DEC-41). */
public record RespuestaPagina<T>(List<T> elementos, int pagina, int tamano, long totalElementos, int totalPaginas) {

    public static <D, T> RespuestaPagina<T> de(PaginaResultados<D> pagina, Function<D, T> conversion) {
        return new RespuestaPagina<>(pagina.elementos().stream().map(conversion).toList(), pagina.pagina(),
                pagina.tamano(), pagina.totalElementos(), pagina.totalPaginas());
    }
}
