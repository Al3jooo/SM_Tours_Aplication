package co.edu.unimagdalena.samaria360.auth.infrastructure;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * PostgreSQL real (misma imagen que docker-compose) y un par de llaves RSA generado para las pruebas,
 * de modo que no dependen de infra/keys. El contenedor es un bean: todas las clases anotadas con
 * {@link PruebaIntegracion} comparten el contexto en caché y un único contenedor.
 */
@TestConfiguration(proxyBeanMethods = false)
public class InfraestructuraTestConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16-alpine");
    }

    @Bean
    DynamicPropertyRegistrar llavesJwtDePrueba() {
        Path directorio = generarLlaves();
        return registro -> {
            registro.add("samaria360.jwt.llave-privada", () -> directorio.resolve("jwt-privada.pem").toString());
            registro.add("samaria360.jwt.llave-publica", () -> directorio.resolve("jwt-publica.pem").toString());
        };
    }

    private static Path generarLlaves() {
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            KeyPair par = generador.generateKeyPair();
            Path directorio = Files.createTempDirectory("samaria360-jwt-prueba");
            escribirPem(directorio.resolve("jwt-privada.pem"), "PRIVATE KEY", par.getPrivate().getEncoded());
            escribirPem(directorio.resolve("jwt-publica.pem"), "PUBLIC KEY", par.getPublic().getEncoded());
            return directorio;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void escribirPem(Path archivo, String tipo, byte[] der) throws IOException {
        String base64 = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
        Files.writeString(archivo, "-----BEGIN " + tipo + "-----\n" + base64 + "\n-----END " + tipo + "-----\n");
        archivo.toFile().deleteOnExit();
    }
}
