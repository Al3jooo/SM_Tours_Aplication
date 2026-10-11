package co.edu.unimagdalena.samaria360.catalog.infrastructure;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;
import java.util.List;

/**
 * MongoDB real (mongo:7.0, la imagen de docker-compose) con los scripts de infra/mongo/init ya ejecutados:
 * validadores, índices y semilla. Es un bean: todas las clases con {@link PruebaIntegracion} comparten el contexto
 * y un único contenedor.
 */
@TestConfiguration(proxyBeanMethods = false)
public class InfraestructuraTestConfig {

    private static final List<String> SCRIPTS = List.of("01-validadores.js", "02-indices.js", "03-semilla-catalogo.js");

    @Bean
    @ServiceConnection
    MongoDBContainer contenedorMongo() throws Exception {
        MongoDBContainer mongo = new MongoDBContainer("mongo:7.0")
                .withCopyFileToContainer(MountableFile.forHostPath(Path.of("../infra/mongo/init")), "/scripts");
        mongo.start();
        for (String script : SCRIPTS) {
            ExecResult resultado = mongo.execInContainer("mongosh", "--quiet", "/scripts/" + script);
            if (resultado.getExitCode() != 0) {
                throw new IllegalStateException(script + " falló: " + resultado.getStderr());
            }
        }
        return mongo;
    }
}
