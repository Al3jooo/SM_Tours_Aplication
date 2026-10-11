package co.edu.unimagdalena.samaria360.auth.application.port.out;

import co.edu.unimagdalena.samaria360.auth.domain.model.Usuario;

import java.time.Instant;

/** Emisión del token de sesión (JWT RS256 con claims {@code sub}, {@code rol} y {@code exp}). */
public interface TokenProviderPort {

    TokenEmitido emitir(Usuario usuario, Instant ahora);
}
