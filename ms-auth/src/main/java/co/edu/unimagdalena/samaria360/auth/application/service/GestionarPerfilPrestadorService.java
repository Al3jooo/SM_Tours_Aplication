package co.edu.unimagdalena.samaria360.auth.application.service;

import co.edu.unimagdalena.samaria360.auth.application.port.in.GestionarPerfilPrestadorUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PerfilPrestador;
import co.edu.unimagdalena.samaria360.auth.application.port.out.PerfilPrestadorRepositoryPort;
import co.edu.unimagdalena.samaria360.auth.domain.exception.UsuarioNoEncontradoException;
import co.edu.unimagdalena.samaria360.auth.domain.model.DatosComercialesPrestador;

import java.util.Objects;
import java.util.UUID;

/**
 * Perfil comercial del Prestador (RF43; DEC-188). Como el perfil del turista (DEC-79): no sube {@code version_cuenta}
 * ni escribe en el outbox, porque ni el token ni las proyecciones llevan estos datos, y no tiene control de
 * concurrencia (DEC-84). La actualización es un solo UPDATE: no necesita una transacción propia.
 */
public class GestionarPerfilPrestadorService implements GestionarPerfilPrestadorUseCase {

    private final PerfilPrestadorRepositoryPort perfiles;

    public GestionarPerfilPrestadorService(PerfilPrestadorRepositoryPort perfiles) {
        this.perfiles = Objects.requireNonNull(perfiles);
    }

    @Override
    public PerfilPrestador consultar(UUID idPrestador) {
        return perfiles.buscar(idPrestador).orElseThrow(() -> new UsuarioNoEncontradoException(idPrestador));
    }

    @Override
    public PerfilPrestador actualizar(UUID idPrestador, String correoCorporativo, String telefono) {
        DatosComercialesPrestador datos = DatosComercialesPrestador.validar(correoCorporativo, telefono);
        if (!perfiles.actualizar(idPrestador, datos)) {
            throw new UsuarioNoEncontradoException(idPrestador);
        }
        return consultar(idPrestador);
    }
}
