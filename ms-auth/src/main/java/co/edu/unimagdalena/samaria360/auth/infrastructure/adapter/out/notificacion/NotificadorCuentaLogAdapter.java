package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.notificacion;

import co.edu.unimagdalena.samaria360.auth.application.port.out.NotificadorCuentaPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Mientras no haya envío de correos (llega con RF34, ADR-006), el aviso queda en el log. Solo con el id del usuario: la
 * dirección de correo es un dato personal y no se escribe en los logs.
 */
@Component
public class NotificadorCuentaLogAdapter implements NotificadorCuentaPort {

    private static final Logger LOG = LoggerFactory.getLogger(NotificadorCuentaLogAdapter.class);

    @Override
    public void avisarRegistroConCorreoExistente(UUID idUsuario) {
        LOG.info("Intento de registro con el correo de una cuenta existente; aviso pendiente de envío (RF34): idUsuario={}",
                idUsuario);
    }
}
