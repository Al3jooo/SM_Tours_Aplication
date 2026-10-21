package co.edu.unimagdalena.samaria360.auth.application.fake;

import co.edu.unimagdalena.samaria360.auth.application.port.out.TokenEmitido;
import co.edu.unimagdalena.samaria360.auth.application.port.out.TokenProviderPort;
import co.edu.unimagdalena.samaria360.auth.domain.model.Usuario;

import java.time.Duration;
import java.time.Instant;

public class TokenProviderFalso implements TokenProviderPort {

    public static final Duration VIGENCIA = Duration.ofMinutes(15);

    private int llamadas;

    @Override
    public TokenEmitido emitir(Usuario usuario, Instant ahora) {
        llamadas++;
        return new TokenEmitido("token-" + usuario.getId(), ahora.plus(VIGENCIA));
    }

    public int llamadas() {
        return llamadas;
    }
}
