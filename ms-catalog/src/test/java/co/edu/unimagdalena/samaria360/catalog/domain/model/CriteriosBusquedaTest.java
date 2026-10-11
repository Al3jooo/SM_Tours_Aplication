package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.CriteriosBusquedaInvalidosException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CriteriosBusquedaTest {

    private static final Coordenada TURISTA = new Coordenada(-74.2125, 11.2425);

    @Test
    void sinFiltrosEsLaPrimeraPaginaDeTamanoMaximo() {
        CriteriosBusqueda criterios = CriteriosBusqueda.sinFiltros();

        assertThat(criterios.pagina()).isZero();
        assertThat(criterios.tamanoPagina()).isEqualTo(10);
        assertThat(criterios.accesibilidad()).isEmpty();
    }

    @Test
    void unaPaginaDe10ElementosEsValida() {
        assertThatCode(() -> criterios(null, null, null, null, 0, 10)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 11, 50})
    void elTamanoDePaginaDebeEstarEntre1Y10(int tamano) {
        assertThatThrownBy(() -> criterios(null, null, null, null, 0, tamano))
                .isInstanceOf(CriteriosBusquedaInvalidosException.class)
                .hasMessageContaining("RF16");
    }

    @Test
    void laPaginaNoPuedeSerNegativa() {
        assertThatThrownBy(() -> criterios(null, null, null, null, -1, 10))
                .isInstanceOf(CriteriosBusquedaInvalidosException.class);
    }

    @Test
    void laDistanciaMaximaExigeUbicacion() {
        assertThatThrownBy(() -> criterios(null, null, 5.0, null, 0, 10))
                .isInstanceOf(CriteriosBusquedaInvalidosException.class)
                .hasMessageContaining("RF38");
        assertThatCode(() -> criterios(null, TURISTA, 5.0, null, 0, 10)).doesNotThrowAnyException();
    }

    @Test
    void laDistanciaMaximaDebeSerPositiva() {
        assertThatThrownBy(() -> criterios(null, TURISTA, 0.0, null, 0, 10))
                .isInstanceOf(CriteriosBusquedaInvalidosException.class);
    }

    @Test
    void ordenarPorDistanciaExigeUbicacion() {
        assertThatThrownBy(() -> criterios(null, null, null, OrdenBusqueda.Distancia, 0, 10))
                .isInstanceOf(CriteriosBusquedaInvalidosException.class)
                .hasMessageContaining("RF61");
        assertThatCode(() -> criterios(null, TURISTA, null, OrdenBusqueda.Distancia, 0, 10)).doesNotThrowAnyException();
    }

    @Test
    void ordenarPorPrecioNoExigeUbicacion() {
        assertThatCode(() -> criterios(null, null, null, OrdenBusqueda.PrecioAscendente, 0, 10))
                .doesNotThrowAnyException();
    }

    @Test
    void elPrecioMaximoNoPuedeSerNegativo() {
        assertThatThrownBy(() -> criterios(new BigDecimal("-1"), null, null, null, 0, 10))
                .isInstanceOf(CriteriosBusquedaInvalidosException.class);
        assertThatCode(() -> criterios(BigDecimal.ZERO, null, null, null, 0, 10)).doesNotThrowAnyException();
    }

    @Test
    void normalizaElTextoYCopiaLaAccesibilidad() {
        CriteriosBusqueda conEspacios = new CriteriosBusqueda("  café  ", null, null, null, null,
                Set.of(CondicionAccesibilidad.MovilidadReducida), null, 0, 10);
        CriteriosBusqueda enBlanco = new CriteriosBusqueda("   ", null, null, null, null, null, null, 0, 10);

        assertThat(conEspacios.texto()).isEqualTo("café");
        assertThat(conEspacios.accesibilidad()).containsExactly(CondicionAccesibilidad.MovilidadReducida);
        assertThat(enBlanco.texto()).isNull();
        assertThat(enBlanco.accesibilidad()).isEmpty();
    }

    private static CriteriosBusqueda criterios(BigDecimal precioMaximo, Coordenada ubicacion, Double distanciaKm,
                                               OrdenBusqueda orden, int pagina, int tamano) {
        return new CriteriosBusqueda(null, Categoria.Tour, precioMaximo, ubicacion, distanciaKm, Set.of(), orden,
                pagina, tamano);
    }
}
