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

    private static final DetalleAlojamiento CABANA = Unidades.alojamiento("Cabaña", 6, 4, "180000",
            List.of("WiFi", "Desayuno", "Parqueadero"));
    /** D1: la misma unidad debe cumplir huéspedes, tipo y precio máximo. */
    private static final DetalleAlojamiento HOTEL = new DetalleAlojamiento(List.of(
            Unidades.unidad("Doble", 12, 2, "150000"),
            Unidades.unidad("Suite familiar", "Suite", 3, 6, "260000")), List.of("WiFi"));
    private static final DetalleGastronomia TIPICA = new DetalleGastronomia("Típica", 40, true, "$20.000");
    private static final DetalleGastronomia SIN_DATO = new DetalleGastronomia("Mariscos", 40, null, "$20.000");
    private static final DetalleTour FACIL = new DetalleTour("Muelle", new BigDecimal("3.5"), "Fácil", 10);
    private static final DetalleTour SIN_NIVEL = new DetalleTour("Muelle", new BigDecimal("2"), null, 10);
    private static final DetalleAlquilerEquipo KAYAK = new DetalleAlquilerEquipo("Kayak doble", 4, null);

    // ------------------------------------------------------------------ Alojamiento

    @Test
    void alojamientoExigeTodosLosServiciosIncluidos() {
        assertThat(alojamiento(Set.of(ServicioIncluido.WiFi, ServicioIncluido.Parqueadero), null, null).cumple(CABANA, null))
                .isTrue();
        assertThat(alojamiento(Set.of(ServicioIncluido.WiFi, ServicioIncluido.Piscina), null, null).cumple(CABANA, null))
                .isFalse();
    }

    @ParameterizedTest
    @CsvSource({"1,true", "4,true", "5,false"})
    void alojamientoExigeCapacidadParaLosHuespedes(int huespedes, boolean cumple) {
        assertThat(alojamiento(null, huespedes, null).cumple(CABANA, null)).isEqualTo(cumple);
    }

    @ParameterizedTest
    @CsvSource({"Cabaña,true", "cabaña,true", "' Cabaña ',true", "Cabana,false", "Caba,false"})
    void elTipoDeHabitacionIgnoraMayusculasPeroNoTildes(String tipo, boolean cumple) {
        assertThat(alojamiento(null, null, tipo).cumple(CABANA, null)).isEqualTo(cumple);
    }

    @ParameterizedTest
    @CsvSource({
            "4,,true",          // la Suite alcanza
            "4,300000,true",
            "4,200000,false",   // la Doble es barata pero pequeña; la Suite alcanza pero es cara
            "2,200000,true",    // la Doble cumple las dos condiciones
            ",200000,true",     // solo el precio: lo decide precioBase, no las unidades
            "7,,false"})
    void unaMismaUnidadDebeCumplirHuespedesYPrecioMaximo(Integer huespedes, BigDecimal precioMaximo, boolean cumple) {
        assertThat(alojamiento(null, huespedes, null).cumple(HOTEL, precioMaximo)).isEqualTo(cumple);
    }

    @ParameterizedTest
    @CsvSource({"suite,,true", "Suite,200000,false", "Doble,200000,true", "Triple,,false"})
    void elTipoDeHabitacionPuedeSerDeCualquierUnidad(String tipo, BigDecimal precioMaximo, boolean cumple) {
        assertThat(alojamiento(null, null, tipo).cumple(HOTEL, precioMaximo)).isEqualTo(cumple);
        assertThat(alojamiento(null, 6, tipo).cumple(HOTEL, precioMaximo)).isEqualTo(cumple && !"Doble".equals(tipo));
    }

    @Test
    void elPrecioDesdeEsElDeLasUnidadesQueCumplen() {
        // HOTEL: Doble (2 huéspedes, 150.000) y Suite familiar (6 huéspedes, 260.000).
        assertThat(alojamiento(null, 3, null).precioDesde(HOTEL, null)).isEqualByComparingTo("260000");
        assertThat(alojamiento(null, 2, null).precioDesde(HOTEL, null)).isEqualByComparingTo("150000");
        assertThat(alojamiento(null, null, "Suite").precioDesde(HOTEL, null)).isEqualByComparingTo("260000");
        assertThat(alojamiento(null, 3, null).precioDesde(HOTEL, new BigDecimal("200000"))).isNull();
        // Sin filtros de unidad, el precio «desde» del alojamiento.
        assertThat(alojamiento(Set.of(ServicioIncluido.WiFi), null, null).precioDesde(HOTEL, null))
                .isEqualByComparingTo("150000");
    }

    @Test
    void sinFiltrosDeUnidadNoSeMiranLasUnidades() {
        FiltrosAlojamiento soloServicios = alojamiento(Set.of(ServicioIncluido.WiFi), null, null);

        assertThat(soloServicios.filtraUnidades()).isFalse();
        assertThat(soloServicios.cumple(HOTEL, new BigDecimal("1"))).isTrue();
        assertThat(alojamiento(null, 1, null).filtraUnidades()).isTrue();
        assertThat(alojamiento(null, null, "Doble").filtraUnidades()).isTrue();
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

        assertThat(vegetariano.cumple(TIPICA, null)).isTrue();
        assertThat(vegetariano.cumple(SIN_DATO, null)).isFalse();
        assertThat(new FiltrosGastronomia(false, null).cumple(SIN_DATO, null)).isTrue();
        assertThat(new FiltrosGastronomia(false, null).camposActivos()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"Típica,true", "típica,true", "TÍPICA,true", "Tipica,false"})
    void elTipoDeCocinaIgnoraMayusculasPeroNoTildes(String tipo, boolean cumple) {
        assertThat(new FiltrosGastronomia(false, tipo).cumple(TIPICA, null)).isEqualTo(cumple);
    }

    // ------------------------------------------------------------------ Tour

    @ParameterizedTest
    @CsvSource({"Fácil,true", "fácil,true", "Facil,false", "Media,false"})
    void elNivelDeDificultadIgnoraMayusculasPeroNoTildes(String nivel, boolean cumple) {
        assertThat(new FiltrosTour(nivel, null).cumple(FACIL, null)).isEqualTo(cumple);
    }

    @Test
    void unTourSinNivelNoCoincideConUnNivel() {
        assertThat(new FiltrosTour("Fácil", null).cumple(SIN_NIVEL, null)).isFalse();
        assertThat(new FiltrosTour(null, null).cumple(SIN_NIVEL, null)).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"3.5,true", "3.50,true", "8,true", "3.4,false"})
    void laDuracionMaximaIncluyeElLimite(String maxima, boolean cumple) {
        assertThat(new FiltrosTour(null, new BigDecimal(maxima)).cumple(FACIL, null)).isEqualTo(cumple);
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
        assertThat(new FiltrosAlquilerEquipo("kayak doble").cumple(KAYAK, null)).isTrue();
        assertThat(new FiltrosAlquilerEquipo("Kayak").cumple(KAYAK, null)).isFalse();
    }

    // ------------------------------------------------------------------ comunes

    @Test
    void unFiltroNoCoincideConElDetalleDeOtraCategoria() {
        assertThat(new FiltrosAlquilerEquipo(null).cumple(CABANA, null)).isFalse();
        assertThat(new FiltrosTour(null, null).cumple(TIPICA, null)).isFalse();
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
