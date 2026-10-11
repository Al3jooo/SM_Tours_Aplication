package co.edu.unimagdalena.samaria360.auth.application.port.in;

import co.edu.unimagdalena.samaria360.auth.domain.exception.CredencialesInvalidasException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaBloqueadaException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaInactivaException;

/** Caso de uso Iniciar sesión (RF15). */
public interface IniciarSesionUseCase {

    /**
     * @throws CredencialesInvalidasException si el correo no existe o la contraseña es incorrecta
     * @throws CuentaBloqueadaException si la cuenta está bloqueada o este fallo la bloqueó
     * @throws CuentaInactivaException si la cuenta no está Activa
     */
    SesionIniciada iniciarSesion(ComandoInicioSesion comando);
}
