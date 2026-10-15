package co.edu.unimagdalena.samaria360.booking.domain.model;

import co.edu.unimagdalena.samaria360.booking.domain.exception.DatosReservaInvalidosException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Rango de las consultas de disponibilidad: por defecto desde hoy y, como máximo, 60 días (ajuste 7). */
class RangoFechasTest {

    private static final LocalDate HOY = LocalDate.of(2030, 3, 10);

    @Test
    void sinFechasVaDesdeHoyConLosDiasPorDefecto() {
        assertThat(rango(null, null, 8)).isEqualTo(new RangoFechas(HOY, HOY.plusDays(7)));
        assertThat(rango("2030-04-01", null, 30)).isEqualTo(new RangoFechas(LocalDate.of(2030, 4, 1),
                LocalDate.of(2030, 4, 30)));
    }

    @Test
    void admiteHasta60DiasIncluidosLosExtremos() {
        assertThat(rango("2030-03-10", "2030-05-08", 30).hasta()).isEqualTo(LocalDate.of(2030, 5, 8));
        assertThatThrownBy(() -> rango("2030-03-10", "2030-05-09", 30))
                .isInstanceOf(DatosReservaInvalidosException.class)
                .satisfies(e -> assertThat(((DatosReservaInvalidosException) e).getCampos())
                        .containsEntry("hasta", MotivoInvalido.MAXIMO_DIAS));
    }

    @Test
    void hastaNoPuedeSerAntesQueDesdeYLasFechasDebenTenerFormato() {
        assertThatThrownBy(() -> rango("2030-03-10", "2030-03-09", 30))
                .satisfies(e -> assertThat(((DatosReservaInvalidosException) e).getCampos())
                        .containsEntry("hasta", MotivoInvalido.RANGO_INVALIDO));
        assertThatThrownBy(() -> rango("10/03/2030", null, 30))
                .satisfies(e -> assertThat(((DatosReservaInvalidosException) e).getCampos())
                        .containsEntry("desde", MotivoInvalido.FORMATO));
    }

    private static RangoFechas rango(String desde, String hasta, int diasPorDefecto) {
        ValidadorCampos validador = new ValidadorCampos();
        RangoFechas rango = RangoFechas.validar(desde, hasta, HOY, diasPorDefecto, validador);
        validador.lanzarSiHayErrores();
        return rango;
    }
}
