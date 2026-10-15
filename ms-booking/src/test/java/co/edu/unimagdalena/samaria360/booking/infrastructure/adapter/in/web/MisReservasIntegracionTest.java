package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.booking.domain.model.CategoriaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.EstadoReserva;
import co.edu.unimagdalena.samaria360.booking.domain.model.HorarioReservas;
import co.edu.unimagdalena.samaria360.booking.domain.model.Reserva;
import co.edu.unimagdalena.samaria360.booking.domain.model.UnidadCobro;
import co.edu.unimagdalena.samaria360.booking.infrastructure.CatalogoSimulado;
import co.edu.unimagdalena.samaria360.booking.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.booking.infrastructure.RelojAjustable;
import co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.out.persistence.ReservaPersistenceAdapter;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static co.edu.unimagdalena.samaria360.booking.infrastructure.TokensDePrueba.bearer;
import static co.edu.unimagdalena.samaria360.booking.infrastructure.TokensDePrueba.turista;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * «Mis reservas» con filtros (RF12, del lado del turista) contra PostgreSQL: la actividad cruza el rango de fechas de
 * Santa Marta, varios estados, orden por inicio descendente con desempate por id, validaciones y
 * {@code cancelableHasta}. Las reservas se insertan directamente, con las fechas exactas de cada caso.
 */
@PruebaIntegracion
class MisReservasIntegracionTest {

    private static final String DICIEMBRE = "desde=2030-12-01&hasta=2030-12-31";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ReservaPersistenceAdapter reservas;
    @Autowired
    private PlatformTransactionManager transacciones;

    private final UUID ana = UUID.randomUUID();

    @Test
    void laActividadDebeCruzarElRangoDeFechas() throws Exception {
        UUID antesYDentro = insertar(en(2030, 11, 30, 15, 0), en(2030, 12, 2, 12, 0), EstadoReserva.Confirmada);
        UUID dentroYDespues = insertar(en(2030, 12, 30, 15, 0), en(2031, 1, 2, 12, 0), EstadoReserva.Confirmada);
        UUID tourDiaHasta = insertar(en(2030, 12, 31, 8, 0), en(2030, 12, 31, 12, 0), EstadoReserva.Confirmada);
        insertar(en(2030, 11, 30, 20, 0), en(2030, 12, 1, 0, 0), EstadoReserva.Confirmada); // termina a las 00:00
        insertar(en(2031, 1, 1, 0, 0), en(2031, 1, 1, 4, 0), EstadoReserva.Confirmada); // empieza el día siguiente

        String pagina = cuerpo(mias(DICIEMBRE));

        // Por inicio descendente: el tour del 31 (08:00), la estadía del 30 (15:00) y la del 30 de noviembre.
        assertThat(JsonPath.<List<String>>read(pagina, "$.elementos[*].idReserva")).containsExactly(
                tourDiaHasta.toString(), dentroYDespues.toString(), antesYDentro.toString());
        assertThat((Integer) JsonPath.read(pagina, "$.totalElementos")).isEqualTo(3);
    }

    @Test
    void conUnSoloExtremoElRangoQuedaAbierto() throws Exception {
        UUID vieja = insertar(en(2030, 5, 1, 8, 0), en(2030, 5, 1, 12, 0), EstadoReserva.Completada);
        UUID nueva = insertar(en(2031, 5, 1, 8, 0), en(2031, 5, 1, 12, 0), EstadoReserva.Confirmada);

        assertThat(ids(mias("desde=2030-12-01"))).containsExactly(nueva.toString());
        assertThat(ids(mias("hasta=2030-12-01"))).containsExactly(vieja.toString());
    }

    @Test
    void elEstadoAdmiteVariosValoresSeparadosPorComa() throws Exception {
        insertar(en(2030, 12, 5, 8, 0), en(2030, 12, 5, 12, 0), EstadoReserva.Confirmada);
        UUID cancelada = insertar(en(2030, 12, 6, 8, 0), en(2030, 12, 6, 12, 0), EstadoReserva.Cancelada);
        UUID completada = insertar(en(2030, 12, 7, 8, 0), en(2030, 12, 7, 12, 0), EstadoReserva.Completada);

        assertThat(ids(mias(DICIEMBRE + "&estado=Cancelada,Completada")))
                .containsExactly(completada.toString(), cancelada.toString());
        assertThat(ids(mias("estado=Cancelada"))).containsExactly(cancelada.toString());
    }

    @Test
    void elOrdenEsPorInicioDescendenteConDesempatePorId() throws Exception {
        Instant inicio = en(2030, 12, 10, 8, 0);
        UUID menor = UUID.fromString("00000000-0000-4000-8000-00000000000a");
        UUID mayor = UUID.fromString("ffffffff-0000-4000-8000-00000000000b");
        insertar(menor, inicio, inicio.plusSeconds(4 * 3600), EstadoReserva.Confirmada);
        insertar(mayor, inicio, inicio.plusSeconds(4 * 3600), EstadoReserva.Confirmada);
        UUID despues = insertar(en(2030, 12, 11, 8, 0), en(2030, 12, 11, 12, 0), EstadoReserva.Confirmada);

        // Desempate por el orden de uuid de PostgreSQL (bytes sin signo): ffff… va antes que 0000… en descendente.
        assertThat(ids(mias(DICIEMBRE))).containsExactly(despues.toString(), mayor.toString(), menor.toString());
    }

    @Test
    void cancelableHastaLoCalculaElServidorYViajaEnMiasYEnElDetalle() throws Exception {
        Instant inicio = en(2030, 12, 20, 15, 0);
        UUID id = insertar(inicio, en(2030, 12, 22, 12, 0), EstadoReserva.Confirmada);

        String esperado = inicio.minusSeconds(24 * 3600).toString();
        assertThat((String) JsonPath.read(cuerpo(mias(DICIEMBRE)), "$.elementos[0].cancelableHasta")).isEqualTo(esperado);
        assertThat((String) JsonPath.read(cuerpo(mvc.perform(get("/api/v1/reservas/" + id)
                .header(HttpHeaders.AUTHORIZATION, bearer(turista(ana)))).andReturn()), "$.cancelableHasta"))
                .isEqualTo(esperado);
    }

    @Test
    void losFiltrosInvalidosSonDatosInvalidos() throws Exception {
        assertCampos(mias("desde=2030-12-31&hasta=2030-12-01"), Map.of("hasta", "RANGO_INVALIDO"));
        assertCampos(mias("desde=2030-01-01&hasta=2031-01-02"), Map.of("hasta", "MAXIMO_DIAS"));
        assertCampos(mias("estado=Pendiente"), Map.of("estado", "NO_PERMITIDO"));
        assertCampos(mias("desde=31/12/2030"), Map.of("desde", "FORMATO"));
        assertThat(mias("desde=2030-01-01&hasta=2031-01-01").getResponse().getStatus()).isEqualTo(200);
    }

    // ------------------------------------------------------------------ auxiliares

    private MvcResult mias(String consulta) throws Exception {
        return mvc.perform(get("/api/v1/reservas/mias?" + consulta)
                .header(HttpHeaders.AUTHORIZATION, bearer(turista(ana)))).andReturn();
    }

    private static List<String> ids(MvcResult resultado) throws Exception {
        assertThat(resultado.getResponse().getStatus()).as(cuerpo(resultado)).isEqualTo(200);
        return JsonPath.read(cuerpo(resultado), "$.elementos[*].idReserva");
    }

    private static void assertCampos(MvcResult resultado, Map<String, String> campos) throws Exception {
        assertThat(resultado.getResponse().getStatus()).as(cuerpo(resultado)).isEqualTo(400);
        assertThat(resultado.getResponse().getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat((String) JsonPath.read(cuerpo(resultado), "$.codigo")).isEqualTo("DATOS_INVALIDOS");
        assertThat(JsonPath.<Map<String, String>>read(cuerpo(resultado), "$.campos")).isEqualTo(campos);
    }

    private UUID insertar(Instant inicio, Instant fin, EstadoReserva estado) {
        return insertar(UUID.randomUUID(), inicio, fin, estado);
    }

    /** Un tour de 1 persona con las fechas exactas del caso (la forma de la fila cumple ck_reserva_forma). */
    private UUID insertar(UUID id, Instant inicio, Instant fin, EstadoReserva estado) {
        Reserva reserva = new Reserva(id, ana, CatalogoSimulado.PRESTADOR, CatalogoSimulado.nuevoId(), "Tour",
                CategoriaReservable.Tour, null, null, estado, 1, 1, new BigDecimal("80000"), UnidadCobro.Persona,
                new BigDecimal("80000"), "COP", inicio.atZone(HorarioReservas.ZONA).toLocalDate(), null,
                inicio.atZone(HorarioReservas.ZONA).toLocalTime(), inicio, fin, RelojAjustable.INICIO,
                estado == EstadoReserva.Cancelada ? RelojAjustable.INICIO : null, UUID.randomUUID(), "a".repeat(64));
        new TransactionTemplate(transacciones).executeWithoutResult(t -> reservas.insertar(reserva));
        return id;
    }

    private static Instant en(int anio, int mes, int dia, int hora, int minuto) {
        return HorarioReservas.instante(LocalDate.of(anio, mes, dia), LocalTime.of(hora, minuto));
    }

    private static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
