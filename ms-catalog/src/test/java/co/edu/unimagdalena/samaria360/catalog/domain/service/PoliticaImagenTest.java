package co.edu.unimagdalena.samaria360.catalog.domain.service;

import co.edu.unimagdalena.samaria360.catalog.domain.model.FormatoImagen;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ImagenSubida;
import co.edu.unimagdalena.samaria360.catalog.domain.model.MotivoInvalido;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/** RF06, DEC-209: de 1 a 5 imágenes, JPEG o PNG por su firma y 5 MB como máximo cada una. */
class PoliticaImagenTest {

    private static final byte[] FIRMA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
    private static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    @Test
    void laCantidadVaDeUnaACinco() {
        assertThat(PoliticaImagen.motivoCantidad(0)).contains(MotivoInvalido.REQUERIDO);
        assertThat(PoliticaImagen.motivoCantidad(1)).isEmpty();
        assertThat(PoliticaImagen.motivoCantidad(5)).isEmpty();
        assertThat(PoliticaImagen.motivoCantidad(6)).contains(MotivoInvalido.CANTIDAD_MAXIMA);
    }

    @Test
    void reconoceJpegYPngPorSuFirmaYNoPorElNombre() {
        assertThat(PoliticaImagen.motivo(new ImagenSubida("foto.png", con(FIRMA_JPEG, 100)))).isEmpty();
        assertThat(PoliticaImagen.motivo(new ImagenSubida("foto.jpg", con(FIRMA_PNG, 100)))).isEmpty();
        assertThat(FormatoImagen.detectar(con(FIRMA_JPEG, 10))).contains(FormatoImagen.JPEG);
        assertThat(FormatoImagen.detectar(con(FIRMA_PNG, 10))).contains(FormatoImagen.PNG);
    }

    @Test
    void unArchivoQueNoEsJpegNiPngEsDeFormatoInvalido() {
        byte[] gif = "GIF89a....".getBytes(StandardCharsets.US_ASCII);
        byte[] pngCortado = Arrays.copyOf(FIRMA_PNG, 4);

        assertThat(PoliticaImagen.motivo(new ImagenSubida("a.jpg", gif))).contains(MotivoInvalido.FORMATO);
        assertThat(PoliticaImagen.motivo(new ImagenSubida("b.png", pngCortado))).contains(MotivoInvalido.FORMATO);
        assertThat(FormatoImagen.detectar(null)).isEmpty();
    }

    @Test
    void cincoMegabytesExactosSirvenYUnByteMasNo() {
        int tope = (int) PoliticaImagen.TAMANO_MAXIMO;

        assertThat(tope).isEqualTo(5 * 1024 * 1024);
        assertThat(PoliticaImagen.motivo(new ImagenSubida("ok.jpg", con(FIRMA_JPEG, tope)))).isEmpty();
        assertThat(PoliticaImagen.motivo(new ImagenSubida("grande.jpg", con(FIRMA_JPEG, tope + 1))))
                .contains(MotivoInvalido.TAMANO_MAXIMO);
    }

    @Test
    void unaParteSinContenidoEsRequerida() {
        assertThat(PoliticaImagen.motivo(new ImagenSubida("vacia.jpg", new byte[0]))).contains(MotivoInvalido.REQUERIDO);
        assertThat(PoliticaImagen.motivo(new ImagenSubida("nula.jpg", null))).contains(MotivoInvalido.REQUERIDO);
        assertThat(PoliticaImagen.motivo(null)).contains(MotivoInvalido.REQUERIDO);
    }

    private static byte[] con(byte[] firma, int tamano) {
        return Arrays.copyOf(firma, tamano);
    }
}
