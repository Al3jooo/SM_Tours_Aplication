package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.fake.PreguntaFrecuenteRepositoryEnMemoria;
import co.edu.unimagdalena.samaria360.catalog.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.LimitePreguntasException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.PreguntaNoEncontradaException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.MotivoInvalido;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PreguntaFrecuente;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** CU «Gestionar FAQs» (RF26) con dobles de los puertos. */
class GestionarPreguntasFrecuentesServiceTest {

    private static final Instant AHORA = Instant.parse("2026-10-22T15:00:00.123456789Z");
    private static final String INEXISTENTE = "6a1f0000000000000000ffff";

    private final PreguntaFrecuenteRepositoryEnMemoria preguntas = new PreguntaFrecuenteRepositoryEnMemoria();
    private final RelojFijo reloj = new RelojFijo(AHORA);
    private final GestionarPreguntasFrecuentesService servicio =
            new GestionarPreguntasFrecuentesService(preguntas, reloj);

    private void llenarHasta(int cantidad) {
        while (preguntas.insertadas() < cantidad) {
            preguntas.agregar(new PreguntaFrecuente(preguntas.nuevoId(), "Pregunta " + preguntas.insertadas(),
                    "Cuerpo", AHORA));
        }
    }

    @Test
    void crearGuardaLosTextosLimpiosConLaHoraDelRelojEnMilisegundos() {
        PreguntaFrecuente creada = servicio.crear("  ¿Hay parqueadero?  ", " Sí, en la entrada. ");

        assertThat(creada.titulo()).isEqualTo("¿Hay parqueadero?");
        assertThat(creada.cuerpo()).isEqualTo("Sí, en la entrada.");
        assertThat(creada.fechaActualizacion()).isEqualTo(Instant.parse("2026-10-22T15:00:00.123Z"));
        assertThat(servicio.listar()).containsExactly(creada);
    }

    @Test
    void conDatosInvalidosNoGuardaNada() {
        assertThatThrownBy(() -> servicio.crear("", "b".repeat(2001)))
                .isInstanceOfSatisfying(ContenidoInvalidoException.class, e -> assertThat(e.getCampos())
                        .containsEntry("titulo", MotivoInvalido.REQUERIDO)
                        .containsEntry("cuerpo", MotivoInvalido.LONGITUD_MAXIMA));
        assertThat(preguntas.insertadas()).isZero();
    }

    @Test
    void con200PreguntasLaSiguienteCreacionSeRechazaYCon199No() {
        llenarHasta(199);
        servicio.crear("La número 200", "Cuerpo");

        assertThatThrownBy(() -> servicio.crear("La número 201", "Cuerpo"))
                .isInstanceOf(LimitePreguntasException.class);
        assertThat(preguntas.insertadas()).isEqualTo(PreguntaFrecuente.MAXIMO_PREGUNTAS);
    }

    @Test
    void losDatosSeValidanAntesQueElTope() {
        llenarHasta(200);

        assertThatThrownBy(() -> servicio.crear(" ", "Cuerpo")).isInstanceOf(ContenidoInvalidoException.class);
    }

    @Test
    void editarReemplazaLosTextosYRenuevaLaFecha() {
        PreguntaFrecuente creada = servicio.crear("Antes", "Cuerpo viejo");
        reloj.avanzar(Duration.ofHours(1));

        PreguntaFrecuente editada = servicio.editar(creada.id(), "Después ", "Cuerpo nuevo");

        assertThat(editada).isEqualTo(new PreguntaFrecuente(creada.id(), "Después", "Cuerpo nuevo",
                Instant.parse("2026-10-22T16:00:00.123Z")));
        assertThat(servicio.listar()).containsExactly(editada);
    }

    @ParameterizedTest
    @ValueSource(strings = {INEXISTENTE, "no-es-un-id", "6A1F00000000000000000001"})
    void editarOEliminarUnIdInexistenteOMalFormadoEs404(String id) {
        assertThatThrownBy(() -> servicio.editar(id, "Título", "Cuerpo"))
                .isInstanceOf(PreguntaNoEncontradaException.class);
        assertThatThrownBy(() -> servicio.eliminar(id)).isInstanceOf(PreguntaNoEncontradaException.class);
    }

    @Test
    void editarValidaLosDatosAntesDeBuscarLaPregunta() {
        assertThatThrownBy(() -> servicio.editar("no-es-un-id", "", "Cuerpo"))
                .isInstanceOf(ContenidoInvalidoException.class);
    }

    @Test
    void eliminarBorraLaPreguntaYDespuesEditarlaOEliminarlaEs404() {
        PreguntaFrecuente creada = servicio.crear("Título", "Cuerpo");

        servicio.eliminar(creada.id());

        assertThat(servicio.listar()).isEmpty();
        assertThatThrownBy(() -> servicio.eliminar(creada.id())).isInstanceOf(PreguntaNoEncontradaException.class);
        assertThatThrownBy(() -> servicio.editar(creada.id(), "Título", "Cuerpo"))
                .isInstanceOf(PreguntaNoEncontradaException.class);
    }

    @Test
    void elListadoVaPorTituloSinDistinguirMayusculasNiTildes() {
        servicio.crear("zeta", "Cuerpo");
        servicio.crear("Ábaco", "Cuerpo");
        servicio.crear("abeja", "Cuerpo");
        servicio.crear("Beta", "Cuerpo");

        assertThat(servicio.listar()).extracting(PreguntaFrecuente::titulo)
                .containsExactly("Ábaco", "abeja", "Beta", "zeta");
    }
}
