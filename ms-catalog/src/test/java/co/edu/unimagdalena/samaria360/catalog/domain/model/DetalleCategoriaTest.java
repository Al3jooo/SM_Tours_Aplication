package co.edu.unimagdalena.samaria360.catalog.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static co.edu.unimagdalena.samaria360.catalog.domain.model.Unidades.alojamiento;
import static co.edu.unimagdalena.samaria360.catalog.domain.model.Unidades.nuevoId;
import static co.edu.unimagdalena.samaria360.catalog.domain.model.Unidades.unidad;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DetalleCategoriaTest {

    @Test
    void cadaDetalleDeclaraSuCategoria() {
        assertThat(alojamiento("Doble", 12, 2, "150000", List.of("WiFi")).categoria()).isEqualTo(Categoria.Alojamiento);
        assertThat(new DetalleGastronomia("Mediterránea", 60, true, "$50.000 - $120.000").categoria())
                .isEqualTo(Categoria.Gastronomia);
        assertThat(new DetalleTour("Iglesia de Minca", new BigDecimal("4"), "Media", 12).categoria())
                .isEqualTo(Categoria.Tour);
        assertThat(new DetalleAlquilerEquipo("Kayak doble", 10, null).categoria()).isEqualTo(Categoria.AlquilerEquipo);
    }

    @Test
    void losCamposOpcionalesDelEsquemaPuedenFaltar() {
        assertThat(alojamiento("Doble", 1, 1, "100000", null).serviciosIncluidos()).isEmpty();
        assertThat(new DetalleGastronomia("Colombiana", 10, null, "$15.000").menuVegetariano()).isNull();
        assertThat(new DetalleTour("Muelle", new BigDecimal("3.5"), null, 8).nivelDificultad()).isNull();
        assertThat(new DetalleAlquilerEquipo("Bicicleta", 0, null).unidadesDisponibles()).isZero();
    }

    @Test
    void losCamposObligatoriosDelEsquemaNoPuedenFaltar() {
        assertThatThrownBy(() -> unidad(" ", 1, 1, "100000")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DetalleGastronomia("Colombiana", 10, true, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DetalleTour(null, BigDecimal.ONE, null, 8))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DetalleTour("Muelle", null, null, 8)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new DetalleAlquilerEquipo("", 1, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elAforoDelTourDebeSerPositivo() {
        assertThatThrownBy(() -> new DetalleTour("Muelle", BigDecimal.ONE, null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("aforoMaxGrupo");
    }

    @Test
    void rechazaCantidadesSinSentido() {
        assertThatThrownBy(() -> unidad("Doble", 0, 2, "100000")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DetalleGastronomia("Colombiana", 0, true, "$15.000"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DetalleTour("Muelle", BigDecimal.ZERO, null, 8))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DetalleAlquilerEquipo("Kayak", -1, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void laListaDeServiciosIncluidosEsInmutableYNoCompartida() {
        List<String> original = new ArrayList<>(List.of("WiFi"));
        DetalleAlojamiento detalle = alojamiento("Doble", 1, 2, "100000", original);
        original.add("Piscina");

        assertThat(detalle.serviciosIncluidos()).containsExactly("WiFi");
        assertThatThrownBy(() -> detalle.serviciosIncluidos().add("Desayuno"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // ------------------------------------------------------------------ unidades de alojamiento (D1)

    @Test
    void unAlojamientoTieneAlMenosUnaUnidad() {
        assertThatThrownBy(() -> new DetalleAlojamiento(List.of(), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("al menos una unidad");
        assertThatThrownBy(() -> new DetalleAlojamiento(null, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elIdDeUnidadNoSeRepiteEnElMismoAlojamiento() {
        UnidadAlojamiento doble = unidad("Doble", 12, 2, "150000");
        UnidadAlojamiento copia = new UnidadAlojamiento(doble.idUnidad(), "Suite", "Suite", 3, 4,
                new BigDecimal("260000"));

        assertThatThrownBy(() -> new DetalleAlojamiento(List.of(doble, copia), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idUnidad repetido");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "6ACFF19A69BFCC83461B196D", "6acff19a69bfcc83461b196", "6acff19a69bfcc83461b196dz"})
    void elIdDeUnidadTieneLaFormaDeUnObjectId(String id) {
        assertThatThrownBy(() -> new UnidadAlojamiento(id, "Doble", "Doble", 1, 2, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("idUnidad");
    }

    @Test
    void losNumerosYElPrecioDeLaUnidadSonPositivos() {
        String id = nuevoId();
        assertThatThrownBy(() -> new UnidadAlojamiento(id, "Doble", "Doble", 1, 0, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("capacidadMaxHuespedes");
        assertThatThrownBy(() -> new UnidadAlojamiento(id, "Doble", "Doble", 1, 2, BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("precioNoche");
        assertThatThrownBy(() -> new UnidadAlojamiento(id, "Doble", "Doble", 1, 2, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void elNombreDeLaUnidadAdmiteHasta100Caracteres() {
        String cien = "n".repeat(UnidadAlojamiento.LONGITUD_MAXIMA_NOMBRE);

        assertThat(unidad(cien, "Doble", 1, 2, "100000").nombre()).hasSize(100);
        assertThatThrownBy(() -> unidad(cien + "n", "Doble", 1, 2, "100000"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("nombre");
    }

    @Test
    void elPrecioDesdeEsElMenorPrecioPorNocheYSeEncuentraCadaUnidad() {
        UnidadAlojamiento suite = unidad("Suite familiar", "Suite", 3, 4, "260000");
        UnidadAlojamiento doble = unidad("Doble", 12, 2, "150000");
        DetalleAlojamiento hotel = new DetalleAlojamiento(List.of(suite, doble), List.of("WiFi"));

        assertThat(hotel.precioDesde()).isEqualByComparingTo("150000");
        assertThat(hotel.unidad(suite.idUnidad())).contains(suite);
        assertThat(hotel.unidad(nuevoId())).isEmpty();
        assertThatThrownBy(() -> hotel.unidades().add(doble)).isInstanceOf(UnsupportedOperationException.class);
    }
}
