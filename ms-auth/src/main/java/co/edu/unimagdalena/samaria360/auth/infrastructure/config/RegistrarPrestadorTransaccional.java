package co.edu.unimagdalena.samaria360.auth.infrastructure.config;

import co.edu.unimagdalena.samaria360.auth.application.port.in.ComandoRegistroPrestador;
import co.edu.unimagdalena.samaria360.auth.application.port.in.RegistrarPrestadorUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.in.RegistroRecibido;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CorreoYaRegistradoException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Registro de prestador en una transacción, con el mismo reintento que {@link RegistrarTuristaTransaccional} para el
 * correo. Una carrera por el RNT termina en 409 RNT_YA_REGISTRADO, sin reintento. Caso límite aceptado (ADR-006): con el
 * mismo correo y el mismo RNT a la vez, el reintento ve primero el RNT y responde 409.
 */
public class RegistrarPrestadorTransaccional implements RegistrarPrestadorUseCase {

    private final RegistrarPrestadorUseCase delegado;
    private final TransactionTemplate transaccion;

    public RegistrarPrestadorTransaccional(RegistrarPrestadorUseCase delegado, TransactionTemplate transaccion) {
        this.delegado = delegado;
        this.transaccion = transaccion;
    }

    @Override
    public RegistroRecibido registrar(ComandoRegistroPrestador comando) {
        try {
            return transaccion.execute(estado -> delegado.registrar(comando));
        } catch (CorreoYaRegistradoException carrera) {
            return transaccion.execute(estado -> delegado.registrar(comando));
        }
    }
}
