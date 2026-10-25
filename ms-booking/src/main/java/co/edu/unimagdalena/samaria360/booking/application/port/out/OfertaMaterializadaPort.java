package co.edu.unimagdalena.samaria360.booking.application.port.out;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;

/**
 * Hasta qué fecha está materializada la oferta de cada servicio ({@code oferta_materializada}, V5; DEC-154). Una fecha
 * hasta esa marca sin filas de inventario es un día que no opera; una posterior, un día que falta materializar.
 */
public interface OfertaMaterializadaPort {

    /** Solo trae los servicios que tienen marca. */
    Map<String, LocalDate> materializadaHasta(Collection<String> idsServicios);

    /** Avanza la marca; nunca la retrocede (UPSERT con GREATEST). */
    void registrar(String idServicio, LocalDate hasta, Instant ahora);
}
