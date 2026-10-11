package co.edu.unimagdalena.samaria360.gateway;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Con ms-catalog respondiendo 503, tras 5 llamadas fallidas (mínimo de la ventana, 100 % de fallos) el circuito de
 * la ruta se abre: las llamadas siguientes reciben el fallback sin llegar al servicio. El circuito es por ruta.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CircuitoAbiertoTest {

    static final ServicioSimulado AUTH = ServicioSimulado.iniciar("ms-auth").responder(200, "{\"token\":\"t\"}");
    static final ServicioSimulado CATALOGO = ServicioSimulado.iniciar("ms-catalog").responder(503, "{}");

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
    void trasCincoFallosElCircuitoSeAbreYNoSeLlamaAlServicio() {
        for (int i = 0; i < 5; i++) {
            buscarYEsperarFallback();
        }
        assertThat(CATALOGO.solicitudes()).hasSize(5);

        for (int i = 0; i < 3; i++) {
            buscarYEsperarFallback();
        }
        // Circuito abierto: el gateway responde sin consultar a ms-catalog.
        assertThat(CATALOGO.solicitudes()).hasSize(5);

        // El circuito de auth es otro: el login no se ve afectado.
        cliente.post().uri("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).bodyValue("{}")
                .exchange()
                .expectStatus().isOk();
    }

    private void buscarYEsperarFallback() {
        cliente.get().uri("/api/v1/catalogo/oferta")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody().jsonPath("$.codigo").isEqualTo("SERVICIO_NO_DISPONIBLE");
    }
}
