package co.edu.unimagdalena.samaria360.auth.application.port.in;

import java.util.UUID;

/**
 * Cambio de rol pedido por el Administrador (RF45). {@code rolNuevo} y {@code rolActual} llegan como texto y los valida
 * el caso de uso (400 con un código por campo); {@code rolActual} es el rol que vio el Administrador.
 */
public record ComandoCambioRol(UUID idCuenta, String rolNuevo, String rolActual, UUID idAdministrador) {
}
