package co.edu.unimagdalena.samaria360.gateway;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * C3 de la iteración E (DEC-209, ADR-009): el registro de un atractivo con una imagen de 5 MB pasa por el gateway hasta
 * ms-catalog sin cambios, byte a byte. El gateway no lee ni limita el cuerpo; el tope lo ponen nginx (30 MB) y
 * ms-catalog. El ms-catalog simulado guarda el cuerpo binario y su {@code Content-Type} (el {@link ServicioSimulado}
 * común lo guarda como texto).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransferenciaImagenTest {

    private static final int CINCO_MB = 5 * 1024 * 1024;
    private static final String LIMITE = "----samaria360-gateway";
    private static final AtomicReference<byte[]> RECIBIDO = new AtomicReference<>();
    private static final AtomicReference<String> TIPO_RECIBIDO = new AtomicReference<>();
    private static final AtomicReference<String> AUTORIZACION_RECIBIDA = new AtomicReference<>();
    private static final AtomicReference<String> RUTA_RECIBIDA = new AtomicReference<>();

    static final DisposableServer CATALOGO = HttpServer.create()
            .host("127.0.0.1")
            .port(0)
            .handle((solicitud, respuesta) -> solicitud.receive().aggregate().asByteArray()
                    .defaultIfEmpty(new byte[0])
                    .flatMap(cuerpo -> {
                        RECIBIDO.set(cuerpo);
                        TIPO_RECIBIDO.set(solicitud.requestHeaders().get(HttpHeaders.CONTENT_TYPE));
                        AUTORIZACION_RECIBIDA.set(solicitud.requestHeaders().get(HttpHeaders.AUTHORIZATION));
                        RUTA_RECIBIDA.set(solicitud.uri());
                        return respuesta.status(201).header("Content-Type", "application/json")
                                .sendString(Mono.just("{\"sha256\":\"" + sha256(cuerpo) + "\"}")).then();
                    }))
            .bindNow();

    @DynamicPropertySource
    static void destinos(DynamicPropertyRegistry registro) {
        registro.add("AUTH_URI", ServicioSimulado::uriSinServidor);
        registro.add("CATALOG_URI", () -> "http://127.0.0.1:" + CATALOGO.port());
        registro.add("BOOKING_URI", ServicioSimulado::uriSinServidor);
    }

    @AfterAll
    static void detener() {
        CATALOGO.disposeNow();
    }

    @Autowired
    private WebTestClient cliente;

    @Test
    void unRegistroConUnaImagenDeCincoMegabytesLlegaIntegroAMsCatalog() {
        byte[] imagen = new byte[CINCO_MB];
        new Random(42).nextBytes(imagen);
        imagen[0] = (byte) 0xFF;
        imagen[1] = (byte) 0xD8;
        imagen[2] = (byte) 0xFF;
        byte[] cuerpo = multipart(imagen);

        cliente.mutate().responseTimeout(Duration.ofSeconds(20)).build()
                .post().uri("/api/v1/catalogo/gestor/atractivos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer token-del-gestor")
                .header(HttpHeaders.CONTENT_TYPE, "multipart/form-data; boundary=" + LIMITE)
                .bodyValue(cuerpo)
                .exchange()
                .expectStatus().isCreated()
                .expectBody().jsonPath("$.sha256").isEqualTo(sha256(cuerpo));

        assertThat(RECIBIDO.get()).hasSize(cuerpo.length).isEqualTo(cuerpo);
        assertThat(TIPO_RECIBIDO.get()).isEqualTo("multipart/form-data; boundary=" + LIMITE);
        assertThat(AUTORIZACION_RECIBIDA.get()).isEqualTo("Bearer token-del-gestor");
        assertThat(RUTA_RECIBIDA.get()).isEqualTo("/api/v1/catalogo/gestor/atractivos");
    }

    /** La parte {@code datos} (JSON) y una parte {@code imagenes}, como las arma el navegador con FormData. */
    private static byte[] multipart(byte[] imagen) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        escribir(salida, "--" + LIMITE + "\r\nContent-Disposition: form-data; name=\"datos\"\r\n"
                + "Content-Type: application/json\r\n\r\n{\"nombre\":\"Cascada\"}\r\n");
        escribir(salida, "--" + LIMITE + "\r\nContent-Disposition: form-data; name=\"imagenes\"; filename=\"a.jpg\"\r\n"
                + "Content-Type: image/jpeg\r\n\r\n");
        salida.writeBytes(imagen);
        escribir(salida, "\r\n--" + LIMITE + "--\r\n");
        return salida.toByteArray();
    }

    private static void escribir(ByteArrayOutputStream salida, String texto) {
        salida.writeBytes(texto.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(byte[] datos) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(datos));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
