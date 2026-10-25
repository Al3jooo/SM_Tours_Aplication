package co.edu.unimagdalena.samaria360.auth.domain.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Transición de estado de una cuenta, con lo que la base debe encontrar para aplicarla (UPDATE condicional, DEC-39) y
 * lo que debe dejar. {@code validacionEsperada} y {@code validacionNueva} son nulas para el turista. {@code rnt} es el del
 * prestador (nulo para el turista): no cambia, pero viaja en el evento (DEC-146).
 */
public record CambioEstadoCuenta(UUID idUsuario, Rol rol, long versionEsperada,
                                 EstadoCuenta cuentaEsperada, EstadoValidacion validacionEsperada,
                                 EstadoCuenta cuentaNueva, EstadoValidacion validacionNueva,
                                 String motivoRechazo, EstadoAdministrativo estadoNuevo, String rnt) {

    public CambioEstadoCuenta {
        Objects.requireNonNull(idUsuario, "idUsuario");
        Objects.requireNonNull(rol, "rol");
        Objects.requireNonNull(cuentaEsperada, "cuentaEsperada");
        Objects.requireNonNull(cuentaNueva, "cuentaNueva");
        Objects.requireNonNull(estadoNuevo, "estadoNuevo");
    }

    public long versionNueva() {
        return versionEsperada + 1;
    }

    /** Solo la validación del prestador (aprobar o rechazar) cambia {@code estado_validacion}. */
    public boolean cambiaValidacion() {
        return validacionNueva != null && validacionNueva != validacionEsperada;
    }
}
