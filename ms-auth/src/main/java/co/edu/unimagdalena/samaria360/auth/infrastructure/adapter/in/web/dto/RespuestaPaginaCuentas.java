package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.auth.application.port.in.PaginaCuentas;

import java.util.List;

/** Página de un listado del Administrador, con la misma forma que «Mis reservas»: {@code pagina} desde 0. */
public record RespuestaPaginaCuentas<T>(List<T> elementos, int pagina, int tamano, long totalElementos,
                                        int totalPaginas) {

    public static <T> RespuestaPaginaCuentas<T> de(PaginaCuentas<T> pagina) {
        return new RespuestaPaginaCuentas<>(pagina.elementos(), pagina.pagina(), pagina.tamano(),
                pagina.totalElementos(), pagina.totalPaginas());
    }
}
