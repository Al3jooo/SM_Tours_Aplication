package co.edu.unimagdalena.samaria360.auth.application.port.out;

import java.util.UUID;

/**
 * Avisos al dueño de una cuenta. Hoy resuelve el caso «olvidé que tenía cuenta»: alguien intentó registrarse con su
 * correo (ADR-006). El envío por correo electrónico llega con RF34; mientras tanto el adaptador solo deja un log, con el id
 * y nunca con la dirección.
 */
public interface NotificadorCuentaPort {

    void avisarRegistroConCorreoExistente(UUID idUsuario);
}
