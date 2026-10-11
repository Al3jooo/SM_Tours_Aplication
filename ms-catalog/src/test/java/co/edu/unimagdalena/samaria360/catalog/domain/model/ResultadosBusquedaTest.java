package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.CriteriosBusquedaInvalidosException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/** Adiciones al dominio de la parte 2: distancia, texto máximo, resultado y página. */
class ResultadosBusquedaTest {

    private static final Coordenada PARQUE_DE_LOS_NOVIOS = new Coordenada(-74.2117, 11.2420);
    private static final Coordenada MINCA = new Coordenada(-74.1185, 11.1430);

    @Test
    void laDistanciaEsLaDeLaLineaRectaEnKm() {
        assertThat(PARQUE_DE_LOS_NOVIOS.distanciaKm(PARQUE_DE_LOS_NOVIOS)).isZero();
        // Santa Marta – Minca: ~15 km en línea recta.
        assertThat(PARQUE_DE_LOS_NOVIOS.distanciaKm(MINCA)).isCloseTo(14.9, within(0.5));
        assertThat(MINCA.distanciaKm(PARQUE_DE_LOS_NOVIOS)).isEqualTo(PARQUE_DE_LOS_NOVIOS.distanciaKm(MINCA));
    }

    @Test
    void elTextoDeBusquedaAdmiteHasta100Caracteres() {
        String cien = "a".repeat(CriteriosBusqueda.LONGITUD_MAXIMA_TEXTO);

        assertThatCode(() -> new CriteriosBusqueda(cien, null, null, null, null, null, null, 0, 10))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> new CriteriosBusqueda(cien + "a", null, null, null, null, null, null, 0, 10))
                .isInstanceOf(CriteriosBusquedaInvalidosException.class)
                .hasMessageContaining("100");
        // Se mide después del trim.
        assertThatCode(() -> new CriteriosBusqueda("  " + cien + "  ", null, null, null, null, null, null, 0, 10))
                .doesNotThrowAnyException();
    }

    @Test
    void conOrdenConservaLosDemasCriterios() {
        CriteriosBusqueda criterios = new CriteriosBusqueda("café", Categoria.Tour, BigDecimal.TEN, MINCA, 5.0,
                Set.of(CondicionAccesibilidad.Visual), null, 2, 5);

        CriteriosBusqueda ordenados = criterios.conOrden(OrdenBusqueda.PrecioAscendente);

        assertThat(ordenados.orden()).isEqualTo(OrdenBusqueda.PrecioAscendente);
        assertThat(ordenados).usingRecursiveComparison().ignoringFields("orden").isEqualTo(criterios);
    }

    @Test
    void laDescripcionCortaTieneComoMaximo140CaracteresYCortaEnPalabra() {
        String larga = "palabra ".repeat(40).trim();

        String corta = ResultadoOferta.acortar(larga);

        assertThat(corta).hasSizeLessThanOrEqualTo(140).endsWith("…").doesNotContain("palab…");
        assertThat(ResultadoOferta.acortar("Breve")).isEqualTo("Breve");
        assertThat(ResultadoOferta.acortar(null)).isNull();
    }

    @Test
    void unAtractivoNoTienePrecioYUnServicioNoTieneEstadoOperativo() {
        assertThatThrownBy(() -> new ResultadoOferta(TipoOferta.Atractivo, "a1", "Cascada", "Naturaleza", null,
                BigDecimal.ONE, null, MINCA, null, Set.of(), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResultadoOferta(TipoOferta.Servicio, "s1", "Hotel", "Alojamiento", null,
                BigDecimal.ONE, null, MINCA, null, Set.of(), EstadoOperativo.Abierto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elTotalDePaginasRedondeaHaciaArriba() {
        assertThat(new PaginaResultados<>(List.of(), 0, 10, 0).totalPaginas()).isZero();
        assertThat(new PaginaResultados<>(List.of(), 0, 10, 10).totalPaginas()).isEqualTo(1);
        assertThat(new PaginaResultados<>(List.of(), 0, 4, 18).totalPaginas()).isEqualTo(5);
    }
}
