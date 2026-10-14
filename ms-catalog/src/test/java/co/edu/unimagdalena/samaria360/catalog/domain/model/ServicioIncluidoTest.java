package co.edu.unimagdalena.samaria360.catalog.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServicioIncluidoTest {

    @Test
    void elVocabularioEsElDeRf10YWf08() {
        assertThat(ServicioIncluido.values())
                .containsExactly(ServicioIncluido.WiFi, ServicioIncluido.Desayuno, ServicioIncluido.Piscina,
                        ServicioIncluido.Parqueadero);
        assertThat(ServicioIncluido.aceptados()).isEqualTo("WiFi, Desayuno, Piscina, Parqueadero");
    }

    @ParameterizedTest
    @CsvSource({"WiFi,WiFi", "wifi,WiFi", "WIFI,WiFi", "' Piscina ',Piscina", "parqueadero,Parqueadero"})
    void seLeeSinDistinguirMayusculas(String texto, ServicioIncluido esperado) {
        assertThat(ServicioIncluido.desdeTexto(texto)).isEqualTo(esperado);
    }

    @Test
    void unValorDesconocidoListaLosAceptados() {
        assertThatThrownBy(() -> ServicioIncluido.desdeTexto("Jacuzzi"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("serviciosIncluidos no admite «Jacuzzi»; valores aceptados: WiFi, Desayuno, Piscina, Parqueadero");
    }

    @Test
    void unValorLargoSeRecortaA40CaracteresAntesDeRepetirlo() {
        String largo = "x".repeat(39) + "ñ" + "y".repeat(500);

        assertThatThrownBy(() -> ServicioIncluido.desdeTexto(largo))
                .hasMessageContaining("«" + "x".repeat(39) + "ñ…»")
                .hasMessageNotContaining("yy");
    }

    @Test
    void elRecorteNoParteUnEmoji() {
        String conEmoji = "a".repeat(39) + "🏊" + "b";

        assertThat(ServicioIncluido.recortar(conEmoji)).isEqualTo("a".repeat(39) + "🏊…");
        assertThat(ServicioIncluido.recortar("corto")).isEqualTo("corto");
    }

    @Test
    void elDetalleDeAlojamientoGuardaLosNombresCanonicosSinRepetidos() {
        DetalleAlojamiento detalle = Unidades.alojamiento("Doble", 1, 2, "100000", List.of("wifi", "DESAYUNO", "WiFi"));

        assertThat(detalle.serviciosIncluidos()).containsExactly("WiFi", "Desayuno");
    }

    @Test
    void elDetalleDeAlojamientoRechazaUnServicioFueraDelVocabulario() {
        assertThatThrownBy(() -> Unidades.alojamiento("Doble", 1, 2, "100000", List.of("Jacuzzi")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Jacuzzi");
    }
}
