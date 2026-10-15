package co.edu.unimagdalena.samaria360.booking.infrastructure;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

/** Filas de inventario_cupo hechas a mano para cada prueba (las pruebas no usan el generador salvo el suyo). */
@Component
public class InventarioDePrueba {

    private final JdbcTemplate jdbc;

    public InventarioDePrueba(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void noches(String servicio, String unidad, LocalDate desde, int noches, int totales, int disponibles) {
        for (int i = 0; i < noches; i++) {
            jdbc.update("INSERT INTO inventario_cupo (id_servicio_mongo, categoria, id_unidad, fecha, hora_inicio,"
                            + " cupos_totales, cupos_disponibles) VALUES (?, 'Alojamiento', ?, ?, NULL, ?, ?)",
                    servicio, unidad, desde.plusDays(i), totales, disponibles);
        }
    }

    public void horario(String servicio, LocalDate fecha, LocalTime hora, int totales, int disponibles) {
        jdbc.update("INSERT INTO inventario_cupo (id_servicio_mongo, categoria, id_unidad, fecha, hora_inicio,"
                        + " cupos_totales, cupos_disponibles) VALUES (?, 'Tour', NULL, ?, ?, ?, ?)",
                servicio, fecha, hora, totales, disponibles);
    }

    public int noche(String servicio, String unidad, LocalDate fecha) {
        return jdbc.queryForObject("SELECT cupos_disponibles FROM inventario_cupo WHERE id_servicio_mongo = ?"
                + " AND id_unidad = ? AND fecha = ?", Integer.class, servicio, unidad, fecha);
    }

    public int horario(String servicio, LocalDate fecha, LocalTime hora) {
        return jdbc.queryForObject("SELECT cupos_disponibles FROM inventario_cupo WHERE id_servicio_mongo = ?"
                + " AND id_unidad IS NULL AND fecha = ? AND hora_inicio = ?", Integer.class, servicio, fecha, hora);
    }

    public int reservasDe(String servicio) {
        return jdbc.queryForObject("SELECT count(*) FROM reserva WHERE id_servicio_mongo = ?", Integer.class, servicio);
    }
}
