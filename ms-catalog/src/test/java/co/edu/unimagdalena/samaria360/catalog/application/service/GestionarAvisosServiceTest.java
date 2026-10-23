package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.fake.AvisoRepositoryEnMemoria;
import co.edu.unimagdalena.samaria360.catalog.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.AvisoNoEncontradoException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.AvisoYaInactivoException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Aviso;
import co.edu.unimagdalena.samaria360.catalog.domain.model.MotivoInvalido;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;
import co.edu.unimagdalena.samaria360.catalog.domain.model.TipoAviso;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** CU «Crear y publicar aviso general» (RF40) y «Consultar avisos generales», con dobles de los puertos. */
class GestionarAvisosServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-22T15:00:00.987654Z");

    private final AvisoRepositoryEnMemoria avisos = new AvisoRepositoryEnMemoria();
    private final RelojFijo reloj = new RelojFijo(AHORA);
    private final GestionarAvisosService servicio = new GestionarAvisosService(avisos, reloj);

    private Aviso publicarHace(int horas, boolean activo) {
        Aviso aviso = new Aviso(avisos.nuevoId(), "Aviso de hace " + horas + " h", TipoAviso.Comunicado, activo,
                AHORA.minus(Duration.ofHours(horas)));
        avisos.agregar(aviso);
        return aviso;
    }

    @Test
    void publicarGuardaUnAvisoActivoConElMensajeLimpioYLaHoraDelReloj() {
        Aviso aviso = servicio.publicar(" Lluvias en la Sierra ", "Clima");

        assertThat(aviso.activo()).isTrue();
        assertThat(aviso.mensaje()).isEqualTo("Lluvias en la Sierra");
        assertThat(aviso.tipo()).isEqualTo(TipoAviso.Clima);
        assertThat(aviso.fechaPublicacion()).isEqualTo(Instant.parse("2026-10-22T15:00:00.987Z"));
        assertThat(avisos.buscar(aviso.id())).contains(aviso);
    }

    @Test
    void conDatosInvalidosNoGuardaNada() {
        assertThatThrownBy(() -> servicio.publicar("", "Lluvia"))
                .isInstanceOfSatisfying(ContenidoInvalidoException.class, e -> assertThat(e.getCampos())
                        .containsEntry("mensaje", MotivoInvalido.REQUERIDO)
                        .containsEntry("tipo", MotivoInvalido.NO_PERMITIDO));
        assertThat(avisos.guardados()).isZero();
    }

    @Test
    void losActivosVanDelMasRecienteAlMasAntiguoSinLosInactivos() {
        Aviso viejo = publicarHace(5, true);
        publicarHace(1, false);
        Aviso reciente = publicarHace(2, true);

        assertThat(servicio.activos()).containsExactly(reciente, viejo);
    }

    @Test
    void elListadoDelAdministradorEsDe10PorPaginaConLosInactivos() {
        for (int horas = 1; horas <= 12; horas++) {
            publicarHace(horas, horas % 2 == 0);
        }

        PaginaResultados<Aviso> primera = servicio.pagina(0);
        PaginaResultados<Aviso> segunda = servicio.pagina(1);

        assertThat(primera.elementos()).hasSize(10);
        assertThat(primera.elementos().get(0).mensaje()).isEqualTo("Aviso de hace 1 h");
        assertThat(primera.totalElementos()).isEqualTo(12);
        assertThat(primera.totalPaginas()).isEqualTo(2);
        assertThat(segunda.elementos()).extracting(Aviso::mensaje).containsExactly("Aviso de hace 11 h",
                "Aviso de hace 12 h");
    }

    @Test
    void unaPaginaNegativaEsUnFormatoInvalido() {
        assertThatThrownBy(() -> servicio.pagina(-1))
                .isInstanceOfSatisfying(ContenidoInvalidoException.class, e -> assertThat(e.getCampos())
                        .containsExactlyEntriesOf(Map.of("pagina", MotivoInvalido.FORMATO)));
    }

    @Test
    void despublicarDejaElAvisoInactivoYFueraDeLosActivos() {
        Aviso aviso = servicio.publicar("Cierre del sendero", "Cierre");

        Aviso despublicado = servicio.despublicar(aviso.id());

        assertThat(despublicado.activo()).isFalse();
        assertThat(despublicado.mensaje()).isEqualTo(aviso.mensaje());
        assertThat(servicio.activos()).isEmpty();
    }

    @Test
    void despublicarUnAvisoYaInactivoEs409() {
        Aviso aviso = publicarHace(1, false);

        assertThatThrownBy(() -> servicio.despublicar(aviso.id())).isInstanceOf(AvisoYaInactivoException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"6a1e0000000000000000ffff", "no-es-un-id"})
    void despublicarUnIdInexistenteOMalFormadoEs404(String id) {
        assertThatThrownBy(() -> servicio.despublicar(id)).isInstanceOf(AvisoNoEncontradoException.class);
    }
}
