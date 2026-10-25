package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

/**
 * Cuerpo de {@code PUT /internal/v1/cuentas/{id}}: el estado de la cuenta y su versión (ADR-008). {@code rnt} solo llega
 * en la cuenta de un prestador (DEC-146).
 */
public record SolicitudCuenta(String rol, String estadoCuenta, Long version, String rnt) {
}
