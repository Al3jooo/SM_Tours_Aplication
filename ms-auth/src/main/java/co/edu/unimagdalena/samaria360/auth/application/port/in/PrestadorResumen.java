package co.edu.unimagdalena.samaria360.auth.application.port.in;

import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoAdministrativo;

import java.time.LocalDate;
import java.util.UUID;

/** Fila del listado de prestadores del Administrador (WF-12). */
public record PrestadorResumen(UUID idUsuario, String representanteLegal, String rnt, String correoCorporativo,
                               LocalDate fechaRegistro, EstadoAdministrativo estado) {
}
