package co.edu.unimagdalena.samaria360.auth.infrastructure.config;

import co.edu.unimagdalena.samaria360.auth.application.port.in.ComandoRegistroTurista;
import co.edu.unimagdalena.samaria360.auth.application.port.in.RegistrarTuristaUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.in.RegistroRecibido;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CorreoYaRegistradoException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Registro de turista en una transacción: usuario, turista y categorías se confirman juntos o no se confirma nada.
 * <p>
 * Si otro registro con el mismo correo termina primero (dos envíos simultáneos), la restricción única hace fallar este y
 * la transacción se revierte entera. Entonces se repite una vez: el correo ya existe y el caso de uso sigue su camino
 * normal (aviso al dueño y la misma respuesta), así que el cliente nunca ve un 500 ni una respuesta distinta (ADR-006).
 */
public class RegistrarTuristaTransaccional implements RegistrarTuristaUseCase {

    private final RegistrarTuristaUseCase delegado;
    private final TransactionTemplate transaccion;

    public RegistrarTuristaTransaccional(RegistrarTuristaUseCase delegado, TransactionTemplate transaccion) {
        this.delegado = delegado;
        this.transaccion = transaccion;
    }

    @Override
    public RegistroRecibido registrar(ComandoRegistroTurista comando) {
        try {
            return transaccion.execute(estado -> delegado.registrar(comando));
        } catch (CorreoYaRegistradoException carrera) {
            return transaccion.execute(estado -> delegado.registrar(comando));
        }
    }
}
