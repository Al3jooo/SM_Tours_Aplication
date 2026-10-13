package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.CriteriosBusquedaInvalidosException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** «Más filtros» (RF10): la condición de cada campo y la comparación de textos (mayúsculas no, tildes sí). */
class FiltrosCategoriaTest {

    private static final DetalleAlojamiento CABANA = new DetalleAlojamiento("Cabaña", 6, 4,
            List.of("WiFi", "Desayuno", "Parqueadero"));
    private static final DetalleGastronomia TIPICA = new DetalleGastronomia("Típica", 40, true, "$20.000");
    private static final DetalleGastronomia SIN_DATO = new DetalleGastronomia("Mariscos", 40, null, "$20.000");
    private static final DetalleTour FACIL = new DetalleTour("Muelle", new BigDecimal("3.5"), "Fácil", 10);
    private static final DetalleTour SIN_NIVEL = new DetalleTour("Muelle", new BigDecimal("2"), null, 10);
    private static final DetalleAlquilerEquipo KAYAK = new DetalleAlquilerEquipo("Kayak doble", 4, null);

    // ------------------------------------------------------------------ Alojamiento

    @Test
    void alojamientoExigeTodosLosServiciosIncluidos() {
        assertThat(alojamiento(Set.of(ServicioIncluido.WiFi, ServicioIncluido.Parqueadero), null, null).cumple(CABANA))
                .isTrue();
        assertThat(alojamiento(Set.of(ServicioIncluido.WiFi, ServicioIncluido.Piscina), null, null).cumple(CABANA))
                .isFalse();
    }

    @ParameterizedTest
    @CsvSource({"1,true", "4,true", "5,false"})
    void alojamientoExigeCapacidadParaLosHuespedes(int huespedes, boolean cumple) {
        assertThat(alojamiento(null, huespedes, null).cumple(CABANA)).isEqualTo(cumple);
    }

    @ParameterizedTest
    @CsvSource({"Cabaña,true", "cabaña,true", "' Cabaña ',true", "Cabana,false", "Caba,false"})
    void elTipoDeHabitacionIgnoraMayusculasPeroNoTildes(String tipo, boolean cumple) {
        assertThat(alojamiento(null, null, tipo).cumple(CABANA)).isEqualTo(cumple);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void losHuespedesDebenSerPositivos(int huespedes) {
        assertThatThrownBy(() -> alojamiento(null, huespedes, null))
                .isInstanceOf(CriteriosBusquedaInvalidosException.class)
                .hasMessage("huespedes debe ser mayor que 0");
    }

    @Test
    void losServiciosSeEnvianEnElOrdenDelVocabulario() {
        FiltrosAlojamiento filtros = alojamiento(Set.of(ServicioIncluido.Parqueadero, ServicioIncluido.WiFi), null, null);

        assertThat(filtros.nombresServiciosIncluidos()).containsExactly("WiFi", "Parqueadero");
        assertThat(filtros.camposActivos()).containsExactly("serviciosIncluidos");
    }

    // ------------------------------------------------------------------ Gastronomía

    @Test
    void soloVegetarianoExigeMenuVegetarianoVerdadero() {
        FiltrosGastronomia vegetariano = new FiltrosGastronomia(true, null);

        assertThat(vegetariano.cumple(TIPICA)).isTrue();
        assertThat(vegetariano.cumple(SIN_DATO)).isFalse();
        assertThat(new FiltrosGastronomia(false, null).cumple(SIN_DATO)).isTrue();
        assertThat(new FiltrosGastronomia(false, null).camposActivos()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"Típica,true", "típica,true", "TÍPICA,true", "Tipica,false"})
    void elTipoDeCocinaIgnoraMayusculasPeroNoTildes(String tipo, boolean cumple) {
        assertThat(new FiltrosGastronomia(false, tipo).cumple(TIPICA)).isEqualTo(cumple);
    }

    // ------------------------------------------------------------------ Tour

    @ParameterizedTest
    @CsvSource({"Fácil,true", "fácil,true", "Facil,false", "Media,false"})
    void elNivelDeDificultadIgnoraMayusculasPeroNoTildes(String nivel, boolean cumple) {
        assertThat(new FiltrosTour(nivel, null).cumple(FACIL)).isEqualTo(cumple);
    }

    @Test
    void unTourSinNivelNoCoincideConUnNivel() {
        assertThat(new FiltrosTour("Fácil", null).cumple(SIN_NIVEL)).isFalse();
        assertThat(new FiltrosTour(null, null).cumple(SIN_NIVEL)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"3.5,true", "3.50,true", "8,true", "3.4,false"})
    void laDuracionMaximaIncluyeElLimite(String maxima, boolean cumple) {
        assertThat(new FiltrosTour(null, new BigDecimal(maxima)).cumple(FACIL)).isEqualTo(cumple);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1"})
    void laDuracionMaximaDebeSerPositiva(String maxima) {
        assertThatThrownBy(() -> new FiltrosTour(null, new BigDecimal(maxima)))
                .isInstanceOf(CriteriosBusquedaInvalidosException.class)
                .hasMessage("duracionMaxHoras debe ser mayor que 0");
    }

    // ------------------------------------------------------------------ Alquiler de equipos

    @Test
    void elTipoDeVehiculoEsElValorCompleto() {
        assertThat(new FiltrosAlquilerEquipo("kayak doble").cumple(KAYAK)).isTrue();
        assertThat(new FiltrosAlquilerEquipo("Kayak").cumple(KAYAK)).isFalse();
    }

    // ------------------------------------------------------------------ comunes

    @Test
    void unFiltroNoCoincideConElDetalleDeOtraCategoria() {
        assertThat(new FiltrosAlquilerEquipo(null).cumple(CABANA)).isFalse();
        assertThat(new FiltrosTour(null, null).cumple(TIPICA)).isFalse();
    }

    @Test
    void unTextoVacioEquivaleASinFiltro() {
        assertThat(new FiltrosAlquilerEquipo("  ").tipoVehiculoEquipo()).isNull();
        assertThat(new FiltrosAlquilerEquipo("  ").camposActivos()).isEmpty();
    }

    @Test
    void unTextoAdmiteHasta100Caracteres() {
        String cien = "a".repeat(100);

        assertThat(new FiltrosGastronomia(false, cien).tipoCocina()).isEqualTo(cien);
        assertThatThrownBy(() -> new FiltrosGastronomia(false, cien + "a"))
                .isInstanceOf(CriteriosBusquedaInvalidosException.class)
                .hasMessage("tipoCocina admite como máximo 100 caracteres");
    }

    private static FiltrosAlojamiento alojamiento(Set<ServicioIncluido> servicios, Integer huespedes, String tipo) {
        return new FiltrosAlojamiento(servicios, huespedes, tipo);
    }
}
