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
import org.springframework.jdbc.core.JdbcTemplate;
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
import static co.edu.unimagdalena.samaria360.booking.infrastructure.TokensDePrueba.prestador;
import static co.edu.unimagdalena.samaria360.booking.infrastructure.TokensDePrueba.turista;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * CU «Consultar historial» del lado del Prestador (RF12; DEC-191, DEC-192) contra PostgreSQL: solo las reservas de
 * sus servicios, filtros, orden, páginas de 20, sin datos del Turista, y la seguridad por rol. Las reservas se
 * insertan directamente, con un prestador nuevo por prueba.
 */
@PruebaIntegracion
class ReservasPrestadorIntegracionTest {

    private static final String RUTA = "/api/v1/reservas/prestador";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ReservaPersistenceAdapter reservas;
    @Autowired
    private PlatformTransactionManager transacciones;
    @Autowired
    private JdbcTemplate jdbc;

    private final UUID duena = UUID.randomUUID();
    private final UUID turistaAna = UUID.randomUUID();
    private final String kayak = CatalogoSimulado.nuevoId();
    private final String cafe = CatalogoSimulado.nuevoId();

    @Test
    void elPrestadorSoloVeLasReservasDeSusServiciosSinDatosDelTurista() throws Exception {
        UUID propia = insertar(duena, kayak, en(2030, 12, 5, 8, 0), EstadoReserva.Confirmada);
        insertar(UUID.randomUUID(), cafe, en(2030, 12, 6, 8, 0), EstadoReserva.Confirmada);

        MvcResult resultado = listar("", duena);

        assertThat(ids(resultado)).containsExactly(propia.toString());
        String json = cuerpo(resultado);
        assertThat(JsonPath.<Integer>read(json, "$.tamano")).isEqualTo(20);
        assertThat(JsonPath.<Map<String, Object>>read(json, "$.elementos[0]")).containsKeys("idReserva", "estado",
                "idServicio", "nombreServicio", "fechaInicio", "precioUnitario", "precioTotal")
                .doesNotContainKeys("idTurista", "cancelableHasta");
        assertThat(json).doesNotContain(turistaAna.toString());
    }

    @Test
    void filtraPorEstadoPorElRangoDeLaActividadYPorServicio() throws Exception {
        UUID confirmada = insertar(duena, kayak, en(2030, 12, 5, 8, 0), EstadoReserva.Confirmada);
        UUID cancelada = insertar(duena, kayak, en(2030, 12, 6, 8, 0), EstadoReserva.Cancelada);
        UUID completada = insertar(duena, cafe, en(2030, 12, 7, 8, 0), EstadoReserva.Completada);
        UUID enero = insertar(duena, cafe, en(2031, 1, 1, 0, 0), EstadoReserva.Confirmada);

        assertThat(ids(listar("?desde=2030-12-01&hasta=2030-12-31", duena)))
                .containsExactly(completada.toString(), cancelada.toString(), confirmada.toString());
        assertThat(ids(listar("?estado=Cancelada,Completada", duena)))
                .containsExactly(completada.toString(), cancelada.toString());
        assertThat(ids(listar("?servicio=" + cafe, duena))).containsExactly(enero.toString(), completada.toString());
        assertThat(ids(listar("?desde=2031-01-01", duena))).containsExactly(enero.toString());
    }

    @Test
    void paginaDeVeinteEnVeinteDeLaActividadMasTardiaALaMasTemprana() throws Exception {
        for (int i = 0; i < 21; i++) {
            insertar(duena, kayak, en(2030, 12, 1, 8, 0).plusSeconds(i * 86_400L), EstadoReserva.Confirmada);
        }

        String primera = cuerpo(listar("", duena));
        String segunda = cuerpo(listar("?pagina=1", duena));

        assertThat(JsonPath.<List<String>>read(primera, "$.elementos[*].fechaInicio")).hasSize(20)
                .first().isEqualTo(en(2030, 12, 21, 8, 0).toString());
        assertThat(JsonPath.<Integer>read(primera, "$.totalElementos")).isEqualTo(21);
        assertThat(JsonPath.<Integer>read(primera, "$.totalPaginas")).isEqualTo(2);
        assertThat(JsonPath.<List<String>>read(segunda, "$.elementos[*].fechaInicio"))
                .containsExactly(en(2030, 12, 1, 8, 0).toString());
    }

    @Test
    void losFiltrosInvalidosSonDatosInvalidos() throws Exception {
        MvcResult resultado = listar("?desde=2030-12-31&hasta=2030-12-01&servicio=abc", duena);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo")).isEqualTo("DATOS_INVALIDOS");
        assertThat(JsonPath.<Map<String, String>>read(cuerpo(resultado), "$.campos"))
                .containsExactly(Map.entry("hasta", "RANGO_INVALIDO"), Map.entry("servicio", "FORMATO"));
        assertThat(listar("?desde=2030-01-01&hasta=2031-01-02", duena).getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    void soloElRolPrestadorYElPrestadorNoEntraAMisReservas() throws Exception {
        assertThat(mvc.perform(get(RUTA).header(HttpHeaders.AUTHORIZATION, bearer(turista(turistaAna))))
                .andReturn().getResponse().getStatus()).isEqualTo(403);
        assertThat(mvc.perform(get(RUTA)).andReturn().getResponse().getStatus()).isEqualTo(401);
        assertThat(mvc.perform(get("/api/v1/reservas/mias").header(HttpHeaders.AUTHORIZATION,
                bearer(prestador(duena)))).andReturn().getResponse().getStatus()).isEqualTo(403);
    }

    /** DEC-191: la consulta tiene su índice (V6), como la del Turista (V2). */
    @Test
    void v6CreaElIndiceDelHistorialDelPrestador() {
        String definicion = jdbc.queryForObject(
                "SELECT indexdef FROM pg_indexes WHERE indexname = 'ix_reserva_prestador_inicio'", String.class);

        assertThat(definicion).contains("(id_prestador, fecha_inicio DESC, id_reserva DESC)");
    }

    // ------------------------------------------------------------------ auxiliares

    private MvcResult listar(String consulta, UUID quien) throws Exception {
        return mvc.perform(get(RUTA + consulta).header(HttpHeaders.AUTHORIZATION, bearer(prestador(quien))))
                .andReturn();
    }

    private static List<String> ids(MvcResult resultado) throws Exception {
        assertThat(resultado.getResponse().getStatus()).as(cuerpo(resultado)).isEqualTo(200);
        return JsonPath.read(cuerpo(resultado), "$.elementos[*].idReserva");
    }

    /** Un tour de 1 persona de 4 horas (la forma de la fila cumple ck_reserva_forma). */
    private UUID insertar(UUID idPrestador, String idServicio, Instant inicio, EstadoReserva estado) {
        UUID id = UUID.randomUUID();
        Reserva reserva = new Reserva(id, turistaAna, idPrestador, idServicio, "Tour", CategoriaReservable.Tour,
                null, null, estado, 1, 1, new BigDecimal("80000"), UnidadCobro.Persona, new BigDecimal("80000"),
                "COP", inicio.atZone(HorarioReservas.ZONA).toLocalDate(), null,
                inicio.atZone(HorarioReservas.ZONA).toLocalTime(), inicio, inicio.plusSeconds(4 * 3600),
                RelojAjustable.INICIO, estado == EstadoReserva.Cancelada ? RelojAjustable.INICIO : null,
                UUID.randomUUID(), "a".repeat(64));
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
