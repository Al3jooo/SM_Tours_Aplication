package co.edu.unimagdalena.samaria360.gateway.config;

import org.springframework.cloud.gateway.config.HttpClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Caché DNS corta para los destinos de las rutas.
 *
 * <p>El cliente HTTP del gateway (Reactor Netty) resuelve los nombres con su propio resolvedor, que guarda cada
 * respuesta el tiempo que indica su TTL. El DNS interno de Docker responde con TTL de 600 s: si un servicio se recrea
 * ({@code docker compose up -d --build --no-deps ms-catalog}) y su contenedor recibe otra IP, el gateway seguía
 * enviando a la IP vieja hasta 10 minutos, con 503 aunque el servicio estuviera sano. Con un máximo de 10 s (el mismo
 * {@code valid=10s} del resolvedor de nginx, frontend/nginx/default.conf) se recupera solo, sin reiniciar el gateway.
 * Resolver de nuevo cada 10 s en la red de Docker no tiene costo apreciable.
 */
@Configuration
public class ResolucionNombresConfig {

    static final Duration TTL_MAXIMO_DNS = Duration.ofSeconds(10);

    @Bean
    public HttpClientCustomizer cacheDnsCorta() {
        return cliente -> cliente.resolver(resolvedor -> resolvedor.cacheMaxTimeToLive(TTL_MAXIMO_DNS));
    }
}
