package co.edu.unimagdalena.samaria360.auth.domain.model;

import co.edu.unimagdalena.samaria360.auth.domain.exception.ValorInvalidoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Objetos de valor del registro (RF01, RF02): Correo, ContrasenaNueva, CodigoPais y CategoriaInteres. */
class ValoresRegistroTest {

    // ------------------------------------------------------------------ Correo

    @Test
    void elCorreoSeNormalizaSinEspaciosYEnMinusculas() {
        assertThat(new Correo("  Ana@X.com  ").valor()).isEqualTo("ana@x.com");
        assertThat(Correo.normalizar(" TURISTA@Samaria360.TEST ")).isEqualTo("turista@samaria360.test");
    }

    @ParameterizedTest
    @ValueSource(strings = {"sin-arroba", "a@b", "a b@c.co", "@x.co", "a@.co"})
    void unCorreoSinLaFormaBasicaEsFormato(String correo) {
        assertMotivo(() -> new Correo(correo), MotivoInvalido.FORMATO);
    }

    @Test
    void elCorreoAdmiteHasta100Caracteres() {
        String cien = "a".repeat(94) + "@x.com";

        assertThat(new Correo(cien).valor()).hasSize(100);
        assertMotivo(() -> new Correo("a" + cien), MotivoInvalido.LONGITUD_MAXIMA);
    }

    @Test
    void unCorreoVacioEsRequerido() {
        assertMotivo(() -> new Correo("   "), MotivoInvalido.REQUERIDO);
        assertMotivo(() -> new Correo(null), MotivoInvalido.REQUERIDO);
    }

    // ------------------------------------------------------------------ ContrasenaNueva

    @Test
    void laContrasenaExigeAlMenos8Caracteres() {
        assertMotivo(() -> new ContrasenaNueva("1234567"), MotivoInvalido.LONGITUD_MINIMA);
        assertThat(new ContrasenaNueva("12345678").valor()).isEqualTo("12345678");
        assertMotivo(() -> new ContrasenaNueva(""), MotivoInvalido.REQUERIDO);
    }

    @Test
    void laContrasenaAdmiteHasta72BytesElLimiteDeBcrypt() {
        assertThat(new ContrasenaNueva("a".repeat(72)).valor()).hasSize(72);
        assertMotivo(() -> new ContrasenaNueva("a".repeat(73)), MotivoInvalido.LONGITUD_MAXIMA);
        // 36 «ñ» son 72 bytes en UTF-8; 37, 74.
        assertThat(new ContrasenaNueva("ñ".repeat(36)).valor()).hasSize(36);
        assertMotivo(() -> new ContrasenaNueva("ñ".repeat(37)), MotivoInvalido.LONGITUD_MAXIMA);
    }

    @Test
    void laContrasenaNoApareceEnToString() {
        assertThat(new ContrasenaNueva("secreta123")).hasToString("ContrasenaNueva[****]");
    }

    // ------------------------------------------------------------------ CodigoPais

    @ParameterizedTest
    @CsvSource({"CO,CO", "co,CO", "' es ',ES", "us,US"})
    void elPaisSeNormalizaAMayusculas(String recibido, String esperado) {
        assertThat(new CodigoPais(recibido).valor()).isEqualTo(esperado);
    }

    @Test
    void unPaisQueNoEsDeDosLetrasEsFormatoYUnoInexistenteNoEstaPermitido() {
        assertMotivo(() -> new CodigoPais("COL"), MotivoInvalido.FORMATO);
        assertMotivo(() -> new CodigoPais("Colombia"), MotivoInvalido.FORMATO);
        assertMotivo(() -> new CodigoPais("XX"), MotivoInvalido.NO_PERMITIDO);
        assertMotivo(() -> new CodigoPais(""), MotivoInvalido.REQUERIDO);
    }

    @Test
    void laListaDePaisesEsLaIso3166DelJdkOrdenadaPorCodigo() {
        assertThat(CodigoPais.todos())
                .containsExactlyElementsOf(
                        Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2).stream().sorted().toList())
                .contains("CO", "ES", "US")
                .allMatch(codigo -> codigo.matches("[A-Z]{2}"))
                .isSorted();
    }

    // ------------------------------------------------------------------ CategoriaInteres

    @Test
    void lasCategoriasSonLasTresDeWf02() {
        assertThat(CategoriaInteres.values())
                .containsExactly(CategoriaInteres.Naturaleza, CategoriaInteres.Cultura, CategoriaInteres.Aventura);
        assertThat(CategoriaInteres.desdeTexto("Cultura")).contains(CategoriaInteres.Cultura);
        assertThat(CategoriaInteres.desdeTexto("cultura")).isEmpty();
        assertThat(CategoriaInteres.desdeTexto("Playa")).isEmpty();
    }

    private static void assertMotivo(Runnable crear, MotivoInvalido motivo) {
        assertThatThrownBy(crear::run)
                .isInstanceOf(ValorInvalidoException.class)
                .extracting(e -> ((ValorInvalidoException) e).getMotivo())
                .isEqualTo(motivo);
    }
}
