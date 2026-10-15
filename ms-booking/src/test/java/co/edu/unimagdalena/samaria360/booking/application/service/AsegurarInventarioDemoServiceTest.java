package co.edu.unimagdalena.samaria360.booking.application.service;

import co.edu.unimagdalena.samaria360.booking.application.fake.CatalogoFalso;
import co.edu.unimagdalena.samaria360.booking.application.fake.Eventos;
import co.edu.unimagdalena.samaria360.booking.application.fake.InventarioEnMemoria;
import co.edu.unimagdalena.samaria360.booking.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.booking.domain.exception.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samaria360.booking.domain.model.CategoriaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.DiaInventario;
import co.edu.unimagdalena.samaria360.booking.domain.model.OfertaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.Ofertas;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Generador de inventario demo (D5): una fila por unidad y noche, o por horario; 60 días; nunca toca lo existente. */
class AsegurarInventarioDemoServiceTest {

    /** 22:00 en Santa Marta = 03:00 UTC del día siguiente: «hoy» es el día de Santa Marta. */
    private static final RelojFijo RELOJ = new RelojFijo(ZonedDateTime.parse("2030-03-10T22:00-05:00").toInstant());
    private static final LocalDate HOY = LocalDate.of(2030, 3, 10);

    private final Eventos eventos = new Eventos();
    private final CatalogoFalso catalogo = new CatalogoFalso(eventos).con(Ofertas.hotel(), Ofertas.tour(),
            Ofertas.restaurante());
    private final InventarioEnMemoria inventario = new InventarioEnMemoria(eventos);
    private final AsegurarInventarioDemoService generador = new AsegurarInventarioDemoService(catalogo, inventario,
            RELOJ);

    @Test
    void creaUnaFilaPorUnidadYNocheYDosHorariosDeTourDuranteSesentaDias() {
        int nuevas = generador.asegurar();

        assertThat(nuevas).isEqualTo(60 * 2 + 60 * 2);
        DiaInventario primeraSuite = new DiaInventario(Ofertas.HOTEL, Ofertas.SUITE, HOY, null);
        DiaInventario ultimaDoble = new DiaInventario(Ofertas.HOTEL, Ofertas.DOBLE, HOY.plusDays(59), null);
        assertThat(inventario.totales(primeraSuite)).isEqualTo(3);
        assertThat(inventario.totales(ultimaDoble)).isEqualTo(12);
        assertThat(inventario.totales(new DiaInventario(Ofertas.TOUR, null, HOY, LocalTime.of(8, 0)))).isEqualTo(12);
        assertThat(inventario.totales(new DiaInventario(Ofertas.TOUR, null, HOY, LocalTime.of(14, 0)))).isEqualTo(12);
        assertThat(inventario.dias()).noneMatch(dia -> dia.fecha().isBefore(HOY) || dia.fecha().isAfter(HOY.plusDays(59)))
                .noneMatch(dia -> dia.idServicio().equals(Ofertas.RESTAURANTE));
    }

    @Test
    void noCambiaLosCuposDeFilasQueYaExisten() {
        DiaInventario consumida = new DiaInventario(Ofertas.HOTEL, Ofertas.SUITE, HOY.plusDays(5), null);
        inventario.con(consumida, CategoriaReservable.Alojamiento, 3, 1);

        generador.asegurar();
        int segundaPasada = generador.asegurar();

        assertThat(inventario.disponibles(consumida)).isEqualTo(1);
        assertThat(segundaPasada).isZero();
    }

    @Test
    void recorreTodasLasPaginasDeLaBusqueda() {
        for (int i = 0; i < 11; i++) {
            catalogo.con(new OfertaReservable(String.format("6c%022x", i), Ofertas.PRESTADOR, "Tour " + i, "Tour",
                    BigDecimal.TEN, 5, BigDecimal.ONE, List.of()));
        }

        generador.asegurar();

        assertThat(inventario.dias()).filteredOn(dia -> dia.idServicio().startsWith("6c")).hasSize(11 * 60 * 2);
    }

    @Test
    void unServicioQueDejoDeSerVisibleSeOmite() {
        catalogo.conNoVisibleEnDetalle("6a00000000000000000000aa");

        generador.asegurar();

        assertThat(inventario.dias()).noneMatch(dia -> dia.idServicio().equals("6a00000000000000000000aa"));
    }

    @Test
    void conElCatalogoCaidoFallaYNoCreaNada() {
        catalogo.caer();

        assertThatThrownBy(generador::asegurar).isInstanceOf(CatalogoNoDisponibleException.class);
        assertThat(inventario.cantidadFilas()).isZero();
    }
}
