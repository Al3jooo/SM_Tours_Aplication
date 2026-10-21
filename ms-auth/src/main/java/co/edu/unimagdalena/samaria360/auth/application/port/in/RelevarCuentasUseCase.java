package co.edu.unimagdalena.samaria360.auth.application.port.in;

import co.edu.unimagdalena.samaria360.auth.domain.model.Destino;

/** Entrega los eventos pendientes de un destino (ADR-008). Lo invoca el programador; un hilo por destino. */
public interface RelevarCuentasUseCase {

    /** @return cuántas entregas salieron bien en esta pasada */
    int relevar(Destino destino);
}
