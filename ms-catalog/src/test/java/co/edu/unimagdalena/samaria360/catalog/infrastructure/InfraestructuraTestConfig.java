package co.edu.unimagdalena.samaria360.catalog.infrastructure;

import co.edu.unimagdalena.samaria360.catalog.infrastructure.mongo.MongoDePrueba;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.MongoDBContainer;

/**
 * MongoDB real (mongo:7.0, la imagen de docker-compose) preparado como en Compose: la línea base de
 * infra/mongo/init (validadores, índices y semilla v1) y después las migraciones, con la semilla de demostración. Es
 * un bean: todas las clases con {@link PruebaIntegracion} comparten el contexto y un único contenedor.
 */
@TestConfiguration(proxyBeanMethods = false)
public class InfraestructuraTestConfig {

    @Bean
    @ServiceConnection
    MongoDBContainer contenedorMongo() throws Exception {
        MongoDBContainer mongo = MongoDePrueba.contenedor();
        mongo.start();
        MongoDePrueba.prepararCatalogoCompleto(mongo);
        return mongo;
    }

    /** El JWKS de ms-auth (simulado): ms-catalog lo descarga al validar el primer token (ADR-005, ADR-008). */
    @Bean
    DynamicPropertyRegistrar jwksDePrueba() {
        return registro -> registro.add("samaria360.seguridad.jwk-set-uri", JwksSimulado::uri);
    }
}
