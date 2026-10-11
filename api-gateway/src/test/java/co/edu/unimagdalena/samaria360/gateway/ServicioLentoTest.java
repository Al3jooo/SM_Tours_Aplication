package co.edu.unimagdalena.samaria360.gateway;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** Con ms-catalog lento (7 s), el timeout de respuesta de 5 s corta y responde 503; el login no espera. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ServicioLentoTest {

    static final ServicioSimulado AUTH = ServicioSimulado.iniciar("ms-auth").responder(200, "{\"token\":\"t\"}");
    static final ServicioSimulado CATALOGO = ServicioSimulado.iniciar("ms-catalog")
            .responder(200, "{\"elementos\":[]}")
            .demorar(Duration.ofSeconds(7));

    @DynamicPropertySource
    static void destinos(DynamicPropertyRegistry registro) {
        registro.add("AUTH_URI", AUTH::uri);
        registro.add("CATALOG_URI", CATALOGO::uri);
    }

    @AfterAll
    static void detener() {
        AUTH.close();
        CATALOGO.close();
    }

    @Autowired
    private WebTestClient cliente;

    @Test
    void unServicioLentoRecibe503AlCumplirseElTimeoutDeRespuesta() {
        long inicio = System.nanoTime();

        cliente.mutate().responseTimeout(Duration.ofSeconds(15)).build()
                .get().uri("/api/v1/catalogo/oferta")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.codigo").isEqualTo("SERVICIO_NO_DISPONIBLE")
                .jsonPath("$.servicio").isEqualTo("ms-catalog");

        Duration transcurrido = Duration.ofNanos(System.nanoTime() - inicio);
        assertThat(transcurrido).isBetween(Duration.ofMillis(4_900), Duration.ofMillis(6_500));
    }

    @Test
    void elLoginRespondeAunqueElCatalogoEsteLento() {
        long inicio = System.nanoTime();

        cliente.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).bodyValue("{}")
                .exchange()
                .expectStatus().isOk();

        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofSeconds(2));
    }
}
