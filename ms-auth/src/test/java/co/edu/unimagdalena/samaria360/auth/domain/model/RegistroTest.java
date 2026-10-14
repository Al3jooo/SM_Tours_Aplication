package co.edu.unimagdalena.samaria360.auth.domain.model;

import co.edu.unimagdalena.samaria360.auth.domain.exception.DatosRegistroInvalidosException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.PoliticasNoAceptadasException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/**
 * Reglas del registro (RF01, RF02): un código por campo con los nombres JSON de la API, todos los errores a la vez, y las
 * políticas después del formato. Las longitudes máximas son las de V1.
 */
class RegistroTest {

    // ------------------------------------------------------------------ turista

    @Test
    void unTuristaValidoSeNormaliza() {
        RegistroTurista turista = turista("  Ana ", " ", "Pérez", null, " Ana@X.com ", "Clave1234", "co",
                List.of("Cultura", "Naturaleza"), true);

        assertThat(turista.primerNombre()).isEqualTo("Ana");
        assertThat(turista.segundoNombre()).isNull();
        assertThat(turista.correo().valor()).isEqualTo("ana@x.com");
        assertThat(turista.paisOrigen().valor()).isEqualTo("CO");
        assertThat(turista.categoriasInteres())
                .containsExactlyInAnyOrder(CategoriaInteres.Naturaleza, CategoriaInteres.Cultura);
    }

    @Test
    void todosLosErroresDelTuristaSeRespondenALaVezConSusClavesJson() {
        Map<String, MotivoInvalido> campos = camposInvalidos(() -> turista("", null, "x".repeat(51), null, "sin-arroba",
                "corta", "COL", List.of(), true));

        assertThat(campos).containsExactly(
                Map.entry("primerNombre", MotivoInvalido.REQUERIDO),
                Map.entry("primerApellido", MotivoInvalido.LONGITUD_MAXIMA),
                Map.entry("correo", MotivoInvalido.FORMATO),
                Map.entry("contrasena", MotivoInvalido.LONGITUD_MINIMA),
                Map.entry("paisOrigen", MotivoInvalido.FORMATO),
                Map.entry("categoriasInteres", MotivoInvalido.REQUERIDO));
    }

    @Test
    void losNombresAdmitenHasta50Caracteres() {
        String cincuenta = "n".repeat(50);

        assertThat(turista(cincuenta, cincuenta, cincuenta, cincuenta, "ana@x.com", "Clave1234", "CO",
                List.of("Aventura"), true).segundoApellido()).hasSize(50);
        assertThat(camposInvalidos(() -> turista("Ana", cincuenta + "n", "Pérez", null, "ana@x.com", "Clave1234", "CO",
                List.of("Aventura"), true))).containsExactly(Map.entry("segundoNombre", MotivoInvalido.LONGITUD_MAXIMA));
    }

    @Test
    void decisionD1_lasCategoriasVanDeUnaATresSinRepetir() {
        assertThat(turista(List.of("Aventura")).categoriasInteres()).containsExactly(CategoriaInteres.Aventura);
        assertThat(turista(List.of("Naturaleza", "Cultura", "Aventura")).categoriasInteres()).hasSize(3);

        assertThat(camposInvalidos(() -> turista(List.of("Cultura", "Cultura"))))
                .containsExactly(Map.entry("categoriasInteres", MotivoInvalido.DUPLICADO));
        assertThat(camposInvalidos(() -> turista(List.of("Playa"))))
                .containsExactly(Map.entry("categoriasInteres", MotivoInvalido.NO_PERMITIDO));
        assertThat(camposInvalidos(() -> turista(List.of())))
                .containsExactly(Map.entry("categoriasInteres", MotivoInvalido.REQUERIDO));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = false)
    void unTuristaSinAceptarLasPoliticasNoSeRegistra(Boolean acepto) {
        assertThatThrownBy(() -> turista("Ana", null, "Pérez", null, "ana@x.com", "Clave1234", "CO",
                List.of("Cultura"), acepto)).isInstanceOf(PoliticasNoAceptadasException.class);
    }

    @Test
    void losErroresDeFormatoVanAntesQueLasPoliticas() {
        assertThatThrownBy(() -> turista("", null, "Pérez", null, "ana@x.com", "Clave1234", "CO", List.of("Cultura"),
                false)).isInstanceOf(DatosRegistroInvalidosException.class);
    }

    // ------------------------------------------------------------------ prestador

    @Test
    void unPrestadorValidoConRazonSocialOpcional() {
        RegistroPrestador prestador = prestador("  Carlos Gómez ", null, " RNT-123 ", "Contacto@Empresa.CO",
                "+57 300 000 0000", "Clave1234", true);

        assertThat(prestador.nombreRepresentanteLegal()).isEqualTo("Carlos Gómez");
        assertThat(prestador.razonSocial()).isNull();
        assertThat(prestador.rnt()).isEqualTo("RNT-123");
        assertThat(prestador.correoCorporativo().valor()).isEqualTo("contacto@empresa.co");
    }

    @Test
    void todosLosErroresDelPrestadorSeRespondenALaVezConSusClavesJson() {
        Map<String, MotivoInvalido> campos = camposInvalidos(() -> prestador("", "r".repeat(151), "x".repeat(51),
                "malo", "1".repeat(21), null, true));

        assertThat(campos).containsExactly(
                Map.entry("nombreRepresentanteLegal", MotivoInvalido.REQUERIDO),
                Map.entry("razonSocial", MotivoInvalido.LONGITUD_MAXIMA),
                Map.entry("rnt", MotivoInvalido.LONGITUD_MAXIMA),
                Map.entry("correoCorporativo", MotivoInvalido.FORMATO),
                Map.entry("telefono", MotivoInvalido.LONGITUD_MAXIMA),
                Map.entry("contrasena", MotivoInvalido.REQUERIDO));
    }

    @Test
    void elPrestadorAdmiteLasLongitudesMaximasDeV1() {
        RegistroPrestador prestador = prestador("r".repeat(150), "s".repeat(150), "n".repeat(50),
                "c".repeat(94) + "@x.com", "1".repeat(20), "Clave1234", true);

        assertThat(prestador.nombreRepresentanteLegal()).hasSize(150);
        assertThat(prestador.razonSocial()).hasSize(150);
        assertThat(prestador.rnt()).hasSize(50);
        assertThat(prestador.correoCorporativo().valor()).hasSize(100);
        assertThat(prestador.telefono()).hasSize(20);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(booleans = false)
    void unPrestadorSinAceptarLasPoliticasNoSeRegistra(Boolean acepto) {
        assertThatThrownBy(() -> prestador("Carlos", null, "RNT-1", "c@x.co", "300", "Clave1234", acepto))
                .isInstanceOf(PoliticasNoAceptadasException.class);
    }

    // ------------------------------------------------------------------ auxiliares

    private static RegistroTurista turista(List<String> categorias) {
        return turista("Ana", null, "Pérez", null, "ana@x.com", "Clave1234", "CO", categorias, true);
    }

    private static RegistroTurista turista(String primerNombre, String segundoNombre, String primerApellido,
                                           String segundoApellido, String correo, String contrasena, String pais,
                                           List<String> categorias, Boolean acepto) {
        return RegistroTurista.validar(primerNombre, segundoNombre, primerApellido, segundoApellido, correo,
                contrasena, pais, categorias, acepto);
    }

    private static RegistroPrestador prestador(String representante, String razonSocial, String rnt, String correo,
                                               String telefono, String contrasena, Boolean acepto) {
        return RegistroPrestador.validar(representante, razonSocial, rnt, correo, telefono, contrasena, acepto);
    }

    private static Map<String, MotivoInvalido> camposInvalidos(Runnable registro) {
        DatosRegistroInvalidosException error = catchThrowableOfType(DatosRegistroInvalidosException.class,
                registro::run);
        assertThat(error).as("se esperaba DATOS_INVALIDOS").isNotNull();
        return error.getCampos();
    }
}
