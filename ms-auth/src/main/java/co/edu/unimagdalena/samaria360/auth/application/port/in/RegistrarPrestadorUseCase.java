package co.edu.unimagdalena.samaria360.auth.application.port.in;

import co.edu.unimagdalena.samaria360.auth.domain.exception.DatosRegistroInvalidosException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.PoliticasNoAceptadasException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.RntYaRegistradoException;

/**
 * Caso de uso Registrarse, prestador (RF02). La cuenta queda Pendiente de Validación hasta que el Administrador la
 * revise (RF28); mientras tanto no puede iniciar sesión (decisión D3).
 */
public interface RegistrarPrestadorUseCase {

    /**
     * @return siempre {@link RegistroRecibido#INSTANCIA}, también si el correo ya tenía una cuenta
     * @throws DatosRegistroInvalidosException si algún campo no es válido
     * @throws PoliticasNoAceptadasException   si no aceptó la Política de Privacidad
     * @throws RntYaRegistradoException        si el RNT ya está registrado (dato público)
     */
    RegistroRecibido registrar(ComandoRegistroPrestador comando);
}
