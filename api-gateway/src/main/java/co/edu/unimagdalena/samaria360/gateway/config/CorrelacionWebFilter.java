package co.edu.unimagdalena.samaria360.gateway.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * {@code X-Correlation-Id}: lo genera si no viene (o si no es válido), lo reenvía al servicio y lo devuelve en la
 * respuesta, para seguir una petición en los logs de todos los servicios. Es un WebFilter (no un GlobalFilter) para
 * cubrir también los 404, los preflight de CORS y los fallbacks.
 */
@Component
public class CorrelacionWebFilter implements WebFilter, Ordered {

    public static final String CABECERA = "X-Correlation-Id";

    /** Ruta pedida por el cliente, antes del forward interno al fallback (para el {@code instance} del 503). */
    public static final String ATRIBUTO_RUTA_ORIGINAL = CorrelacionWebFilter.class.getName() + ".rutaOriginal";

    /** Solo valores seguros para escribir en logs: evita inyección de líneas y valores desmedidos. */
    private static final Pattern VALIDO = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");
    private static final Logger LOG = LoggerFactory.getLogger(CorrelacionWebFilter.class);

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String recibido = exchange.getRequest().getHeaders().getFirst(CABECERA);
        String id = recibido != null && VALIDO.matcher(recibido).matches() ? recibido : UUID.randomUUID().toString();

        ServerWebExchange conId = exchange.mutate().request(r -> r.headers(h -> h.set(CABECERA, id))).build();
        conId.getAttributes().putIfAbsent(ATRIBUTO_RUTA_ORIGINAL, exchange.getRequest().getPath().value());
        conId.getResponse().beforeCommit(() -> {
            conId.getResponse().getHeaders().set(CABECERA, id);
            return Mono.empty();
        });

        long inicio = System.nanoTime();
        return chain.filter(conId).doFinally(senal -> LOG.info("{} {} -> {} ({} ms) correlacion={}",
                conId.getRequest().getMethod(), conId.getRequest().getPath().value(),
                conId.getResponse().getStatusCode(), (System.nanoTime() - inicio) / 1_000_000, id));
    }
}
