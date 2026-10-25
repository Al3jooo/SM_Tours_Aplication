package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.out.persistence;

import co.edu.unimagdalena.samaria360.booking.application.port.out.OfertaMaterializadaPort;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code oferta_materializada} (V5, DEC-154) con SQL nativo. Cada escritura es una sola sentencia, fuera de las
 * transacciones de reservar y cancelar (DEC-158): dos materializaciones simultáneas no se pisan, porque la marca solo
 * avanza.
 */
@Component
public class OfertaMaterializadaJdbcAdapter implements OfertaMaterializadaPort {

    private final NamedParameterJdbcTemplate jdbc;

    public OfertaMaterializadaJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Map<String, LocalDate> materializadaHasta(Collection<String> idsServicios) {
        Map<String, LocalDate> marcas = new HashMap<>();
        if (idsServicios.isEmpty()) {
            return marcas;
        }
        jdbc.query("""
                        SELECT id_servicio_mongo, materializada_hasta FROM oferta_materializada
                        WHERE id_servicio_mongo IN (:servicios)""",
                new MapSqlParameterSource("servicios", List.copyOf(idsServicios)), fila -> {
                    marcas.put(fila.getString("id_servicio_mongo"),
                            fila.getDate("materializada_hasta").toLocalDate());
                });
        return marcas;
    }

    @Override
    public void registrar(String idServicio, LocalDate hasta, Instant ahora) {
        jdbc.update("""
                        INSERT INTO oferta_materializada (id_servicio_mongo, materializada_hasta, fecha_actualizacion)
                        VALUES (:servicio, :hasta, :ahora)
                        ON CONFLICT (id_servicio_mongo) DO UPDATE
                        SET materializada_hasta = GREATEST(oferta_materializada.materializada_hasta,
                                                           EXCLUDED.materializada_hasta),
                            fecha_actualizacion = EXCLUDED.fecha_actualizacion""",
                new MapSqlParameterSource("servicio", idServicio)
                        .addValue("hasta", hasta, Types.DATE)
                        .addValue("ahora", Timestamp.from(ahora)));
    }
}
