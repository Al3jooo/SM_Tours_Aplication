package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.fake.GestionAtractivosEnMemoria;
import co.edu.unimagdalena.samaria360.catalog.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.AtractivoModificadoException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.AtractivoNoEncontradoException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.AtractivoPublico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorAtractivo;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Coordenada;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EstadoOperativo;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** CU «Actualizar atractivo» y «Cambiar estado operativo» (RF07, RF24, RF27, RF33, RF58; DEC-206, DEC-207). */
class ActualizarAtractivoServiceTest {

    private static final String ID = "0000000000000000000000aa";
    private static final UUID OTRO_GESTOR = UUID.fromString("00000000-0000-4000-8000-0000000000ff");
    private static final Instant AHORA = Instant.parse("2026-10-30T15:00:00.999999Z");

    private final GestionAtractivosEnMemoria atractivos = new GestionAtractivosEnMemoria();
    private final ActualizarAtractivoService servicio = new ActualizarAtractivoService(atractivos, new RelojFijo(AHORA));

    @Test
    void cualquierGestorEditaYLaVersionSube() {
        atractivos.agregar(atractivo(2));

        AtractivoPublico editado = servicio.actualizar(ID, borrador("Cascada renovada"), 2);

        assertThat(editado.getNombre()).isEqualTo("Cascada renovada");
        assertThat(editado.getVersion()).isEqualTo(3);
        assertThat(editado.getFechaActualizacion()).contains(Instant.parse("2026-10-30T15:00:00.999Z"));
        assertThat(editado.getGestorId()).as("quién lo registró no cambia").isEqualTo(OTRO_GESTOR);
    }

    @Test
    void cambiaElEstadoYSubeLaVersion() {
        atractivos.agregar(atractivo(0));

        AtractivoPublico cerrado = servicio.cambiarEstado(ID, "En_mantenimiento", 0);

        assertThat(cerrado.getEstadoOperativo()).contains(EstadoOperativo.En_mantenimiento);
        assertThat(cerrado.getVersion()).isEqualTo(1);
    }

    @Test
    void unaVersionViejaEsAtractivoModificadoConLaVersionActual() {
        atractivos.agregar(atractivo(5));

        assertThatThrownBy(() -> servicio.actualizar(ID, borrador("Otro"), 4))
                .isInstanceOfSatisfying(AtractivoModificadoException.class,
                        e -> assertThat(e.getVersionActual()).isEqualTo(5));
        assertThat(atractivos.guardado(ID).getNombre()).isEqualTo("Cascada");
    }

    @Test
    void siOtroGestorEscribeEntreLaLecturaYLaEscrituraEs409() {
        atractivos.agregar(atractivo(1));
        atractivos.antesDeEscribir(() -> atractivos.agregar(GestionAtractivosEnMemoria.conVersion(atractivo(1), 2)));

        assertThatThrownBy(() -> servicio.cambiarEstado(ID, "Cerrado_temporalmente", 1))
                .isInstanceOfSatisfying(AtractivoModificadoException.class,
                        e -> assertThat(e.getVersionActual()).isEqualTo(2));
    }

    @Test
    void unIdInvalidoOInexistenteEs404AntesQueLosDatos() {
        assertThatThrownBy(() -> servicio.actualizar("no-es-un-id", null, null))
                .isInstanceOf(AtractivoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.cambiarEstado(ID, null, null))
                .isInstanceOf(AtractivoNoEncontradoException.class);
    }

    @Test
    void losDatosInvalidosSon400SinEscribir() {
        atractivos.agregar(atractivo(0));

        assertThatThrownBy(() -> servicio.actualizar(ID, borrador(" "), 0))
                .isInstanceOf(ContenidoInvalidoException.class);
        assertThat(atractivos.guardado(ID).getVersion()).isZero();
    }

    private static BorradorAtractivo borrador(String nombre) {
        return new BorradorAtractivo(nombre, "Naturaleza", "Sendero", new BorradorAtractivo.DatosCoordenadas(-74.1, 11.1),
                90, null, "Sin fogatas", "Martes a domingo", List.of("Visual"));
    }

    private static AtractivoPublico atractivo(int version) {
        return new AtractivoPublico(ID, OTRO_GESTOR, "Cascada", new Coordenada(-74.11, 11.15), "Sendero", "Naturaleza",
                List.of("/imagenes/catalogo/cascada-el-salto.webp"), 80, EstadoOperativo.Abierto, null, Set.of(), null,
                Instant.parse("2026-10-01T10:00:00Z"), null, version);
    }
}
