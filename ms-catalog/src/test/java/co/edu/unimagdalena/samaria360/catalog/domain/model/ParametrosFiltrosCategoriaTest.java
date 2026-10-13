package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.CriteriosBusquedaInvalidosException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cada parámetro de «Más filtros» exige su categoría (400 con «X requiere categoria=Y»). */
class ParametrosFiltrosCategoriaTest {

    static Stream<Arguments> unParametroPorCategoria() {
        return Stream.of(
                Arguments.of("serviciosIncluidos", con(p -> p.servicios = List.of("WiFi")), Categoria.Alojamiento),
                Arguments.of("huespedes", con(p -> p.huespedes = 2), Categoria.Alojamiento),
                Arguments.of("tipoHabitacion", con(p -> p.tipoHabitacion = "Doble"), Categoria.Alojamiento),
                Arguments.of("menuVegetariano", con(p -> p.menuVegetariano = true), Categoria.Gastronomia),
                Arguments.of("tipoCocina", con(p -> p.tipoCocina = "Típica"), Categoria.Gastronomia),
                Arguments.of("nivelDificultad", con(p -> p.nivelDificultad = "Fácil"), Categoria.Tour),
                Arguments.of("duracionMaxHoras", con(p -> p.duracionMaxHoras = BigDecimal.TEN), Categoria.Tour),
                Arguments.of("tipoVehiculoEquipo", con(p -> p.tipoVehiculo = "Kayak"), Categoria.AlquilerEquipo));
    }

    @ParameterizedTest(name = "{0} sin categoría")
    @MethodSource("unParametroPorCategoria")
    void sinCategoriaElMensajeDiceCualExige(String parametro, ParametrosFiltrosCategoria parametros, Categoria suya) {
        assertThatThrownBy(() -> parametros.paraCategoria(null))
                .isInstanceOf(CriteriosBusquedaInvalidosException.class)
                .hasMessage(parametro + " requiere categoria=" + suya.name());
    }

    @ParameterizedTest(name = "{0} con otra categoría")
    @MethodSource("unParametroPorCategoria")
    void conOtraCategoriaElMensajeDiceCualExige(String parametro, ParametrosFiltrosCategoria parametros,
                                               Categoria suya) {
        for (Categoria otra : Arrays.stream(Categoria.values()).filter(c -> c != suya).toList()) {
            assertThatThrownBy(() -> parametros.paraCategoria(otra))
                    .isInstanceOf(CriteriosBusquedaInvalidosException.class)
                    .hasMessage(parametro + " requiere categoria=" + suya.name());
        }
    }

    @ParameterizedTest(name = "{0} con su categoría")
    @MethodSource("unParametroPorCategoria")
    void conSuCategoriaProduceSusFiltros(String parametro, ParametrosFiltrosCategoria parametros, Categoria suya) {
        FiltrosCategoria filtros = parametros.paraCategoria(suya);

        assertThat(filtros.categoria()).isEqualTo(suya);
        assertThat(filtros.camposActivos()).containsExactly(parametro);
    }

    @Test
    void sinParametrosNoHayFiltros() {
        assertThat(ParametrosFiltrosCategoria.ninguno().paraCategoria(null)).isNull();
        assertThat(ParametrosFiltrosCategoria.ninguno().paraCategoria(Categoria.Tour)).isNull();
    }

    @Test
    void menuVegetarianoFalsoYTextosVaciosNoSonFiltros() {
        ParametrosFiltrosCategoria parametros = new ParametrosFiltrosCategoria(List.of("", " "), null, " ", false,
                "", null, null, "  ");

        assertThatCode(() -> parametros.paraCategoria(null)).doesNotThrowAnyException();
        assertThat(parametros.paraCategoria(Categoria.Tour)).isNull();
    }

    @Test
    void combinaLosDeUnaMismaCategoria() {
        FiltrosCategoria filtros = new ParametrosFiltrosCategoria(List.of("wifi", "Piscina", "WiFi"), 3, "Doble", null,
                null, null, null, null).paraCategoria(Categoria.Alojamiento);

        assertThat(filtros).isEqualTo(new FiltrosAlojamiento(
                Set.of(ServicioIncluido.WiFi, ServicioIncluido.Piscina), 3, "Doble"));
    }

    @Test
    void elPrimerParametroDeOtraCategoriaEsElQueSeReporta() {
        ParametrosFiltrosCategoria mezclados = new ParametrosFiltrosCategoria(null, 2, null, true, null, null, null,
                null);

        assertThatThrownBy(() -> mezclados.paraCategoria(Categoria.Alojamiento))
                .hasMessage("menuVegetariano requiere categoria=Gastronomia");
    }

    @Test
    void unServicioIncluidoDesconocidoEsUnCriterioInvalidoConElValorRecortado() {
        ParametrosFiltrosCategoria jacuzzi = new ParametrosFiltrosCategoria(List.of("WiFi", "Jacuzzi"), null, null,
                null, null, null, null, null);
        ParametrosFiltrosCategoria largo = new ParametrosFiltrosCategoria(List.of("J".repeat(300)), null, null,
                null, null, null, null, null);

        assertThatThrownBy(() -> jacuzzi.paraCategoria(Categoria.Alojamiento))
                .isInstanceOf(CriteriosBusquedaInvalidosException.class)
                .hasMessage("serviciosIncluidos no admite «Jacuzzi»; valores aceptados: WiFi, Desayuno, Piscina, Parqueadero");
        assertThatThrownBy(() -> largo.paraCategoria(Categoria.Alojamiento))
                .isInstanceOf(CriteriosBusquedaInvalidosException.class)
                .hasMessageContaining("«" + "J".repeat(40) + "…»")
                .hasMessageNotContaining("J".repeat(41));
    }

    // ------------------------------------------------------------------ auxiliares

    /** Constructor legible para los casos parametrizados. */
    private static final class Valores {
        List<String> servicios;
        Integer huespedes;
        String tipoHabitacion;
        Boolean menuVegetariano;
        String tipoCocina;
        String nivelDificultad;
        BigDecimal duracionMaxHoras;
        String tipoVehiculo;
    }

    private static ParametrosFiltrosCategoria con(Consumer<Valores> asignar) {
        Valores v = new Valores();
        asignar.accept(v);
        return new ParametrosFiltrosCategoria(v.servicios, v.huespedes, v.tipoHabitacion, v.menuVegetariano,
                v.tipoCocina, v.nivelDificultad, v.duracionMaxHoras, v.tipoVehiculo);
    }
}
