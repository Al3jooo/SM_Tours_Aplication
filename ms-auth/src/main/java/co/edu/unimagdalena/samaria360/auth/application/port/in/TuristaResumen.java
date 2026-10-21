package co.edu.unimagdalena.samaria360.auth.application.port.in;

import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoAdministrativo;

import java.time.LocalDate;
import java.util.UUID;

/** Fila del listado de turistas del Administrador (WF-12); {@code pais} es el código ISO 3166-1 alfa-2. */
public record TuristaResumen(UUID idUsuario, String nombre, String correo, String pais, LocalDate fechaRegistro,
                             EstadoAdministrativo estado) {
}
