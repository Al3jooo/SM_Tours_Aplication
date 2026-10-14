package co.edu.unimagdalena.samaria360.auth.application.port.out;

import co.edu.unimagdalena.samaria360.auth.domain.model.RegistroPrestador;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Lo que se persiste de un prestador nuevo: usuario (Activo, rol Prestador) y prestador (Pendiente, políticas
 * aceptadas). El correo va en {@code usuario.correo} y en {@code prestador.correo_corporativo} (decisión D2).
 */
public record PrestadorNuevo(UUID idUsuario, RegistroPrestador datos, String contrasenaHash,
                             LocalDate fechaRegistro) {

    public PrestadorNuevo {
        Objects.requireNonNull(idUsuario, "idUsuario");
        Objects.requireNonNull(datos, "datos");
        Objects.requireNonNull(contrasenaHash, "contrasenaHash");
        Objects.requireNonNull(fechaRegistro, "fechaRegistro");
    }
}
