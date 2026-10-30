package co.edu.unimagdalena.samaria360.auth.domain.model;

import co.edu.unimagdalena.samaria360.auth.domain.exception.DatosRegistroInvalidosException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/** Reglas del perfil comercial del Prestador (RF43, DEC-188): las del registro (RF02) para el correo y el teléfono. */
class DatosComercialesPrestadorTest {

    @Test
    void unosDatosValidosSeNormalizan() {
        DatosComercialesPrestador datos = DatosComercialesPrestador.validar("  Reservas@Kayak.TEST ", " +57 311 ");

        assertThat(datos.correoCorporativo().valor()).isEqualTo("reservas@kayak.test");
        assertThat(datos.telefono()).isEqualTo("+57 311");
    }

    @Test
    void todosLosErroresSeRespondenALaVezConSusClavesJson() {
        assertThat(campos(null, " ")).containsExactly(Map.entry("correoCorporativo", MotivoInvalido.REQUERIDO),
                Map.entry("telefono", MotivoInvalido.REQUERIDO));
        assertThat(campos("sin-arroba", "1".repeat(21))).containsExactly(
                Map.entry("correoCorporativo", MotivoInvalido.FORMATO),
                Map.entry("telefono", MotivoInvalido.LONGITUD_MAXIMA));
        assertThat(campos("a".repeat(95) + "@x.com", "300")).containsExactly(
                Map.entry("correoCorporativo", MotivoInvalido.LONGITUD_MAXIMA));
    }

    private static Map<String, MotivoInvalido> campos(String correo, String telefono) {
        DatosRegistroInvalidosException error = catchThrowableOfType(DatosRegistroInvalidosException.class,
                () -> DatosComercialesPrestador.validar(correo, telefono));
        assertThat(error).isNotNull();
        return error.getCampos();
    }
}
