package co.edu.unimagdalena.samaria360.catalog.infrastructure.mongo;

import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.MountableFile;

import java.nio.file.Path;
import java.util.List;

/**
 * MongoDB de las pruebas: mongo:7.0 (la imagen de docker-compose) con los scripts de infra/mongo copiados dentro, y
 * el mismo camino que en Compose: la línea base congelada (init/01, 02 y 03) y después las migraciones con
 * aplicar.js, como el servicio mongo-migraciones.
 */
public final class MongoDePrueba {

    static final Path INIT = Path.of("../infra/mongo/init");
    static final Path MIGRACIONES = Path.of("../infra/mongo/migraciones");
    static final Path RECURSOS = Path.of("src/test/resources/mongo");
    public static final List<String> LINEA_BASE = List.of("01-validadores.js", "02-indices.js", "03-semilla-catalogo.js");

    private MongoDePrueba() {
    }

    /** Sin iniciar: /scripts (init), /migraciones y /recursos (archivos de prueba). */
    public static MongoDBContainer contenedor() {
        return new MongoDBContainer("mongo:7.0")
                .withCopyFileToContainer(MountableFile.forHostPath(INIT), "/scripts")
                .withCopyFileToContainer(MountableFile.forHostPath(MIGRACIONES), "/migraciones")
                .withCopyFileToContainer(MountableFile.forHostPath(RECURSOS), "/recursos");
    }

    /** Ejecuta un script de mongosh dentro del contenedor (p. ej. «/scripts/01-validadores.js»). */
    public static ExecResult ejecutar(MongoDBContainer mongo, String ruta) throws Exception {
        return mongo.execInContainer("mongosh", "--quiet", ruta);
    }

    /** Ejecuta los scripts indicados de /scripts, en orden, y falla si alguno termina con error. */
    public static void ejecutarScripts(MongoDBContainer mongo, List<String> scripts) throws Exception {
        for (String script : scripts) {
            exigirExito(script, ejecutar(mongo, "/scripts/" + script));
        }
    }

    /** aplicar.js, como el servicio mongo-migraciones; con {@code semilla}, también migraciones/semilla. */
    public static ExecResult migrar(MongoDBContainer mongo, boolean semilla) throws Exception {
        return mongo.execInContainer("sh", "-c",
                "MONGO_SEMILLA=" + semilla + " mongosh --quiet /migraciones/aplicar.js");
    }

    /** Volumen nuevo completo: línea base y todas las migraciones, con la semilla de demostración. */
    public static void prepararCatalogoCompleto(MongoDBContainer mongo) throws Exception {
        ejecutarScripts(mongo, LINEA_BASE);
        exigirExito("aplicar.js", migrar(mongo, true));
    }

    public static void exigirExito(String nombre, ExecResult resultado) {
        if (resultado.getExitCode() != 0) {
            throw new IllegalStateException(nombre + " falló:\n" + resultado.getStdout() + resultado.getStderr());
        }
    }
}
