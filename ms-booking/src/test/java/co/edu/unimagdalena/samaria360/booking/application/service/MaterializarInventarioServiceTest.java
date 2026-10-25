package co.edu.unimagdalena.samaria360.booking.application.service;

import co.edu.unimagdalena.samaria360.booking.application.fake.CatalogoFalso;
import co.edu.unimagdalena.samaria360.booking.application.fake.Eventos;
import co.edu.unimagdalena.samaria360.booking.application.fake.InventarioEnMemoria;
import co.edu.unimagdalena.samaria360.booking.application.fake.OfertaMaterializadaEnMemoria;
import co.edu.unimagdalena.samaria360.booking.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.booking.domain.exception.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samaria360.booking.domain.model.CategoriaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.DiaInventario;
import co.edu.unimagdalena.samaria360.booking.domain.model.Ofertas;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import static co.edu.unimagdalena.samaria360.booking.domain.model.Ofertas.HOY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Materialización bajo demanda con dobles de los puertos (DEC-152 a DEC-158): marca, 404, caído y fechas. */
class MaterializarInventarioServiceTest {

    private static final LocalTime OCHO = LocalTime.of(8, 0);
    private static final LocalTime DOS = LocalTime.of(14, 0);

    private final Eventos eventos = new Eventos();
    private final CatalogoFalso catalogo = new CatalogoFalso(eventos);
    private final InventarioEnMemoria inventario = new InventarioEnMemoria(eventos);
    private final OfertaMaterializadaEnMemoria marcas = new OfertaMaterializadaEnMemoria(eventos);
    private final MaterializarInventarioService materializacion = new MaterializarInventarioService(catalogo,
            inventario, marcas, new RelojFijo(Ofertas.AHORA));

    @Test
    void materializaLaVentanaCompletaYAvanzaLaMarca() {
        catalogo.con(Ofertas.tourQueSale(Ofertas.TODOS_LOS_DIAS, OCHO, DOS));

        materializacion.asegurar(List.of(Ofertas.TOUR), HOY.plusDays(2));

        assertThat(inventario.cantidadFilas()).isEqualTo(60 * 2);
        assertThat(marcas.marca(Ofertas.TOUR)).isEqualTo(HOY.plusDays(59));
    }

    @Test
    void conLaMarcaAlDiaNoLlamaAlCatalogo() {
        marcas.con(Ofertas.TOUR, HOY.plusDays(59));

        materializacion.asegurar(List.of(Ofertas.TOUR), HOY.plusDays(59));

        assertThat(catalogo.consultasDetalle()).isZero();
    }

    @Test
    void unaMarcaDeAyerNoCubreElUltimoDiaDeHoy() {
        catalogo.con(Ofertas.tourQueSale(Ofertas.TODOS_LOS_DIAS, OCHO));
        marcas.con(Ofertas.TOUR, HOY.plusDays(58));

        materializacion.asegurar(List.of(Ofertas.TOUR), HOY.plusDays(10));
        materializacion.asegurar(List.of(Ofertas.TOUR), HOY.plusDays(70));

        assertThat(catalogo.consultasDetalle()).as("solo el pedido que llega al horizonte").isEqualTo(1);
        assertThat(marcas.marca(Ofertas.TOUR)).isEqualTo(HOY.plusDays(59));
    }

    @Test
    void materializarDosVecesNoDuplicaNiCambiaLosCuposConsumidos() {
        catalogo.con(Ofertas.hotelQueOpera(Ofertas.TODOS_LOS_DIAS));
        DiaInventario consumida = new DiaInventario(Ofertas.HOTEL, Ofertas.SUITE, HOY.plusDays(5), null);
        inventario.con(consumida, CategoriaReservable.Alojamiento, 3, 1);

        materializacion.asegurar(List.of(Ofertas.HOTEL), HOY.plusDays(59));
        materializacion.asegurar(Ofertas.hotelQueOpera(Ofertas.TODOS_LOS_DIAS), HOY.plusDays(59));
        new MaterializarInventarioService(catalogo, inventario, new OfertaMaterializadaEnMemoria(eventos),
                new RelojFijo(Ofertas.AHORA)).asegurar(List.of(Ofertas.HOTEL), HOY.plusDays(59));

        assertThat(inventario.cantidadFilas()).isEqualTo(60 * 2);
        assertThat(inventario.disponibles(consumida)).isEqualTo(1);
    }

    @Test
    void un404NoMaterializaNiAvanzaLaMarca() {
        materializacion.asegurar(List.of(Ofertas.TOUR), HOY.plusDays(7));

        assertThat(inventario.cantidadFilas()).isZero();
        assertThat(marcas.marca(Ofertas.TOUR)).isNull();
        assertThat(eventos.todos()).doesNotContain("marcas.registrar");
    }

    @Test
    void unServicioDeOtraCategoriaOSinOfertaAvanzaLaMarcaSinFilas() {
        catalogo.con(Ofertas.restaurante(), Ofertas.tour());

        materializacion.asegurar(List.of(Ofertas.RESTAURANTE, Ofertas.TOUR), HOY.plusDays(7));

        assertThat(inventario.cantidadFilas()).isZero();
        assertThat(marcas.marca(Ofertas.RESTAURANTE)).isEqualTo(HOY.plusDays(59));
        assertThat(marcas.marca(Ofertas.TOUR)).isEqualTo(HOY.plusDays(59));
        assertThat(eventos.todos()).doesNotContain("inventario.asegurar");
    }

    @Test
    void conElCatalogoCaidoCortaYNoCreaNada() {
        catalogo.caer();

        assertThatThrownBy(() -> materializacion.asegurar(List.of(Ofertas.TOUR, Ofertas.HOTEL), HOY))
                .isInstanceOf(CatalogoNoDisponibleException.class);
        assertThat(inventario.cantidadFilas()).isZero();
        assertThat(eventos.todos().stream().filter("catalogo.detalle"::equals)).hasSize(1);
    }

    @Test
    void unRangoYaPasadoNoMaterializaNiLlamaAlCatalogo() {
        materializacion.asegurar(List.of(Ofertas.TOUR), HOY.minusDays(1));

        assertThat(eventos.todos()).isEmpty();
    }

    /** 22:00 en Santa Marta = 03:00 UTC del día siguiente: «hoy» es el día de Santa Marta. */
    @Test
    void hoyEsElDiaDeSantaMarta() {
        catalogo.con(Ofertas.tourQueSale(Ofertas.TODOS_LOS_DIAS, OCHO));
        MaterializarInventarioService deNoche = new MaterializarInventarioService(catalogo, inventario, marcas,
                new RelojFijo(ZonedDateTime.parse("2030-03-10T22:00-05:00").toInstant()));

        deNoche.asegurar(List.of(Ofertas.TOUR), HOY);

        assertThat(inventario.dias()).extracting(DiaInventario::fecha).contains(HOY).doesNotContain(HOY.minusDays(1));
        assertThat(marcas.marca(Ofertas.TOUR)).isEqualTo(HOY.plusDays(59));
    }
}
