package co.edu.unimagdalena.samaria360.catalog.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AtractivoPublicoTest {

    private static final UUID GESTOR = UUID.fromString("00000000-0000-4000-8000-000000000003");
    private static final Coordenada QUEBRADA_VALENCIA = new Coordenada(-73.796, 11.235);

    @Test
    void creaUnAtractivoValidoConEstadoOperativoOpcional() {
        AtractivoPublico cascada = atractivo("Cascada Quebrada Valencia", 300, EstadoOperativo.Abierto);
        AtractivoPublico sinEstado = atractivo("Mirador", 1, null);

        assertThat(cascada.getEstadoOperativo()).contains(EstadoOperativo.Abierto);
        assertThat(cascada.getLimiteCapacidadCarga()).isEqualTo(300);
        assertThat(sinEstado.getEstadoOperativo()).isEmpty();
    }

    @Test
    void elLimiteDeCapacidadDeCargaEsAlMenos1() {
        assertThatThrownBy(() -> atractivo("Cascada", 0, EstadoOperativo.Abierto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limiteCapacidadCarga");
    }

    @Test
    void elNombreYElGestorSonObligatorios() {
        assertThatThrownBy(() -> atractivo(" ", 10, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AtractivoPublico(null, null, "Cascada", QUEBRADA_VALENCIA, null, null, null, 10,
                null, null, null))
                .isInstanceOf(NullPointerException.class);
    }

    /** Iteración E (DEC-207, DEC-208): un documento anterior a M012 se rehidrata sin horario, fechas ni versión. */
    @Test
    void sinLosCamposDeLaGestionEsLaVersionCeroSinHorarioNiFechas() {
        AtractivoPublico semilla = atractivo("Cascada", 10, EstadoOperativo.Abierto);
        AtractivoPublico gestionado = new AtractivoPublico("0000000000000000000000aa", GESTOR, "Cascada",
                QUEBRADA_VALENCIA, null, "Naturaleza", null, 10, null, null, null, "Lunes a viernes",
                Instant.parse("2026-10-01T10:00:00Z"), null, 3);

        assertThat(semilla.getVersion()).isZero();
        assertThat(semilla.getHorarioAtencion()).isEmpty();
        assertThat(semilla.getFechaRegistro()).isEmpty();
        assertThat(gestionado.getHorarioAtencion()).contains("Lunes a viernes");
        assertThat(gestionado.getVersion()).isEqualTo(3);
        assertThat(gestionado.getImagenes()).isEmpty();
        assertThatThrownBy(() -> new AtractivoPublico(null, GESTOR, "Cascada", QUEBRADA_VALENCIA, null, null, null,
                10, null, null, null, null, null, null, -1)).isInstanceOf(IllegalArgumentException.class);
    }

    private static AtractivoPublico atractivo(String nombre, int limite, EstadoOperativo estado) {
        return new AtractivoPublico(null, GESTOR, nombre, QUEBRADA_VALENCIA, "Piscinas naturales", "Naturaleza",
                List.of("/img/cascada.svg"), limite, estado, "No usar bloqueador", Set.of());
    }
}
