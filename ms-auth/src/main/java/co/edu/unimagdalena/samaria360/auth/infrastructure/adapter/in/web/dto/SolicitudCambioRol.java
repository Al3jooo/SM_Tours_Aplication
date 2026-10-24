package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto;

/**
 * Cambio de rol (RF45): {@code rolNuevo} es Turista, Gestor o Administrador; {@code rolActual}, el que vio el
 * Administrador (control de concurrencia). La validación la hace el caso de uso, que responde un código por campo.
 */
public record SolicitudCambioRol(String rolNuevo, String rolActual) {
}
