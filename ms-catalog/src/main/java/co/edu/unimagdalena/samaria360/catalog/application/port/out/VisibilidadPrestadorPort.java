package co.edu.unimagdalena.samaria360.catalog.application.port.out;

/** Mantiene {@code servicios.prestadorActivo}, la copia desnormalizada del estado del prestador (RF22). */
public interface VisibilidadPrestadorPort {

    /** Fija {@code prestadorActivo} en todos los servicios del prestador; idempotente. */
    void fijarPrestadorActivo(String idPrestador, boolean activo);
}
