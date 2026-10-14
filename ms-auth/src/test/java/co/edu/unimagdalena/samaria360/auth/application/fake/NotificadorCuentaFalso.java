package co.edu.unimagdalena.samaria360.auth.application.fake;

import co.edu.unimagdalena.samaria360.auth.application.port.out.NotificadorCuentaPort;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class NotificadorCuentaFalso implements NotificadorCuentaPort {

    private final List<UUID> avisados = new ArrayList<>();

    @Override
    public void avisarRegistroConCorreoExistente(UUID idUsuario) {
        avisados.add(idUsuario);
    }

    public List<UUID> avisados() {
        return List.copyOf(avisados);
    }
}
