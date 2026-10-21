package co.edu.unimagdalena.samaria360.auth.application.port.out;

import co.edu.unimagdalena.samaria360.auth.domain.model.Destino;

/** Entrega un evento a un servicio: {@code PUT /internal/v1/cuentas/{id}} con el JWT de servicio. */
public interface DestinoCuentasPort {

    /** @throws EntregaFallidaException si el destino no responde 2xx */
    void enviar(Destino destino, EntregaPendiente entrega);
}
