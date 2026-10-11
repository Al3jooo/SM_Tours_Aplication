package co.edu.unimagdalena.samaria360.catalog.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CoordenadaTest {

    @Test
    void guardaLongitudYLatitudEnEseOrden() {
        Coordenada parqueDeLosNovios = new Coordenada(-74.2125, 11.2425);

        assertThat(parqueDeLosNovios.longitud()).isEqualTo(-74.2125);
        assertThat(parqueDeLosNovios.latitud()).isEqualTo(11.2425);
    }

    @ParameterizedTest
    @CsvSource({"-180, -90", "180, 90", "0, 0"})
    void aceptaLosLimitesExactos(double longitud, double latitud) {
        assertThatCode(() -> new Coordenada(longitud, latitud)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @CsvSource({"-180.0001, 0", "180.0001, 0", "0, -90.0001", "0, 90.0001"})
    void rechazaValoresFueraDeRango(double longitud, double latitud) {
        assertThatThrownBy(() -> new Coordenada(longitud, latitud)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elRangoNoDetectaUnOrdenInvertidoDentroDeLosLimites() {
        // Santa Marta con el orden invertido [latitud, longitud] sigue siendo un punto válido del planeta
        // (latitud -74,21, en la Antártida). Por eso el orden se verifica también contra el área de Santa Marta
        // en ScriptsInicializacionMongoTest y se deberá verificar en el mapeo del adaptador.
        assertThatCode(() -> new Coordenada(11.24, -74.21)).doesNotThrowAnyException();
    }

    @Test
    void rechazaValoresNoFinitos() {
        assertThatThrownBy(() -> new Coordenada(Double.NaN, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Coordenada(0, Double.POSITIVE_INFINITY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
