package co.edu.unimagdalena.samaria360.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El cliente HTTP de las rutas no guarda una IP más de 10 s: tras recrear un contenedor con otra IP, el gateway la
 * vuelve a resolver solo (el DNS de Docker responde con TTL de 600 s).
 */
@SpringBootTest
class ResolucionNombresTest {

    @Autowired
    private HttpClient clienteDeLasRutas;

    @Test
    void elClienteDeLasRutasLimitaLaCacheDnsA10Segundos() {
        var resolvedor = clienteDeLasRutas.configuration().getNameResolverProvider();

        assertThat(resolvedor).as("resolvedor configurado por ResolucionNombresConfig").isNotNull();
        assertThat(resolvedor.cacheMaxTimeToLive()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void sinLaConfiguracionReactorNettyRespetariaElTtlDelDns() {
        var porDefecto = HttpClient.create().configuration().getNameResolverProvider();

        // Documenta la causa: por defecto no hay tope (o es mayor que 600 s, el TTL del DNS de Docker).
        assertThat(porDefecto == null || porDefecto.cacheMaxTimeToLive().compareTo(Duration.ofSeconds(600)) > 0)
                .isTrue();
    }
}
