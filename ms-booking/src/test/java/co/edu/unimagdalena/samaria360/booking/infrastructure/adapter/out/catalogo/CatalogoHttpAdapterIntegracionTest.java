package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.out.catalogo;

import co.edu.unimagdalena.samaria360.booking.application.port.out.CatalogoPort;
import co.edu.unimagdalena.samaria360.booking.domain.exception.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samaria360.booking.domain.model.OfertaReservable;
import co.edu.unimagdalena.samaria360.booking.infrastructure.CatalogoSimulado;
import co.edu.unimagdalena.samaria360.booking.infrastructure.CatalogoSimulado.Unidad;
import co.edu.unimagdalena.samaria360.booking.infrastructure.PruebaIntegracion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cliente de ms-catalog (precisión 2): reintenta una vez solo ante red o 5xx, nunca ante 404 ni otros 4xx; el
 * reintento va dentro del circuito; con el catálogo lento, la latencia máxima es de ≈ 3 s de lectura más la conexión.
 */
@PruebaIntegracion
class CatalogoHttpAdapterIntegracionTest {

    @Autowired
    private CatalogoPort catalogoPort;
    @Autowired
    private CatalogoSimulado catalogo;
    @Autowired
    private Resilience4JCircuitBreakerFactory circuitos;

    @BeforeEach
    @AfterEach
    void cerrarCircuito() {
        circuitos.getCircuitBreakerRegistry().circuitBreaker("catalogo").reset();
    }

    @Test
    void elDetalleTraeLasUnidadesDelAlojamiento() {
        String doble = CatalogoSimulado.nuevoId();
        String hotel = catalogo.publicarHotel("Hotel", new Unidad(doble, "Doble", 12, 2, "150000"));

        OfertaReservable oferta = catalogoPort.buscarReservable(hotel).orElseThrow();

        assertThat(oferta.categoria()).isEqualTo("Alojamiento");
        assertThat(oferta.idPrestador()).isEqualTo(CatalogoSimulado.PRESTADOR);
        assertThat(oferta.unidad(doble)).hasValueSatisfying(unidad -> {
            assertThat(unidad.numHabitaciones()).isEqualTo(12);
            assertThat(unidad.precioNoche()).isEqualByComparingTo("150000");
        });
    }

    @Test
    void un404EsNoReservableSinReintento() {
        String retirado = CatalogoSimulado.nuevoId();

        assertThat(catalogoPort.buscarReservable(retirado)).isEmpty();
        assertThat(catalogo.llamadas(retirado)).isEqualTo(1);
    }

    @Test
    void otro4xxNoSeReintentaNiEsIndisponibilidad() {
        String tour = catalogo.publicarTour("Tour", 10, "1000");
        catalogo.fallar(tour, 400, -1);

        assertThatThrownBy(() -> catalogoPort.buscarReservable(tour))
                .isInstanceOf(RespuestaInesperadaCatalogoException.class);
        assertThat(catalogo.llamadas(tour)).isEqualTo(1);
    }

    @Test
    void un5xxSeReintentaUnaVez() {
        String tour = catalogo.publicarTour("Tour", 10, "1000");
        catalogo.fallar(tour, 503, 1);

        assertThat(catalogoPort.buscarReservable(tour)).isPresent();
        assertThat(catalogo.llamadas(tour)).isEqualTo(2);

        String caido = catalogo.publicarTour("Tour caído", 10, "1000");
        catalogo.fallar(caido, 500, -1);
        assertThatThrownBy(() -> catalogoPort.buscarReservable(caido))
                .isInstanceOf(CatalogoNoDisponibleException.class);
        assertThat(catalogo.llamadas(caido)).isEqualTo(2);
    }

    @Test
    void conElCatalogoLentoSeAgotaElTiempoDosVecesYResponde503EnMenosDe4Segundos() {
        String lento = catalogo.publicarTour("Tour lento", 10, "1000");
        catalogo.demorar(lento, Duration.ofMillis(2500));
        long inicio = System.nanoTime();
        try {
            assertThatThrownBy(() -> catalogoPort.buscarReservable(lento))
                    .isInstanceOf(CatalogoNoDisponibleException.class);
        } finally {
            catalogo.normalizar(lento);
        }
        Duration tardo = Duration.ofNanos(System.nanoTime() - inicio);

        assertThat(catalogo.llamadas(lento)).isEqualTo(2);
        assertThat(tardo).isBetween(Duration.ofMillis(2900), Duration.ofMillis(4000));
    }

    @Test
    void conElCircuitoAbiertoNiSiquieraSeLlamaAlCatalogo() {
        String caido = catalogo.publicarTour("Tour caído", 10, "1000");
        catalogo.fallar(caido, 503, -1);
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> catalogoPort.buscarReservable(caido))
                    .isInstanceOf(CatalogoNoDisponibleException.class);
        }
        int antes = catalogo.llamadas(caido);

        assertThatThrownBy(() -> catalogoPort.buscarReservable(caido))
                .isInstanceOf(CatalogoNoDisponibleException.class);
        assertThat(catalogo.llamadas(caido)).isEqualTo(antes);
    }
}
