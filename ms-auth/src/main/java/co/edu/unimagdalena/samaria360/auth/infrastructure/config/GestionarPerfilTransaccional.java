package co.edu.unimagdalena.samaria360.auth.infrastructure.config;

import co.edu.unimagdalena.samaria360.auth.application.port.in.ComandoPerfil;
import co.edu.unimagdalena.samaria360.auth.application.port.in.GestionarPerfilUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PerfilActual;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

/**
 * El perfil en una transacción (DEC-85): los datos del turista y el reemplazo de sus categorías se confirman juntos o
 * no se confirma nada; la respuesta se lee dentro de la misma transacción, así que refleja lo que se guardó.
 */
public class GestionarPerfilTransaccional implements GestionarPerfilUseCase {

    private final GestionarPerfilUseCase delegado;
    private final TransactionTemplate transaccion;

    public GestionarPerfilTransaccional(GestionarPerfilUseCase delegado, TransactionTemplate transaccion) {
        this.delegado = delegado;
        this.transaccion = transaccion;
    }

    @Override
    public PerfilActual consultar(UUID idTurista) {
        return transaccion.execute(estado -> delegado.consultar(idTurista));
    }

    @Override
    public PerfilActual actualizar(UUID idTurista, ComandoPerfil comando) {
        return transaccion.execute(estado -> delegado.actualizar(idTurista, comando));
    }
}
