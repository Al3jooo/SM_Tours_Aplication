package co.edu.unimagdalena.samaria360.auth.application.port.in;

import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoAdministrativo;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Datos organizacionales del prestador que el Administrador revisa (RF28). {@code fechaRegistro} es el día en que se
 * registró y aceptó las políticas (decisión D6); no hay hora. {@code razonSocial}, {@code motivoRechazo} y
 * {@code revisadoEn} pueden ser nulos.
 */
public record PrestadorDetalle(UUID idUsuario, String representanteLegal, String razonSocial, String rnt,
                               String correoCorporativo, String telefono, boolean aceptoPoliticas,
                               LocalDate fechaRegistro, EstadoAdministrativo estado, String motivoRechazo,
                               Instant revisadoEn) {
}
