package co.edu.unimagdalena.samaria360.auth.application.port.in;

import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Fila del listado del personal (RF45, WF-12) y respuesta del cambio de rol. {@code nombre} es el del turista (nulo en
 * las cuentas sin fila en {@code turista}); {@code revocable} dice si la cuenta tiene ese rol base al que volver.
 */
public record PersonalResumen(UUID idUsuario, String nombre, String correo, Rol rol, LocalDate fechaRegistro,
                              boolean revocable) {
}
