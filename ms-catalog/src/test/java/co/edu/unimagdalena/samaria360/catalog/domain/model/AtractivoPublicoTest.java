package co.edu.unimagdalena.samaria360.catalog.domain.model;

import org.junit.jupiter.api.Test;

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

    private static AtractivoPublico atractivo(String nombre, int limite, EstadoOperativo estado) {
        return new AtractivoPublico(null, GESTOR, nombre, QUEBRADA_VALENCIA, "Piscinas naturales", "Naturaleza",
                List.of("/img/cascada.svg"), limite, estado, "No usar bloqueador", Set.of());
    }
}
