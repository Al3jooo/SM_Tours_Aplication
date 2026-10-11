package co.edu.unimagdalena.samaria360.gateway;

import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servicio HTTP simulado (ms-auth o ms-catalog) sobre Reactor Netty HttpServer, que ya llega con el gateway:
 * no hace falta otra librería. Registra cada petición y responde con el estado, el cuerpo y la demora configurados.
 */
public final class ServicioSimulado implements AutoCloseable {

    /** Petición tal como la recibió el servicio; las cabeceras van en minúsculas. */
    public record Solicitud(String metodo, String uri, Map<String, String> cabeceras, String cuerpo) {

        public String cabecera(String nombre) {
            return cabeceras.get(nombre.toLowerCase(Locale.ROOT));
        }
    }

    private final List<Solicitud> solicitudes = new CopyOnWriteArrayList<>();
    private final DisposableServer servidor;
    private volatile int estado = 200;
    private volatile String cuerpoRespuesta = "{}";
    private volatile Duration demora = Duration.ZERO;

    private ServicioSimulado(String nombre) {
        this.servidor = HttpServer.create()
                .host("127.0.0.1")
                .port(0)
                .handle((solicitud, respuesta) -> solicitud.receive().aggregate().asString(StandardCharsets.UTF_8)
                        .defaultIfEmpty("")
                        .flatMap(cuerpo -> {
                            Map<String, String> cabeceras = new LinkedHashMap<>();
                            solicitud.requestHeaders().forEach(
                                    e -> cabeceras.put(e.getKey().toLowerCase(Locale.ROOT), e.getValue()));
                            solicitudes.add(new Solicitud(solicitud.method().name(), solicitud.uri(), cabeceras, cuerpo));
                            return Mono.delay(demora).then(respuesta.status(estado)
                                    .header("Content-Type", "application/json")
                                    .header("X-Servicio", nombre)
                                    .sendString(Mono.just(cuerpoRespuesta))
                                    .then());
                        }))
                .bindNow();
    }

    public static ServicioSimulado iniciar(String nombre) {
        return new ServicioSimulado(nombre);
    }

    public ServicioSimulado responder(int estado, String cuerpo) {
        this.estado = estado;
        this.cuerpoRespuesta = cuerpo;
        return this;
    }

    public ServicioSimulado demorar(Duration demora) {
        this.demora = demora;
        return this;
    }

    public String uri() {
        return "http://127.0.0.1:" + servidor.port();
    }

    public List<Solicitud> solicitudes() {
        return List.copyOf(solicitudes);
    }

    public Solicitud ultima() {
        return solicitudes.get(solicitudes.size() - 1);
    }

    public void limpiar() {
        solicitudes.clear();
    }

    @Override
    public void close() {
        servidor.disposeNow();
    }

    /** Un puerto donde no escucha nadie: simula un servicio caído (conexión rechazada). */
    public static String uriSinServidor() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return "http://127.0.0.1:" + socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
