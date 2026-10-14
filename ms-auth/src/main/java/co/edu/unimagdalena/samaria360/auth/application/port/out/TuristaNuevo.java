package co.edu.unimagdalena.samaria360.auth.application.port.out;

import co.edu.unimagdalena.samaria360.auth.domain.model.RegistroTurista;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/** Lo que se persiste de un turista nuevo: usuario (Activo, rol Turista), turista y sus categorías. */
public record TuristaNuevo(UUID idUsuario, RegistroTurista datos, String contrasenaHash, LocalDate fechaRegistro) {

    public TuristaNuevo {
        Objects.requireNonNull(idUsuario, "idUsuario");
        Objects.requireNonNull(datos, "datos");
        Objects.requireNonNull(contrasenaHash, "contrasenaHash");
        Objects.requireNonNull(fechaRegistro, "fechaRegistro");
    }
}
