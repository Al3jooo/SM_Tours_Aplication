package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.out.persistence;

import co.edu.unimagdalena.samaria360.booking.application.port.in.AsegurarInventarioDemoUseCase;
import co.edu.unimagdalena.samaria360.booking.infrastructure.CatalogoSimulado;
import co.edu.unimagdalena.samaria360.booking.infrastructure.CatalogoSimulado.Unidad;
import co.edu.unimagdalena.samaria360.booking.infrastructure.InventarioDePrueba;
import co.edu.unimagdalena.samaria360.booking.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.booking.infrastructure.RelojAjustable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Generador de inventario demo (D5) contra PostgreSQL: ON CONFLICT DO NOTHING y una fila por unidad y noche. */
@PruebaIntegracion
class GeneradorInventarioIntegracionTest {

    @Autowired
    private AsegurarInventarioDemoUseCase generador;
    @Autowired
    private CatalogoSimulado catalogo;
    @Autowired
    private InventarioDePrueba inventario;
    @Autowired
    private RelojAjustable reloj;
    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void limpiar() {
        catalogo.limpiarBusqueda();
        reloj.reiniciar();
    }

    @Test
    void creaLasFilasRespetaLasConsumidasYLeDaFilasAUnaUnidadNueva() {
        reloj.reiniciar();
        String doble = CatalogoSimulado.nuevoId();
        String suite = CatalogoSimulado.nuevoId();
        String hotel = catalogo.publicarHotel("Hotel", new Unidad(doble, "Doble", 12, 2, "150000"),
                new Unidad(suite, "Suite", 3, 4, "260000"));
        String tour = catalogo.publicarTour("Tour", 15, "150000");
        catalogo.enBusqueda("Alojamiento", hotel);
        catalogo.enBusqueda("Tour", tour);

        assertThat(generador.asegurar()).isEqualTo(60 * 2 + 60 * 2);
        assertThat(filas(hotel)).isEqualTo(120);
        assertThat(inventario.horario(tour, RelojAjustable.HOY, LocalTime.of(14, 0))).isEqualTo(15);

        // Se consumen cupos y el prestador cambia el catálogo: más habitaciones y una unidad nueva.
        jdbc.update("UPDATE inventario_cupo SET cupos_disponibles = 1 WHERE id_servicio_mongo = ? AND id_unidad = ?"
                + " AND fecha = ?", hotel, suite, RelojAjustable.HOY.plusDays(5));
        String familiar = CatalogoSimulado.nuevoId();
        catalogo.publicarHotel(hotel, "Hotel", new Unidad(doble, "Doble", 20, 2, "150000"),
                new Unidad(suite, "Suite", 3, 4, "260000"), new Unidad(familiar, "Familiar", 2, 6, "300000"));

        assertThat(generador.asegurar()).isEqualTo(60);
        assertThat(inventario.noche(hotel, suite, RelojAjustable.HOY.plusDays(5))).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT cupos_totales FROM inventario_cupo WHERE id_servicio_mongo = ?"
                + " AND id_unidad = ? AND fecha = ?", Integer.class, hotel, doble, RelojAjustable.HOY)).isEqualTo(12);
        assertThat(inventario.noche(hotel, familiar, RelojAjustable.HOY.plusDays(59))).isEqualTo(2);
    }

    private int filas(String servicio) {
        return jdbc.queryForObject("SELECT count(*) FROM inventario_cupo WHERE id_servicio_mongo = ?", Integer.class,
                servicio);
    }
}
