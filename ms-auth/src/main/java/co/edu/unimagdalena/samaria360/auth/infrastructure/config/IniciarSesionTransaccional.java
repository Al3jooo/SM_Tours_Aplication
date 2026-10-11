package co.edu.unimagdalena.samaria360.auth.infrastructure.config;

import co.edu.unimagdalena.samaria360.auth.application.port.in.ComandoInicioSesion;
import co.edu.unimagdalena.samaria360.auth.application.port.in.IniciarSesionUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.in.SesionIniciada;
import co.edu.unimagdalena.samaria360.auth.domain.exception.InicioSesionException;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ejecuta el caso de uso completo en una transacción: el lock de buscarPorCorreo se mantiene hasta el final.
 * <p>
 * {@code noRollbackFor} es obligatorio: las excepciones del login (credenciales inválidas, cuenta bloqueada)
 * se lanzan después de registrar el intento fallido, y ese cambio debe confirmarse (RF15).
 */
public class IniciarSesionTransaccional implements IniciarSesionUseCase {

    private final IniciarSesionUseCase delegado;

    public IniciarSesionTransaccional(IniciarSesionUseCase delegado) {
        this.delegado = delegado;
    }

    @Override
    @Transactional(noRollbackFor = InicioSesionException.class)
    public SesionIniciada iniciarSesion(ComandoInicioSesion comando) {
        return delegado.iniciarSesion(comando);
    }
}
