package co.edu.unimagdalena.samaria360.auth.application.port.in;

import co.edu.unimagdalena.samaria360.auth.domain.exception.DatosRegistroInvalidosException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.PoliticasNoAceptadasException;

/** Caso de uso Registrarse, turista (RF01). La cuenta queda Activa; no inicia sesión (ADR-006). */
public interface RegistrarTuristaUseCase {

    /**
     * @return siempre {@link RegistroRecibido#INSTANCIA}, también si el correo ya tenía una cuenta
     * @throws DatosRegistroInvalidosException si algún campo no es válido
     * @throws PoliticasNoAceptadasException   si no aceptó la Política de Privacidad
     */
    RegistroRecibido registrar(ComandoRegistroTurista comando);
}
