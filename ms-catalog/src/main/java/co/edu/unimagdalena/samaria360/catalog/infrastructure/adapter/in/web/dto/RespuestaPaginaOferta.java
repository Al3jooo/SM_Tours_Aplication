package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ResultadoOferta;

import java.util.List;

/** Respuesta de GET /api/v1/catalogo/oferta: página de 10 elementos o menos (RF16). */
public record RespuestaPaginaOferta(List<ElementoOferta> elementos, int pagina, int tamano, long totalElementos,
                                    int totalPaginas) {

    public static RespuestaPaginaOferta de(PaginaResultados<ResultadoOferta> pagina) {
        return new RespuestaPaginaOferta(pagina.elementos().stream().map(ElementoOferta::de).toList(),
                pagina.pagina(), pagina.tamano(), pagina.totalElementos(), pagina.totalPaginas());
    }
}
