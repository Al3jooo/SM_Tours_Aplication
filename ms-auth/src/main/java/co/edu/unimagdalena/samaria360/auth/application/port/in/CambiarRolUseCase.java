package co.edu.unimagdalena.samaria360.auth.application.port.in;

/** Asignar, modificar o revocar el rol de Gestor o Administrador (RF45). Cada cambio es una transacción con su evento. */
public interface CambiarRolUseCase {

    PersonalResumen cambiarRol(ComandoCambioRol comando);
}
