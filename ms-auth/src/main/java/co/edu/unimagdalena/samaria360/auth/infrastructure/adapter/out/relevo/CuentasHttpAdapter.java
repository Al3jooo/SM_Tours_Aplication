package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.relevo;

import co.edu.unimagdalena.samaria360.auth.application.port.out.DestinoCuentasPort;
import co.edu.unimagdalena.samaria360.auth.application.port.out.EntregaFallidaException;
import co.edu.unimagdalena.samaria360.auth.application.port.out.EntregaPendiente;
import co.edu.unimagdalena.samaria360.auth.application.port.out.RelojPort;
import co.edu.unimagdalena.samaria360.auth.application.port.out.TokenServicioPort;
import co.edu.unimagdalena.samaria360.auth.domain.model.Destino;
import co.edu.unimagdalena.samaria360.auth.infrastructure.config.RelevoPropiedades;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Entrega a {@code PUT /internal/v1/cuentas/{id}} de ms-catalog y ms-booking con el JWT de servicio del destino. Tiempos
 * de 0,5 s de conexión y 1,5 s de lectura, como la llamada de ms-booking al catálogo (ADR-007). El arriendo del outbox
 * (30 s) es mayor que el peor caso de una entrega. El cuerpo lleva {@code rnt} solo para un prestador (DEC-146): ms-booking
 * lo ignora y ms-catalog lo guarda en su proyección.
 */
@Component
public class CuentasHttpAdapter implements DestinoCuentasPort {

    private final RestClient cliente;
    private final RelevoPropiedades propiedades;
    private final TokenServicioPort tokens;
    private final RelojPort reloj;

    public CuentasHttpAdapter(RelevoPropiedades propiedades, TokenServicioPort tokens, RelojPort reloj) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofMillis(500));
        fabrica.setReadTimeout(Duration.ofMillis(1500));
        this.cliente = RestClient.builder().requestFactory(fabrica).build();
        this.propiedades = propiedades;
        this.tokens = tokens;
        this.reloj = reloj;
    }

    @Override
    public void enviar(Destino destino, EntregaPendiente entrega) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("rol", entrega.rol().name());
        cuerpo.put("estadoCuenta", entrega.estadoCuenta().name());
        cuerpo.put("version", entrega.version());
        if (entrega.rnt() != null) {
            cuerpo.put("rnt", entrega.rnt());
        }
        try {
            cliente.put()
                    .uri(propiedades.destinos().de(destino) + "/internal/v1/cuentas/{id}", entrega.idUsuario())
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.emitir(destino, reloj.ahora()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException respuesta) {
            throw new EntregaFallidaException(destino.texto() + " respondió " + respuesta.getStatusCode().value(),
                    respuesta);
        } catch (RestClientException error) {
            throw new EntregaFallidaException(destino.texto() + " no respondió: " + error.getMessage(), error);
        }
    }
}
