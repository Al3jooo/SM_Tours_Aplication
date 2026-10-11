package co.edu.unimagdalena.samaria360.auth.application.service;

import co.edu.unimagdalena.samaria360.auth.application.port.in.ComandoInicioSesion;
import co.edu.unimagdalena.samaria360.auth.application.port.in.IniciarSesionUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.in.SesionIniciada;
import co.edu.unimagdalena.samaria360.auth.application.port.out.PasswordHasherPort;
import co.edu.unimagdalena.samaria360.auth.application.port.out.RelojPort;
import co.edu.unimagdalena.samaria360.auth.application.port.out.TokenEmitido;
import co.edu.unimagdalena.samaria360.auth.application.port.out.TokenProviderPort;
import co.edu.unimagdalena.samaria360.auth.application.port.out.UsuarioRepositoryPort;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CredencialesInvalidasException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaBloqueadaException;
import co.edu.unimagdalena.samaria360.auth.domain.model.Usuario;
import co.edu.unimagdalena.samaria360.auth.domain.service.PoliticaBloqueo;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Caso de uso Iniciar sesión (RF15). La transacción la abre la infraestructura.
 */
public class IniciarSesionService implements IniciarSesionUseCase {

    /**
     * Hash BCrypt real de costo 10 (igual que el de las cuentas) de una contraseña aleatoria descartada.
     * Se compara cuando el correo no existe para que el tiempo de respuesta no revele qué correos están
     * registrados; un valor que no fuera BCrypt válido se rechazaría sin calcular nada.
     */
    static final String HASH_FICTICIO = "$2a$10$T6zsF.mE8.NoxkZi4SQbPuIQ9h3uxhsiB2.08ghsaFTuZ05kifUaS";

    private final UsuarioRepositoryPort usuarios;
    private final PasswordHasherPort hasher;
    private final TokenProviderPort tokens;
    private final RelojPort reloj;
    private final PoliticaBloqueo politica;

    public IniciarSesionService(UsuarioRepositoryPort usuarios, PasswordHasherPort hasher,
                                TokenProviderPort tokens, RelojPort reloj, PoliticaBloqueo politica) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.hasher = Objects.requireNonNull(hasher);
        this.tokens = Objects.requireNonNull(tokens);
        this.reloj = Objects.requireNonNull(reloj);
        this.politica = Objects.requireNonNull(politica);
    }

    @Override
    public SesionIniciada iniciarSesion(ComandoInicioSesion comando) {
        Instant ahora = reloj.ahora();
        String correo = normalizar(comando.correo());

        Optional<Usuario> encontrado = usuarios.buscarPorCorreo(correo);
        if (encontrado.isEmpty()) {
            hasher.coincide(comando.contrasena(), HASH_FICTICIO);
            throw new CredencialesInvalidasException();
        }
        Usuario usuario = encontrado.get();

        // Inactiva o bloqueada: se rechaza sin verificar la contraseña.
        politica.verificarAcceso(usuario, ahora);

        if (!hasher.coincide(comando.contrasena(), usuario.getContrasenaHash())) {
            politica.registrarIntentoFallido(usuario, ahora);
            usuarios.guardar(usuario);
            if (usuario.estaBloqueadoEn(ahora)) {
                throw new CuentaBloqueadaException(usuario.getBloqueadoHasta().orElseThrow());
            }
            throw new CredencialesInvalidasException();
        }

        politica.registrarLoginExitoso(usuario);
        usuarios.guardar(usuario);
        TokenEmitido token = tokens.emitir(usuario, ahora);
        return new SesionIniciada(token.token(), token.expiraEn(), usuario.getId(), usuario.getRol());
    }

    private static String normalizar(String correo) {
        return correo.trim().toLowerCase(Locale.ROOT);
    }
}
