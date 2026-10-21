package co.edu.unimagdalena.samaria360.auth.application.port.out;

import co.edu.unimagdalena.samaria360.auth.domain.model.Destino;

import java.time.Instant;

/** JWT de servicio de ms-auth para un destino: {@code rol = SERVICIO}, {@code aud} = el destino, vigencia de 5 min. */
public interface TokenServicioPort {

    String emitir(Destino destino, Instant ahora);
}
