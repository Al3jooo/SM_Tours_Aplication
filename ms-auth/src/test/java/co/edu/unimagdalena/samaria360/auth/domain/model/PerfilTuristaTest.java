package co.edu.unimagdalena.samaria360.auth.domain.model;

import co.edu.unimagdalena.samaria360.auth.domain.exception.DatosRegistroInvalidosException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/** Reglas del perfil del turista (RF53, DEC-85): las mismas del registro (RF01), sin correo ni contraseña. */
class PerfilTuristaTest {

    @Test
    void unPerfilValidoSeNormaliza() {
        PerfilTurista perfil = PerfilTurista.validar("  Ana ", " ", "Pérez", "", "co", List.of("Aventura", "Cultura"));

        assertThat(perfil.primerNombre()).isEqualTo("Ana");
        assertThat(perfil.segundoNombre()).isNull();
        assertThat(perfil.segundoApellido()).isNull();
        assertThat(perfil.paisOrigen().valor()).isEqualTo("CO");
        assertThat(perfil.categoriasOrdenadas()).containsExactly(CategoriaInteres.Cultura, CategoriaInteres.Aventura);
    }

    @Test
    void todosLosErroresSeRespondenALaVezConSusClavesJson() {
        Map<String, MotivoInvalido> campos = camposInvalidos(() -> PerfilTurista.validar(null, "s".repeat(51), " ",
                null, "COL", null));

        assertThat(campos).containsExactly(
                Map.entry("primerNombre", MotivoInvalido.REQUERIDO),
                Map.entry("segundoNombre", MotivoInvalido.LONGITUD_MAXIMA),
                Map.entry("primerApellido", MotivoInvalido.REQUERIDO),
                Map.entry("paisOrigen", MotivoInvalido.FORMATO),
                Map.entry("categoriasInteres", MotivoInvalido.REQUERIDO));
    }

    @Test
    void losNombresAdmitenHasta50CaracteresContadosEnPuntosDeCodigo() {
        String cincuenta = "ñ".repeat(49) + "😀";

        assertThat(PerfilTurista.validar(cincuenta, cincuenta, cincuenta, cincuenta, "CO", List.of("Cultura"))
                .segundoApellido()).isEqualTo(cincuenta);
        assertThat(camposInvalidos(() -> PerfilTurista.validar("Ana", null, cincuenta + "n", null, "CO",
                List.of("Cultura")))).containsExactly(Map.entry("primerApellido", MotivoInvalido.LONGITUD_MAXIMA));
    }

    @Test
    void unPaisInexistenteNoEstaPermitido() {
        assertThat(camposInvalidos(() -> PerfilTurista.validar("Ana", null, "Pérez", null, "ZZ", List.of("Cultura"))))
                .containsExactly(Map.entry("paisOrigen", MotivoInvalido.NO_PERMITIDO));
    }

    @Test
    void decisionDEC85_lasCategoriasVanDeUnaATresSinRepetirYDelCatalogo() {
        assertThat(perfil(List.of("Naturaleza")).categoriasInteres()).containsExactly(CategoriaInteres.Naturaleza);
        assertThat(perfil(List.of("Naturaleza", "Cultura", "Aventura")).categoriasInteres()).hasSize(3);

        assertThat(camposInvalidos(() -> perfil(List.of("Cultura", "Cultura"))))
                .containsExactly(Map.entry("categoriasInteres", MotivoInvalido.DUPLICADO));
        assertThat(camposInvalidos(() -> perfil(List.of("Naturaleza", "Cultura", "Aventura", "Aventura"))))
                .containsExactly(Map.entry("categoriasInteres", MotivoInvalido.DUPLICADO));
        assertThat(camposInvalidos(() -> perfil(List.of("Playa"))))
                .containsExactly(Map.entry("categoriasInteres", MotivoInvalido.NO_PERMITIDO));
        assertThat(camposInvalidos(() -> perfil(List.of("cultura"))))
                .containsExactly(Map.entry("categoriasInteres", MotivoInvalido.NO_PERMITIDO));
        assertThat(camposInvalidos(() -> perfil(List.of())))
                .containsExactly(Map.entry("categoriasInteres", MotivoInvalido.REQUERIDO));
    }

    @Test
    void leidoDeLaBasePuedeNoTenerCategorias() {
        PerfilTurista perfil = new PerfilTurista("Ana", null, "Pérez", null, new CodigoPais("US"), Set.of());

        assertThat(perfil.categoriasOrdenadas()).isEmpty();
    }

    private static PerfilTurista perfil(List<String> categorias) {
        return PerfilTurista.validar("Ana", null, "Pérez", null, "CO", categorias);
    }

    private static Map<String, MotivoInvalido> camposInvalidos(Runnable accion) {
        return catchThrowableOfType(DatosRegistroInvalidosException.class, accion::run).getCampos();
    }
}
