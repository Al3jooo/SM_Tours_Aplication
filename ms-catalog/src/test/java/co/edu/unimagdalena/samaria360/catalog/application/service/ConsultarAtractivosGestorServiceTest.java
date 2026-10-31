package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.fake.GestionAtractivosEnMemoria;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.AtractivoNoEncontradoException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.AtractivoPublico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Coordenada;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EstadoOperativo;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Panel «Atractivos» del Gestor (WF-24; DEC-206): todos, de 10 en 10, del más reciente al más antiguo. */
class ConsultarAtractivosGestorServiceTest {

    private final GestionAtractivosEnMemoria atractivos = new GestionAtractivosEnMemoria();
    private final ConsultarAtractivosGestorService servicio = new ConsultarAtractivosGestorService(atractivos);

    @Test
    void listaLosDeTodosLosGestoresDelMasRecienteAlMasAntiguoEnPaginasDeDiez() {
        for (int i = 1; i <= 12; i++) {
            atractivos.agregar(atractivo(i, UUID.randomUUID()));
        }

        PaginaResultados<AtractivoPublico> primera = servicio.listar(0);
        PaginaResultados<AtractivoPublico> segunda = servicio.listar(1);

        assertThat(primera.elementos()).hasSize(10).first()
                .satisfies(a -> assertThat(a.getNombre()).isEqualTo("Atractivo 12"));
        assertThat(primera.totalElementos()).isEqualTo(12);
        assertThat(segunda.elementos()).extracting(AtractivoPublico::getNombre)
                .containsExactly("Atractivo 2", "Atractivo 1");
    }

    @Test
    void unaPaginaNegativaEs400YUnIdInvalidoEs404() {
        assertThatThrownBy(() -> servicio.listar(-1)).isInstanceOf(ContenidoInvalidoException.class);
        assertThatThrownBy(() -> servicio.detalle("x")).isInstanceOf(AtractivoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.detalle("0000000000000000000000aa"))
                .isInstanceOf(AtractivoNoEncontradoException.class);
    }

    private static AtractivoPublico atractivo(int n, UUID gestor) {
        return new AtractivoPublico(String.format("%024x", n), gestor, "Atractivo " + n, new Coordenada(-74, 11), null,
                "Cultura", List.of(), 10, EstadoOperativo.Abierto, null, Set.of(), null,
                Instant.parse("2026-10-01T00:00:00Z").plusSeconds(n), null, 0);
    }
}
