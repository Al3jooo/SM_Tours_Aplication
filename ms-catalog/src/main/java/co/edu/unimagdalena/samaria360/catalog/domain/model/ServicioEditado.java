package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.util.Objects;

/**
 * Resultado de una edición del Prestador ya validada (RF32, RF43): el servicio con los cambios y la versión que el
 * Prestador leyó. La escritura es condicional sobre esa versión y la sube en uno (DEC-186).
 */
public record ServicioEditado(ServicioComercial servicio, int versionEsperada) {

    public ServicioEditado {
        Objects.requireNonNull(servicio, "servicio");
        if (versionEsperada < 0) {
            throw new IllegalArgumentException("versionEsperada no puede ser negativa");
        }
    }
}
