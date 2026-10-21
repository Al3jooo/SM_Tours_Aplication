package co.edu.unimagdalena.samaria360.gateway.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.UriUtils;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Rechaza con 400 {@code RUTA_INVALIDA}, antes de enrutar, una ruta que no está normalizada: con segmentos {@code .} o
 * {@code ..} (en la ruta cruda o en la decodificada), una barra invertida, o {@code %2e}, {@code %2f} o {@code %5c}. El
 * gateway reenvía la ruta sin reescribirla, así que una como {@code /api/v1/reservas/../../internal/...} llegaría tal
 * cual al servicio (ADR-008: {@code /internal/**} no se expone). Un segmento que solo empieza con punto, como
 * {@code .well-known}, es válido. Corre después de {@link CorrelacionWebFilter}: el 400 lleva {@code X-Correlation-Id}.
 */
@Component
public class RutaSeguraWebFilter implements WebFilter, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(RutaSeguraWebFilter.class);
    private static final String[] CODIFICADOS_PROHIBIDOS = {"%2e", "%2f", "%5c"};

    private final ObjectMapper json;

    public RutaSeguraWebFilter(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String cruda = exchange.getRequest().getURI().getRawPath();
        if (!esInvalida(cruda)) {
            return chain.filter(exchange);
        }
        LOG.warn("Ruta no normalizada rechazada con 400 RUTA_INVALIDA correlacion={}",
                exchange.getRequest().getHeaders().getFirst(CorrelacionWebFilter.CABECERA));
        return rechazar(exchange.getResponse());
    }

    static boolean esInvalida(String cruda) {
        if (cruda == null || cruda.isEmpty()) {
            return false;
        }
        String minusculas = cruda.toLowerCase(Locale.ROOT);
        for (String prohibido : CODIFICADOS_PROHIBIDOS) {
            if (minusculas.contains(prohibido)) {
                return true;
            }
        }
        String decodificada;
        try {
            decodificada = UriUtils.decode(cruda, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException malCodificada) {
            return true;
        }
        return tienePuntosOBarraInvertida(cruda) || tienePuntosOBarraInvertida(decodificada);
    }

    private static boolean tienePuntosOBarraInvertida(String ruta) {
        if (ruta.indexOf('\\') >= 0) {
            return true;
        }
        for (String segmento : ruta.split("/", -1)) {
            if (segmento.equals(".") || segmento.equals("..")) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> rechazar(ServerHttpResponse respuesta) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "La ruta no es válida");
        problema.setProperty("codigo", "RUTA_INVALIDA");
        byte[] cuerpo;
        try {
            cuerpo = json.writeValueAsBytes(problema);
        } catch (JsonProcessingException e) {
            cuerpo = "{\"status\":400,\"codigo\":\"RUTA_INVALIDA\"}".getBytes(StandardCharsets.UTF_8);
        }
        respuesta.setStatusCode(HttpStatus.BAD_REQUEST);
        respuesta.getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);
        DataBuffer buffer = respuesta.bufferFactory().wrap(cuerpo);
        return respuesta.writeWith(Mono.just(buffer));
    }
}
