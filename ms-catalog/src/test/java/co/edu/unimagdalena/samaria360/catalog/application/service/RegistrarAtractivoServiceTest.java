package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.fake.AlmacenImagenesEnMemoria;
import co.edu.unimagdalena.samaria360.catalog.application.fake.GestionAtractivosEnMemoria;
import co.edu.unimagdalena.samaria360.catalog.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.AtractivoPublico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorAtractivo;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ImagenSubida;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

/** CU «Registrar atractivo» (RF06; DEC-209): orden de validación, guardado e insert, y la compensación (ADR-009). */
class RegistrarAtractivoServiceTest {

    private static final UUID GESTOR = UUID.fromString("00000000-0000-4000-8000-000000000003");
    private static final byte[] JPEG = Arrays.copyOf(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}, 32);

    private final GestionAtractivosEnMemoria atractivos = new GestionAtractivosEnMemoria();
    private final AlmacenImagenesEnMemoria imagenes = new AlmacenImagenesEnMemoria();
    private final RelojFijo reloj = new RelojFijo(Instant.parse("2026-10-30T15:00:00.123456Z"));
    private final RegistrarAtractivoService servicio = new RegistrarAtractivoService(atractivos, imagenes, reloj);

    @Test
    void guardaLasImagenesEInsertaElAtractivoConLaFechaEnMilisegundos() {
        AtractivoPublico atractivo = servicio.registrar(GESTOR, borrador(), dos());

        assertThat(atractivos.todos()).containsExactly(atractivo);
        assertThat(imagenes.ids()).hasSize(2);
        assertThat(atractivo.getImagenes()).allSatisfy(ruta -> assertThat(imagenes.ids())
                .contains(ruta.substring("/api/v1/catalogo/imagenes/".length())));
        assertThat(atractivo.getFechaRegistro()).contains(Instant.parse("2026-10-30T15:00:00.123Z"));
        assertThat(atractivo.getGestorId()).isEqualTo(GESTOR);
    }

    @Test
    void conDatosInvalidosNoSeEscribeNada() {
        assertThatThrownBy(() -> servicio.registrar(GESTOR, null, dos()))
                .isInstanceOf(ContenidoInvalidoException.class);

        assertThat(atractivos.todos()).isEmpty();
        assertThat(imagenes.ids()).isEmpty();
    }

    /** C4: si el insert falla, se borran las imágenes ya guardadas y llega el error original. */
    @Test
    void siElInsertFallaSeBorranLasImagenesGuardadas() {
        IllegalStateException caida = new IllegalStateException("insert falló");
        atractivos.fallarAlInsertar(caida);

        assertThat(catchThrowable(() -> servicio.registrar(GESTOR, borrador(), dos()))).isSameAs(caida);

        assertThat(imagenes.ids()).isEmpty();
        assertThat(atractivos.todos()).isEmpty();
    }

    @Test
    void siFallaLaSegundaImagenSeBorraLaPrimeraYNoSeInserta() {
        IllegalStateException caida = new IllegalStateException("GridFS falló");
        imagenes.fallarAlGuardarLaNumero(2, caida);

        assertThat(catchThrowable(() -> servicio.registrar(GESTOR, borrador(), dos()))).isSameAs(caida);

        assertThat(imagenes.ids()).isEmpty();
        assertThat(atractivos.todos()).isEmpty();
    }

    /** Riesgo de ADR-009: si el borrado también falla, queda un huérfano y el error se conserva como suprimido. */
    @Test
    void unBorradoQueFallaQuedaComoSuprimidoSinOcultarElErrorOriginal() {
        IllegalStateException caida = new IllegalStateException("insert falló");
        IllegalStateException borrado = new IllegalStateException("borrado falló");
        atractivos.fallarAlInsertar(caida);
        imagenes.fallarAlBorrar(borrado);

        Throwable error = catchThrowable(() -> servicio.registrar(GESTOR, borrador(), dos()));

        assertThat(error).isSameAs(caida);
        assertThat(error.getSuppressed()).containsExactly(borrado, borrado);
        assertThat(imagenes.ids()).hasSize(2);
    }

    private static List<ImagenSubida> dos() {
        return List.of(new ImagenSubida("a.jpg", JPEG), new ImagenSubida("b.jpg", JPEG));
    }

    private static BorradorAtractivo borrador() {
        return new BorradorAtractivo("Playa Cristal", "Naturaleza", "Aguas transparentes",
                new BorradorAtractivo.DatosCoordenadas(-74.05, 11.326), 400, "Abierto", null, null, List.of());
    }
}
