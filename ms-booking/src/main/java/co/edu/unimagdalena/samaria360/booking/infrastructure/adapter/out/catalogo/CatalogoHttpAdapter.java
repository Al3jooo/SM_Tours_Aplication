package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.out.catalogo;

import co.edu.unimagdalena.samaria360.booking.domain.exception.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samaria360.booking.application.port.out.CatalogoPort;
import co.edu.unimagdalena.samaria360.booking.application.port.out.PaginaCatalogo;
import co.edu.unimagdalena.samaria360.booking.domain.model.CategoriaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.OfertaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.UnidadReservable;
import co.edu.unimagdalena.samaria360.booking.infrastructure.config.CatalogoPropiedades;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * ms-catalog por HTTP (D4, ADR-007): el endpoint público de detalle, que solo devuelve servicios Publicados de
 * prestadores activos, y la búsqueda pública para el generador. Sin privilegios por estar en la red interna.
 * <ul>
 *   <li>Timeouts: {@code samaria360.catalogo.timeout-conexion} (500 ms) y {@code timeout-lectura} (1,5 s).</li>
 *   <li>Un reintento, <b>solo</b> ante errores de red o 5xx; nunca ante 404 ni otros 4xx. Va dentro de la llamada
 *   protegida, así que el circuito cuenta una sola llamada lógica.</li>
 *   <li>Circuit breaker {@code catalogo}: un 404 es una respuesta válida («no reservable») y no cuenta como fallo; con
 *   el circuito abierto o el catálogo caído, {@link CatalogoNoDisponibleException} (503).</li>
 * </ul>
 */
@Component
public class CatalogoHttpAdapter implements CatalogoPort {

    static final String CIRCUITO = "catalogo";

    private final RestClient cliente;
    private final CircuitBreaker circuito;

    public CatalogoHttpAdapter(CatalogoPropiedades propiedades, CircuitBreakerFactory<?, ?> circuitos,
                               RestClient.Builder constructor) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(propiedades.timeoutConexion());
        fabrica.setReadTimeout(propiedades.timeoutLectura());
        this.cliente = constructor.baseUrl(propiedades.url()).requestFactory(fabrica).build();
        this.circuito = circuitos.create(CIRCUITO);
    }

    @Override
    public Optional<OfertaReservable> buscarReservable(String idServicio) {
        return protegida(() -> cliente.get().uri("/api/v1/catalogo/servicios/{id}", idServicio)
                .exchange((solicitud, respuesta) -> {
                    if (respuesta.getStatusCode().value() == 404) {
                        return Optional.empty();
                    }
                    verificarEstado(respuesta.getStatusCode());
                    return Optional.of(aOferta(respuesta.bodyTo(DetalleServicioCatalogo.class)));
                }));
    }

    @Override
    public PaginaCatalogo listarPublicados(CategoriaReservable categoria, int pagina) {
        return protegida(() -> cliente.get()
                .uri("/api/v1/catalogo/oferta?categoria={categoria}&pagina={pagina}", categoria.name(), pagina)
                .exchange((solicitud, respuesta) -> {
                    verificarEstado(respuesta.getStatusCode());
                    PaginaOfertaCatalogo cuerpo = respuesta.bodyTo(PaginaOfertaCatalogo.class);
                    List<String> ids = cuerpo == null || cuerpo.elementos() == null ? List.of()
                            : cuerpo.elementos().stream().filter(e -> "Servicio".equals(e.tipo()))
                            .map(PaginaOfertaCatalogo.Elemento::id).toList();
                    return new PaginaCatalogo(ids, cuerpo == null ? 0 : cuerpo.totalPaginas());
                }));
    }

    /** Circuito afuera, reintento adentro. Una respuesta inesperada (4xx) no es indisponibilidad: sube tal cual. */
    private <T> T protegida(Supplier<T> llamada) {
        return circuito.run(() -> conUnReintento(llamada), error -> {
            for (Throwable causa = error; causa != null; causa = causa.getCause()) {
                if (causa instanceof RespuestaInesperadaCatalogoException inesperada) {
                    throw inesperada;
                }
            }
            throw new CatalogoNoDisponibleException("ms-catalog no está disponible", error);
        });
    }

    private static <T> T conUnReintento(Supplier<T> llamada) {
        try {
            return llamada.get();
        } catch (RuntimeException error) {
            if (!transitorio(error)) {
                throw error;
            }
            return llamada.get();
        }
    }

    /** Red (conexión rechazada, tiempo agotado) o 5xx, aunque RestClient los entregue envueltos. */
    private static boolean transitorio(Throwable error) {
        for (Throwable causa = error; causa != null; causa = causa.getCause()) {
            if (causa instanceof RespuestaInesperadaCatalogoException) {
                return false;
            }
            if (causa instanceof ResourceAccessException || causa instanceof ErrorServidorCatalogoException) {
                return true;
            }
        }
        return false;
    }

    private static void verificarEstado(HttpStatusCode estado) {
        if (estado.is5xxServerError()) {
            throw new ErrorServidorCatalogoException(estado.value());
        }
        if (!estado.is2xxSuccessful()) {
            throw new RespuestaInesperadaCatalogoException("ms-catalog respondió " + estado.value());
        }
    }

    private static OfertaReservable aOferta(DetalleServicioCatalogo detalle) {
        if (detalle == null || detalle.id() == null || detalle.categoria() == null) {
            throw new RespuestaInesperadaCatalogoException("El detalle del catálogo no tiene el formato esperado");
        }
        JsonNode datos = detalle.detalleCategoria();
        Integer aforo = null;
        BigDecimal duracion = null;
        List<UnidadReservable> unidades = new ArrayList<>();
        if (datos != null) {
            aforo = datos.hasNonNull("aforoMaxGrupo") ? datos.get("aforoMaxGrupo").asInt() : null;
            duracion = datos.hasNonNull("duracionHoras") ? datos.get("duracionHoras").decimalValue() : null;
            for (JsonNode unidad : datos.path("unidades")) {
                unidades.add(new UnidadReservable(unidad.path("idUnidad").asText(), unidad.path("nombre").asText(),
                        unidad.path("numHabitaciones").asInt(), unidad.path("capacidadMaxHuespedes").asInt(),
                        unidad.path("precioNoche").decimalValue()));
            }
        }
        return new OfertaReservable(detalle.id(), UUID.fromString(detalle.idPrestador()), detalle.nombreComercial(),
                detalle.categoria(), detalle.precioBase(), aforo, duracion, unidades);
    }

    /** Lo que se usa de {@code GET /api/v1/catalogo/servicios/{id}}; los demás campos se ignoran. */
    record DetalleServicioCatalogo(String id, String idPrestador, String nombreComercial, String categoria,
                                   BigDecimal precioBase, JsonNode detalleCategoria) {
    }

    /** Lo que se usa de {@code GET /api/v1/catalogo/oferta}. */
    record PaginaOfertaCatalogo(List<Elemento> elementos, int totalPaginas) {

        record Elemento(String id, String tipo) {
        }
    }
}
