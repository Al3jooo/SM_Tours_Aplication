package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorAtractivo.DatosCoordenadas;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/** CU «Registrar atractivo» (RF06, RF07, RF24, RF27, RF33, RF58; DEC-207 a DEC-214). */
class RegistroAtractivoTest {

    private static final UUID GESTOR = UUID.fromString("00000000-0000-4000-8000-000000000003");
    private static final Instant AHORA = Instant.parse("2026-10-30T15:00:00.123Z");
    private static final String ID = "0000000000000000000000aa";
    static final byte[] JPEG = Arrays.copyOf(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0}, 64);
    static final byte[] PNG = Arrays.copyOf(new byte[]{(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'}, 64);

    @Test
    void registraUnAtractivoPublicadoConSusImagenesEnLaRutaPublicaYVersionCero() {
        RegistroAtractivo.Registro registro = registrar(borrador(), List.of(
                new ImagenSubida("cascada.jpg", JPEG), new ImagenSubida("sendero.png", PNG)));

        AtractivoPublico atractivo = registro.atractivo();
        assertThat(atractivo.getId()).contains(ID);
        assertThat(atractivo.getGestorId()).isEqualTo(GESTOR);
        assertThat(atractivo.getNombre()).isEqualTo("Sendero Cascada El Salto");
        assertThat(atractivo.getCategoria()).contains("Naturaleza");
        assertThat(atractivo.getCoordenada()).isEqualTo(new Coordenada(-74.11, 11.15));
        assertThat(atractivo.getLimiteCapacidadCarga()).isEqualTo(80);
        assertThat(atractivo.getEstadoOperativo()).contains(EstadoOperativo.Abierto);
        assertThat(atractivo.getNormasAmbientales()).contains("Prohibido hacer fogatas.");
        assertThat(atractivo.getHorarioAtencion()).contains("Martes a domingo, 8:00 a 16:00");
        assertThat(atractivo.getCondicionesAccesibilidad()).containsExactly(CondicionAccesibilidad.MovilidadReducida);
        assertThat(atractivo.getFechaRegistro()).contains(AHORA);
        assertThat(atractivo.getFechaActualizacion()).isEmpty();
        assertThat(atractivo.getVersion()).isZero();
        assertThat(registro.imagenes()).extracting(RegistroAtractivo.ImagenPorGuardar::formato)
                .containsExactly(FormatoImagen.JPEG, FormatoImagen.PNG);
        assertThat(atractivo.getImagenes()).containsExactly(
                "/api/v1/catalogo/imagenes/" + registro.imagenes().get(0).id(),
                "/api/v1/catalogo/imagenes/" + registro.imagenes().get(1).id());
    }

    @Test
    void losTextosSeRecortanYLosOpcionalesVaciosQuedanSinValor() {
        BorradorAtractivo borrador = new BorradorAtractivo("  Playa Cristal  ", "Naturaleza", " Aguas claras ",
                new DatosCoordenadas(-74.05, 11.326), 400, "Abierto", "   ", "", null);

        AtractivoPublico atractivo = registrar(borrador, List.of(new ImagenSubida("a.jpg", JPEG))).atractivo();

        assertThat(atractivo.getNombre()).isEqualTo("Playa Cristal");
        assertThat(atractivo.getDescripcion()).contains("Aguas claras");
        assertThat(atractivo.getNormasAmbientales()).isEmpty();
        assertThat(atractivo.getHorarioAtencion()).isEmpty();
        assertThat(atractivo.getCondicionesAccesibilidad()).isEmpty();
    }

    @Test
    void sinDatosNiImagenesCadaObligatorioTieneSuMotivo() {
        Map<String, MotivoInvalido> campos = invalidos(null, List.of());

        assertThat(campos).containsOnly(
                Map.entry("nombre", MotivoInvalido.REQUERIDO),
                Map.entry("categoria", MotivoInvalido.REQUERIDO),
                Map.entry("descripcion", MotivoInvalido.REQUERIDO),
                Map.entry("coordenadas", MotivoInvalido.REQUERIDO),
                Map.entry("limiteCapacidadCarga", MotivoInvalido.REQUERIDO),
                Map.entry("estadoOperativo", MotivoInvalido.REQUERIDO),
                Map.entry("imagenes", MotivoInvalido.REQUERIDO));
    }

    @Test
    void validaVocabulariosRangosYLongitudesConLosNombresJson() {
        BorradorAtractivo borrador = new BorradorAtractivo("x".repeat(121), "Aventura", "d".repeat(1001),
                new DatosCoordenadas(-181.0, 91.0), 100_001, "Cerrado", "n".repeat(1001), "h".repeat(501),
                List.of("Visual", "Braille", "Visual"));

        Map<String, MotivoInvalido> campos = invalidos(borrador, List.of(new ImagenSubida("a.jpg", JPEG)));

        assertThat(campos).containsOnly(
                Map.entry("nombre", MotivoInvalido.LONGITUD_MAXIMA),
                Map.entry("categoria", MotivoInvalido.NO_PERMITIDO),
                Map.entry("descripcion", MotivoInvalido.LONGITUD_MAXIMA),
                Map.entry("coordenadas.longitud", MotivoInvalido.VALOR_MINIMO),
                Map.entry("coordenadas.latitud", MotivoInvalido.VALOR_MAXIMO),
                Map.entry("limiteCapacidadCarga", MotivoInvalido.VALOR_MAXIMO),
                Map.entry("estadoOperativo", MotivoInvalido.NO_PERMITIDO),
                Map.entry("normasAmbientales", MotivoInvalido.LONGITUD_MAXIMA),
                Map.entry("horarioAtencion", MotivoInvalido.LONGITUD_MAXIMA),
                Map.entry("condicionesAccesibilidad[1]", MotivoInvalido.NO_PERMITIDO),
                Map.entry("condicionesAccesibilidad[2]", MotivoInvalido.DUPLICADO));
    }

    @Test
    void losMaximosSeCuentanEnPuntosDeCodigoYLaCapacidadMinimaEsUno() {
        String emoji = "🌴";
        BorradorAtractivo borrador = new BorradorAtractivo(emoji.repeat(120), "Cultura", emoji.repeat(1000),
                new DatosCoordenadas(180.0, -90.0), 1, "En_mantenimiento", emoji.repeat(1000), emoji.repeat(500),
                List.of("MovilidadReducida", "Visual", "Auditiva"));

        AtractivoPublico atractivo = registrar(borrador, List.of(new ImagenSubida("a.jpg", JPEG))).atractivo();

        assertThat(atractivo.getLimiteCapacidadCarga()).isEqualTo(1);
        assertThat(invalidos(con(borrador, 0), List.of(new ImagenSubida("a.jpg", JPEG))))
                .containsOnly(Map.entry("limiteCapacidadCarga", MotivoInvalido.VALOR_MINIMO));
    }

    @Test
    void cadaImagenInvalidaTieneSuMotivoConSuIndice() {
        byte[] grande = Arrays.copyOf(JPEG, 5 * 1024 * 1024 + 1);
        List<ImagenSubida> imagenes = List.of(new ImagenSubida("ok.jpg", JPEG),
                new ImagenSubida("falsa.jpg", "texto".getBytes()), new ImagenSubida("grande.jpg", grande),
                new ImagenSubida("vacia.png", new byte[0]));

        assertThat(invalidos(borrador(), imagenes)).containsOnly(
                Map.entry("imagenes[1]", MotivoInvalido.FORMATO),
                Map.entry("imagenes[2]", MotivoInvalido.TAMANO_MAXIMO),
                Map.entry("imagenes[3]", MotivoInvalido.REQUERIDO));
    }

    @Test
    void masDeCincoImagenesEsCantidadMaximaYNoSeRevisaCadaUna() {
        List<ImagenSubida> seis = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            seis.add(new ImagenSubida(i + ".jpg", "no".getBytes()));
        }

        assertThat(invalidos(borrador(), seis)).containsOnly(Map.entry("imagenes", MotivoInvalido.CANTIDAD_MAXIMA));
    }

    @Test
    void siAlgoEsInvalidoNoSeGeneranIdsDeImagen() {
        AtomicInteger generados = new AtomicInteger();
        Supplier<String> ids = () -> String.format("%024x", generados.incrementAndGet());

        catchThrowableOfType(ContenidoInvalidoException.class, () -> RegistroAtractivo.registrar(
                con(borrador(), null), List.of(new ImagenSubida("a.jpg", JPEG)), ID, ids, GESTOR, AHORA));

        assertThat(generados).hasValue(0);
    }

    // ---- auxiliares ------------------------------------------------------------------------------------------

    static BorradorAtractivo borrador() {
        return new BorradorAtractivo("Sendero Cascada El Salto", "Naturaleza", "Sendero entre bosque húmedo.",
                new DatosCoordenadas(-74.11, 11.15), 80, "Abierto", "Prohibido hacer fogatas.",
                "Martes a domingo, 8:00 a 16:00", List.of("MovilidadReducida"));
    }

    private static BorradorAtractivo con(BorradorAtractivo b, Integer limite) {
        return new BorradorAtractivo(b.nombre(), b.categoria(), b.descripcion(), b.coordenadas(), limite,
                b.estadoOperativo(), b.normasAmbientales(), b.horarioAtencion(), b.condicionesAccesibilidad());
    }

    private static RegistroAtractivo.Registro registrar(BorradorAtractivo borrador, List<ImagenSubida> imagenes) {
        AtomicInteger contador = new AtomicInteger();
        return RegistroAtractivo.registrar(borrador, imagenes, ID,
                () -> String.format("%024x", 0xB00 + contador.incrementAndGet()), GESTOR, AHORA);
    }

    private static Map<String, MotivoInvalido> invalidos(BorradorAtractivo borrador, List<ImagenSubida> imagenes) {
        return catchThrowableOfType(ContenidoInvalidoException.class, () -> registrar(borrador, imagenes))
                .getCampos();
    }
}
