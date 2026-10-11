package co.edu.unimagdalena.samaria360.gateway;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

/** Aislamiento de fallos: con ms-catalog caído, la búsqueda da 503 y el login sigue funcionando. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServicioCaidoTest {

    static final ServicioSimulado AUTH = ServicioSimulado.iniciar("ms-auth").responder(200, "{\"token\":\"t\"}");

    @DynamicPropertySource
    static void destinos(DynamicPropertyRegistry registro) {
        registro.add("AUTH_URI", AUTH::uri);
        registro.add("CATALOG_URI", ServicioSimulado::uriSinServidor);
    }

    @AfterAll
    static void detener() {
        AUTH.close();
    }

    @Autowired
    private WebTestClient cliente;

    @Test
    void conMsCatalogCaidoLaBusquedaResponde503ConProblemDetail() {
        cliente.get().uri("/api/v1/catalogo/oferta?texto=cafe")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectHeader().exists("X-Correlation-Id")
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.codigo").isEqualTo("SERVICIO_NO_DISPONIBLE")
                .jsonPath("$.servicio").isEqualTo("ms-catalog")
                .jsonPath("$.instance").isEqualTo("/api/v1/catalogo/oferta");
    }

    @Test
    void conMsCatalogCaidoElLoginSigueFuncionando() {
        cliente.get().uri("/api/v1/catalogo/oferta").exchange().expectStatus().isEqualTo(503);

        cliente.post().uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"correo\":\"turista@samaria360.test\",\"contrasena\":\"Samaria360*\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.token").isEqualTo("t");
    }
}
