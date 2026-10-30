package co.edu.unimagdalena.samaria360.booking.application.service;

import co.edu.unimagdalena.samaria360.booking.application.fake.Eventos;
import co.edu.unimagdalena.samaria360.booking.application.fake.ReservaRepositoryEnMemoria;
import co.edu.unimagdalena.samaria360.booking.domain.exception.DatosReservaInvalidosException;
import co.edu.unimagdalena.samaria360.booking.domain.model.CategoriaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.EstadoReserva;
import co.edu.unimagdalena.samaria360.booking.domain.model.HorarioReservas;
import co.edu.unimagdalena.samaria360.booking.domain.model.MotivoInvalido;
import co.edu.unimagdalena.samaria360.booking.domain.model.Ofertas;
import co.edu.unimagdalena.samaria360.booking.domain.model.PaginaReservas;
import co.edu.unimagdalena.samaria360.booking.domain.model.Reserva;
import co.edu.unimagdalena.samaria360.booking.domain.model.UnidadCobro;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CU «Consultar historial» del lado del Prestador con dobles (RF12; DEC-191, DEC-192): solo las reservas de sus
 * servicios, con los filtros de «Mis reservas» más el servicio, en páginas de 20.
 */
class ConsultarReservasPrestadorServiceTest {

    private static final UUID PRESTADOR = UUID.fromString("00000000-0000-4000-8000-0000000000e1");
    private static final UUID OTRO = UUID.fromString("00000000-0000-4000-8000-0000000000e2");
    private static final String KAYAK = "6a00000000000000000000aa";
    private static final String CAFE = "6a00000000000000000000bb";

    private final ReservaRepositoryEnMemoria reservas = new ReservaRepositoryEnMemoria(new Eventos());
    private final ConsultarReservasService consultar = new ConsultarReservasService(reservas);

    @Test
    void soloTraeLasReservasDeLosServiciosDelPrestador() {
        Reserva propia = insertar(PRESTADOR, KAYAK, 5, EstadoReserva.Confirmada);
        insertar(OTRO, CAFE, 6, EstadoReserva.Confirmada);

        PaginaReservas pagina = consultar.consultarDelPrestador(PRESTADOR, null, null, null, null, 0);

        assertThat(pagina.elementos()).containsExactly(propia);
        assertThat(pagina.tamano()).isEqualTo(20);
    }

    @Test
    void filtraPorEstadoPorRangoDeLaActividadYPorServicio() {
        Reserva kayakConfirmada = insertar(PRESTADOR, KAYAK, 5, EstadoReserva.Confirmada);
        Reserva kayakCancelada = insertar(PRESTADOR, KAYAK, 6, EstadoReserva.Cancelada);
        Reserva cafeCompletada = insertar(PRESTADOR, CAFE, 7, EstadoReserva.Completada);
        insertar(PRESTADOR, CAFE, 40, EstadoReserva.Confirmada); // en enero

        assertThat(consultar.consultarDelPrestador(PRESTADOR, "2030-12-01", "2030-12-31", null, null, 0).elementos())
                .containsExactly(cafeCompletada, kayakCancelada, kayakConfirmada);
        assertThat(consultar.consultarDelPrestador(PRESTADOR, null, null, "Cancelada,Completada", null, 0)
                .elementos()).containsExactly(cafeCompletada, kayakCancelada);
        assertThat(consultar.consultarDelPrestador(PRESTADOR, null, null, null, KAYAK, 0).elementos())
                .containsExactly(kayakCancelada, kayakConfirmada);
        // Un servicio ajeno no es un error: no tiene reservas suyas.
        assertThat(consultar.consultarDelPrestador(OTRO, null, null, null, KAYAK, 0).elementos()).isEmpty();
    }

    @Test
    void paginaDeVeinteEnVeinte() {
        for (int i = 0; i < 21; i++) {
            insertar(PRESTADOR, KAYAK, 1 + i, EstadoReserva.Confirmada);
        }

        assertThat(consultar.consultarDelPrestador(PRESTADOR, null, null, null, null, 0).elementos()).hasSize(20);
        PaginaReservas segunda = consultar.consultarDelPrestador(PRESTADOR, null, null, null, null, 1);
        assertThat(segunda.elementos()).hasSize(1);
        assertThat(segunda.totalElementos()).isEqualTo(21);
    }

    @Test
    void losFiltrosInvalidosSonDatosInvalidos() {
        assertThatThrownBy(() -> consultar.consultarDelPrestador(PRESTADOR, "2030-01-01", "2031-01-02", "Pendiente",
                "x", -1))
                .isInstanceOfSatisfying(DatosReservaInvalidosException.class, e -> assertThat(e.getCampos())
                        .containsExactly(Map.entry("hasta", MotivoInvalido.MAXIMO_DIAS),
                                Map.entry("estado", MotivoInvalido.NO_PERMITIDO),
                                Map.entry("servicio", MotivoInvalido.FORMATO),
                                Map.entry("pagina", MotivoInvalido.VALOR_MINIMO)));
    }

    /** Un tour de 1 persona el día {@code dia} de diciembre de 2030 (40 cae en enero de 2031). */
    private Reserva insertar(UUID prestador, String servicio, int dia, EstadoReserva estado) {
        Instant inicio = HorarioReservas.instante(LocalDate.of(2030, 12, 1).plusDays(dia - 1), LocalTime.of(8, 0));
        Reserva reserva = new Reserva(UUID.randomUUID(), UUID.randomUUID(), prestador, servicio, "Tour",
                CategoriaReservable.Tour, null, null, estado, 1, 1, new BigDecimal("80000"), UnidadCobro.Persona,
                new BigDecimal("80000"), "COP", inicio.atZone(HorarioReservas.ZONA).toLocalDate(), null,
                LocalTime.of(8, 0), inicio, inicio.plusSeconds(4 * 3600), Ofertas.AHORA,
                estado == EstadoReserva.Cancelada ? Ofertas.AHORA : null, UUID.randomUUID(), "h");
        reservas.insertar(reserva);
        return reserva;
    }
}
