package co.edu.unimagdalena.samaria360.booking.application.port.in;

import co.edu.unimagdalena.samaria360.booking.domain.model.EstadoDisponibilidad;

import java.util.List;
import java.util.Map;

/** Disponibilidad resumida para las tarjetas de búsqueda (§3 del plan): hasta 10 servicios por consulta. */
public interface ResumirDisponibilidadUseCase {

    int MAXIMO_SERVICIOS = 10;

    /** Por defecto, hoy y los 7 días siguientes. El mapa conserva el orden pedido, sin repetidos. */
    Map<String, EstadoDisponibilidad> resumir(List<String> idsServicios, String desde, String hasta);
}
