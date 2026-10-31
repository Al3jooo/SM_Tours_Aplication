package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.infrastructure.InfraestructuraTestConfig;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.FIRMA_JPEG;
import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.datos;
import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.imagen;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Los topes de {@code spring.servlet.multipart} con un Tomcat real (MockMvc no los aplica; ADR-009, DEC-209): una
 * imagen de 5 MB exactos se registra y una parte de más de 6 MB es 413 CARGA_DEMASIADO_GRANDE, con respuesta (no una
 * conexión cortada) gracias a {@code server.tomcat.max-swallow-size}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(InfraestructuraTestConfig.class)
class CargaMultipartServidorTest {

    private static final int CINCO_MB = 5 * 1024 * 1024;
    private static final String LIMITE = "----samaria360-prueba";

    @LocalServerPort
    private int puerto;

    @Autowired
    private MongoTemplate mongo;

    private final UUID gestor = UUID.randomUUID();
    private final HttpClient cliente = HttpClient.newHttpClient();

    @BeforeEach
    void preparar() {
        SolicitudesAtractivo.filaDeCuenta(mongo.getDb(), gestor, "Gestor", 1);
    }

    @AfterEach
    void limpiar() {
        SolicitudesAtractivo.limpiar(mongo.getDb(), gestor);
    }

    @Test
    void unaImagenDeCincoMegabytesExactosPasaPorTomcatYSeRegistra() throws Exception {
        HttpResponse<String> respuesta = enviar(imagen(FIRMA_JPEG, CINCO_MB));

        assertThat(respuesta.statusCode()).as(respuesta.body()).isEqualTo(201);
        String ruta = JsonPath.read(respuesta.body(), "$.imagenes[0]");
        HttpResponse<byte[]> servida = cliente.send(HttpRequest.newBuilder(uri(ruta)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertThat(servida.body()).hasSize(CINCO_MB);
    }

    @Test
    void unaParteDeMasDeSeisMegabytesEs413CargaDemasiadoGrande() throws Exception {
        HttpResponse<String> respuesta = enviar(imagen(FIRMA_JPEG, 7 * 1024 * 1024));

        assertThat(respuesta.statusCode()).isEqualTo(413);
        assertThat(JsonPath.<String>read(respuesta.body(), "$.codigo")).isEqualTo("CARGA_DEMASIADO_GRANDE");
        assertThat(mongo.getCollection("atractivos").countDocuments(
                new org.bson.Document("gestorId", gestor.toString()))).isZero();
    }

    private HttpResponse<String> enviar(byte[] contenido) throws Exception {
        HttpRequest solicitud = HttpRequest.newBuilder(uri(SolicitudesAtractivo.RUTA))
                .header("Authorization", SolicitudesAtractivo.tokenGestor(gestor, 1))
                .header("Content-Type", "multipart/form-data; boundary=" + LIMITE)
                .POST(HttpRequest.BodyPublishers.ofByteArray(cuerpo(contenido)))
                .build();
        return cliente.send(solicitud, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    /** Multipart a mano: la parte {@code datos} (JSON) y una parte {@code imagenes}. */
    private static byte[] cuerpo(byte[] imagen) throws Exception {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        String datos = SolicitudesServicio.json(datos("Atractivo multipart " + UUID.randomUUID()));
        escribir(salida, "--" + LIMITE + "\r\nContent-Disposition: form-data; name=\"datos\"\r\n"
                + "Content-Type: application/json\r\n\r\n" + datos + "\r\n");
        escribir(salida, "--" + LIMITE + "\r\nContent-Disposition: form-data; name=\"imagenes\"; filename=\"a.jpg\"\r\n"
                + "Content-Type: image/jpeg\r\n\r\n");
        salida.write(imagen);
        escribir(salida, "\r\n--" + LIMITE + "--\r\n");
        return salida.toByteArray();
    }

    private static void escribir(ByteArrayOutputStream salida, String texto) {
        salida.writeBytes(texto.getBytes(StandardCharsets.UTF_8));
    }

    private URI uri(String ruta) {
        return URI.create("http://localhost:" + puerto + ruta);
    }
}
