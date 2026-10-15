package co.edu.unimagdalena.samaria360.booking.infrastructure;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * PostgreSQL real (postgres:16-alpine, la imagen de docker-compose: NULLS NOT DISTINCT y los bloqueos de fila de
 * verdad), ms-catalog y el JWKS de ms-auth simulados, y un reloj ajustable. Todo son beans: las clases con
 * {@link PruebaIntegracion} comparten el contexto, el contenedor y el catálogo simulado.
 */
@TestConfiguration(proxyBeanMethods = false)
public class InfraestructuraTestConfig {

    static final CatalogoSimulado CATALOGO = CatalogoSimulado.iniciar();

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }

    @Bean
    CatalogoSimulado catalogoSimulado() {
        return CATALOGO;
    }

    @Bean
    DynamicPropertyRegistrar destinosSimulados() {
        return registro -> {
            registro.add("samaria360.catalogo.url", CATALOGO::url);
            registro.add("samaria360.seguridad.jwk-set-uri",
                    () -> CATALOGO.url() + "/api/v1/auth/.well-known/jwks.json");
        };
    }

    /** Reemplaza al reloj del sistema (es un RelojPort). */
    @Bean
    @Primary
    RelojAjustable relojAjustable() {
        return new RelojAjustable();
    }
}
