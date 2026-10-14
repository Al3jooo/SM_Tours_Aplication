package co.edu.unimagdalena.samaria360.auth.domain.service;

import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaInactivaException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaPendienteValidacionException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaRechazadaException;
import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoValidacion;
import co.edu.unimagdalena.samaria360.auth.domain.model.Usuario;

import java.util.Optional;

/**
 * Estado de la cuenta en el inicio de sesión, evaluado <b>después</b> de verificar la contraseña: sin ella, nadie puede
 * averiguar si una cuenta está inactiva o si un prestador está pendiente. Son dos estados distintos:
 * <ul>
 *   <li>{@code usuario.estado_cuenta} (RF22, RF62): solo Activo puede entrar → 403 CUENTA_INACTIVA;</li>
 *   <li>{@code prestador.estado_validacion} (RF02, RF28; decisión D3): Pendiente → 403 CUENTA_PENDIENTE_VALIDACION,
 *       Rechazado → 403 CUENTA_RECHAZADA.</li>
 * </ul>
 */
public class PoliticaAcceso {

    /**
     * @param validacion estado de validación si el usuario es prestador; vacío para los demás roles
     */
    public void verificar(Usuario usuario, Optional<EstadoValidacion> validacion) {
        if (!usuario.getEstadoCuenta().permiteIniciarSesion()) {
            throw new CuentaInactivaException(usuario.getEstadoCuenta());
        }
        validacion.ifPresent(estado -> {
            switch (estado) {
                case Pendiente -> throw new CuentaPendienteValidacionException();
                case Rechazado -> throw new CuentaRechazadaException();
                case Activo -> {
                    // Validado por el Administrador: puede entrar.
                }
            }
        });
    }
}
