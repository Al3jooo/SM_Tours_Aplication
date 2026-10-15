package co.edu.unimagdalena.samaria360.booking.domain.model;

import co.edu.unimagdalena.samaria360.booking.domain.exception.DatosReservaInvalidosException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/** Formato del pedido (todos los errores a la vez) y hash del comando normalizado (D6). */
class SolicitudReservaTest {

    @Test
    void unPedidoValidoSeNormaliza() {
        SolicitudReserva solicitud = SolicitudReserva.validar(" " + Ofertas.TOUR + " ", null, "2030-03-12", null,
                "08:00:00", null, 2, new BigDecimal("160000.00"));

        assertThat(solicitud.idServicio()).isEqualTo(Ofertas.TOUR);
        assertThat(solicitud.fechaEntrada()).isEqualTo(LocalDate.of(2030, 3, 12));
        assertThat(solicitud.horaInicio()).isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void todosLosErroresDeFormatoSeRespondenALaVezConSusClavesJson() {
        Map<String, MotivoInvalido> campos = campos(() -> SolicitudReserva.validar("hotel", "abc", "12/03/2030",
                "mañana", "8 am", 0, null, new BigDecimal("-1")));

        assertThat(campos).containsExactly(
                Map.entry("idServicio", MotivoInvalido.FORMATO),
                Map.entry("idUnidad", MotivoInvalido.FORMATO),
                Map.entry("fechaEntrada", MotivoInvalido.FORMATO),
                Map.entry("fechaSalida", MotivoInvalido.FORMATO),
                Map.entry("horaInicio", MotivoInvalido.FORMATO),
                Map.entry("cantidad", MotivoInvalido.VALOR_MINIMO),
                Map.entry("personas", MotivoInvalido.REQUERIDO),
                Map.entry("precioTotalEsperado", MotivoInvalido.VALOR_MINIMO));
    }

    @Test
    void losCamposComunesSonObligatorios() {
        assertThat(campos(() -> SolicitudReserva.validar(null, null, " ", null, null, null, 0, null))).containsExactly(
                Map.entry("idServicio", MotivoInvalido.REQUERIDO),
                Map.entry("fechaEntrada", MotivoInvalido.REQUERIDO),
                Map.entry("personas", MotivoInvalido.VALOR_MINIMO),
                Map.entry("precioTotalEsperado", MotivoInvalido.REQUERIDO));
    }

    @Test
    void elMismoPedidoEscritoDeOtraFormaTieneElMismoHash() {
        String uno = SolicitudReserva.validar(Ofertas.TOUR, null, "2030-03-12", null, "08:00", null, 2,
                new BigDecimal("160000")).hash();
        String otro = SolicitudReserva.validar(" " + Ofertas.TOUR, " ", "2030-03-12 ", "", "08:00:00", null, 2,
                new BigDecimal("160000.00")).hash();

        assertThat(uno).isEqualTo(otro).hasSize(64).matches("[0-9a-f]{64}");
    }

    @Test
    void cualquierDatoDistintoIncluidaLaUnidadCambiaElHash() {
        SolicitudReserva doble = Ofertas.alojamiento(Ofertas.DOBLE, LocalDate.of(2030, 3, 12), 2, 1, 2, "300000");
        SolicitudReserva suite = Ofertas.alojamiento(Ofertas.SUITE, LocalDate.of(2030, 3, 12), 2, 1, 2, "300000");
        SolicitudReserva otraNoche = Ofertas.alojamiento(Ofertas.DOBLE, LocalDate.of(2030, 3, 13), 2, 1, 2, "300000");

        assertThat(doble.hash()).isNotEqualTo(suite.hash()).isNotEqualTo(otraNoche.hash());
    }

    private static Map<String, MotivoInvalido> campos(Runnable validacion) {
        DatosReservaInvalidosException error = catchThrowableOfType(DatosReservaInvalidosException.class,
                validacion::run);
        assertThat(error).as("se esperaba DATOS_INVALIDOS").isNotNull();
        return error.getCampos();
    }
}
