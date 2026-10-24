package co.edu.unimagdalena.samaria360.auth.application.fake;

import co.edu.unimagdalena.samaria360.auth.application.port.in.PerfilActual;
import co.edu.unimagdalena.samaria360.auth.application.port.out.PerfilRepositoryPort;
import co.edu.unimagdalena.samaria360.auth.domain.model.PerfilTurista;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Perfiles de turista en memoria; el correo nunca cambia, como en la base. */
public class PerfilRepositoryEnMemoria implements PerfilRepositoryPort {

    private final Map<UUID, PerfilActual> perfiles = new HashMap<>();
    private int actualizaciones;

    public void agregar(UUID idTurista, String correo, PerfilTurista perfil) {
        perfiles.put(idTurista, new PerfilActual(correo, perfil));
    }

    public int actualizaciones() {
        return actualizaciones;
    }

    @Override
    public Optional<PerfilActual> buscar(UUID idTurista) {
        return Optional.ofNullable(perfiles.get(idTurista));
    }

    @Override
    public boolean actualizar(UUID idTurista, PerfilTurista perfil) {
        PerfilActual actual = perfiles.get(idTurista);
        if (actual == null) {
            return false;
        }
        actualizaciones++;
        perfiles.put(idTurista, new PerfilActual(actual.correo(), perfil));
        return true;
    }
}
