package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.security;

import co.edu.unimagdalena.samaria360.auth.application.port.out.PasswordHasherPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/** RNF05: contraseñas con BCrypt. */
@Component
public class BCryptPasswordHasherAdapter implements PasswordHasherPort {

    /**
     * Debe coincidir con el costo de los hashes de la semilla y de IniciarSesionService.HASH_FICTICIO;
     * si cambia, el tiempo de respuesta revelaría qué correos existen.
     */
    static final int COSTO = 10;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(COSTO);

    @Override
    public boolean coincide(String plana, String hash) {
        return encoder.matches(plana, hash);
    }
}
