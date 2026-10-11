package co.edu.unimagdalena.samaria360.catalog.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DetalleCategoriaTest {

    @Test
    void cadaDetalleDeclaraSuCategoria() {
        assertThat(new DetalleAlojamiento("Doble", 12, 2, List.of("WiFi")).categoria()).isEqualTo(Categoria.Alojamiento);
        assertThat(new DetalleGastronomia("Mediterránea", 60, true, "$50.000 - $120.000").categoria())
                .isEqualTo(Categoria.Gastronomia);
        assertThat(new DetalleTour("Iglesia de Minca", new BigDecimal("4"), "Media", 12).categoria())
                .isEqualTo(Categoria.Tour);
        assertThat(new DetalleAlquilerEquipo("Kayak doble", 10, null).categoria()).isEqualTo(Categoria.AlquilerEquipo);
    }

    @Test
    void losCamposOpcionalesDelEsquemaPuedenFaltar() {
        assertThat(new DetalleAlojamiento("Doble", 1, 1, null).serviciosIncluidos()).isEmpty();
        assertThat(new DetalleGastronomia("Colombiana", 10, null, "$15.000").menuVegetariano()).isNull();
        assertThat(new DetalleTour("Muelle", new BigDecimal("3.5"), null, 8).nivelDificultad()).isNull();
        assertThat(new DetalleAlquilerEquipo("Bicicleta", 0, null).unidadesDisponibles()).isZero();
    }

    @Test
    void losCamposObligatoriosDelEsquemaNoPuedenFaltar() {
        assertThatThrownBy(() -> new DetalleAlojamiento(" ", 1, 1, null)).isInstanceOf(IllegalArgumentException.class);
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
        assertThatThrownBy(() -> new DetalleAlojamiento("Doble", 0, 2, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DetalleGastronomia("Colombiana", 0, true, "$15.000"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DetalleTour("Muelle", BigDecimal.ZERO, null, 8))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DetalleAlquilerEquipo("Kayak", -1, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void laListaDeServiciosIncluidosEsInmutableYNoCompartida() {
        List<String> original = new ArrayList<>(List.of("WiFi"));
        DetalleAlojamiento detalle = new DetalleAlojamiento("Doble", 1, 2, original);
        original.add("Piscina");

        assertThat(detalle.serviciosIncluidos()).containsExactly("WiFi");
        assertThatThrownBy(() -> detalle.serviciosIncluidos().add("Desayuno"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
