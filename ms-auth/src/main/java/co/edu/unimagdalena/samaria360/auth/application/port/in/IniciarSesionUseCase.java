package co.edu.unimagdalena.samaria360.auth.application.port.in;

import co.edu.unimagdalena.samaria360.auth.domain.exception.CredencialesInvalidasException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaBloqueadaException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaInactivaException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaPendienteValidacionException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaRechazadaException;

/** Caso de uso Iniciar sesión (RF15). */
public interface IniciarSesionUseCase {

    /**
     * @throws CredencialesInvalidasException si el correo no existe o la contraseña es incorrecta
     * @throws CuentaBloqueadaException si la cuenta está bloqueada o este fallo la bloqueó
     * @throws CuentaInactivaException si la contraseña es correcta pero la cuenta no está Activa (RF22)
     * @throws CuentaPendienteValidacionException si la contraseña es correcta y el prestador está Pendiente (D3)
     * @throws CuentaRechazadaException si la contraseña es correcta y el prestador fue Rechazado (D3)
     */
    SesionIniciada iniciarSesion(ComandoInicioSesion comando);
}
