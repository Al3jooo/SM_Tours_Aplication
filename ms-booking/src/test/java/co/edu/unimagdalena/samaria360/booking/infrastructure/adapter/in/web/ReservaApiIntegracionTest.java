package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.booking.infrastructure.CatalogoSimulado;
import co.edu.unimagdalena.samaria360.booking.infrastructure.CatalogoSimulado.Unidad;
import co.edu.unimagdalena.samaria360.booking.infrastructure.InventarioDePrueba;
import co.edu.unimagdalena.samaria360.booking.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.booking.infrastructure.RelojAjustable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static co.edu.unimagdalena.samaria360.booking.infrastructure.TokensDePrueba.bearer;
import static co.edu.unimagdalena.samaria360.booking.infrastructure.TokensDePrueba.prestador;
import static co.edu.unimagdalena.samaria360.booking.infrastructure.TokensDePrueba.turista;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * API de reservas contra PostgreSQL y el catálogo simulado: RF20 (confirmación), RF32 (datos congelados), RF37 (precio),
 * RF18 y RF44 (cancelación), D1 (unidades), D6 (idempotencia), seguridad y disponibilidad pública.
 */
@PruebaIntegracion
class ReservaApiIntegracionTest {

    private static final LocalDate ENTRADA = RelojAjustable.HOY.plusDays(20);
    private static final LocalTime OCHO = LocalTime.of(8, 0);

    @Autowired
    private MockMvc mvc;
    @Autowired
    private CatalogoSimulado catalogo;
    @Autowired
    private InventarioDePrueba inventario;
    @Autowired
    private RelojAjustable reloj;
    @Autowired
    private Resilience4JCircuitBreakerFactory circuitos;
    @Autowired
    private ObjectMapper json;

    private final UUID ana = UUID.randomUUID();
    private String hotel;
    private String doble;
    private String suite;
    private String tour;

    @BeforeEach
    void publicar() {
        reloj.reiniciar();
        doble = CatalogoSimulado.nuevoId();
        suite = CatalogoSimulado.nuevoId();
        hotel = catalogo.publicarHotel("Hotel Casa Tayrona", new Unidad(doble, "Doble", 12, 2, "150000"),
                new Unidad(suite, "Suite familiar", 3, 4, "260000"));
        inventario.noches(hotel, doble, ENTRADA, 3, 12, 12);
        inventario.noches(hotel, suite, ENTRADA, 3, 3, 3);
        tour = catalogo.publicarTour("Tour de Café en Minca", 12, "80000");
        inventario.horario(tour, ENTRADA, OCHO, 12, 12);
    }

    @AfterEach
    void cerrarCircuito() {
        circuitos.getCircuitBreakerRegistry().circuitBreaker("catalogo").reset();
    }

    // ------------------------------------------------------------------ reservar (RF20, D1)

    @Test
    void reservarUnTourResponde201ConLaReservaYDescuentaLasPersonas() throws Exception {
        MvcResult resultado = reservar(ana, UUID.randomUUID(), cuerpoTour(3, "240000"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        String cuerpo = cuerpo(resultado);
        String id = JsonPath.read(cuerpo, "$.idReserva");
        assertThat(resultado.getResponse().getHeader(HttpHeaders.LOCATION)).isEqualTo("/api/v1/reservas/" + id);
        assertThat(resultado.getResponse().getHeader("Idempotent-Replayed")).isNull();
        assertThat((String) JsonPath.read(cuerpo, "$.estado")).isEqualTo("Confirmada");
        assertThat((String) JsonPath.read(cuerpo, "$.unidadCobro")).isEqualTo("Persona");
        assertThat((String) JsonPath.read(cuerpo, "$.horaInicio")).isEqualTo("08:00:00");
        assertThat(decimal(JsonPath.read(cuerpo, "$.precioTotal"))).isEqualByComparingTo("240000");
        assertThat((String) JsonPath.read(cuerpo, "$.moneda")).isEqualTo("COP");
        assertThat(inventario.horario(tour, ENTRADA, OCHO)).isEqualTo(9);
    }

    @Test
    void reservarUnaUnidadDescuentaSoloEsaUnidadYGuardaSuNombre() throws Exception {
        MvcResult resultado = reservar(ana, UUID.randomUUID(), cuerpoAlojamiento(suite, 2, 2, 3, "1040000"));

        assertThat(resultado.getResponse().getStatus()).as(cuerpo(resultado)).isEqualTo(201);
        assertThat((String) JsonPath.read(cuerpo(resultado), "$.idUnidad")).isEqualTo(suite);
        assertThat((String) JsonPath.read(cuerpo(resultado), "$.nombreUnidad")).isEqualTo("Suite familiar");
        assertThat((String) JsonPath.read(cuerpo(resultado), "$.fechaSalida")).isEqualTo(ENTRADA.plusDays(2).toString());
        assertThat(inventario.noche(hotel, suite, ENTRADA)).isEqualTo(1);
        assertThat(inventario.noche(hotel, suite, ENTRADA.plusDays(1))).isEqualTo(1);
        assertThat(inventario.noche(hotel, suite, ENTRADA.plusDays(2))).isEqualTo(3);
        assertThat(inventario.noche(hotel, doble, ENTRADA)).isEqualTo(12);
    }

    @Test
    void unaUnidadQueElAlojamientoNoTieneResponde409SinReservarNiDescontar() throws Exception {
        MvcResult resultado = reservar(ana, UUID.randomUUID(),
                cuerpoAlojamiento(CatalogoSimulado.nuevoId(), 1, 1, 1, "150000"));

        assertCodigo(resultado, 409, "UNIDAD_NO_DISPONIBLE");
        assertThat(inventario.reservasDe(hotel)).isZero();
        assertThat(inventario.noche(hotel, doble, ENTRADA)).isEqualTo(12);
    }

    @Test
    void unServicioQueElCatalogoNoMuestraResponde409() throws Exception {
        catalogo.retirar(tour);

        assertCodigo(reservar(ana, UUID.randomUUID(), cuerpoTour(1, "80000")), 409, "OFERTA_NO_DISPONIBLE");
    }

    @Test
    void unaCategoriaNoReservableEsDatosInvalidos() throws Exception {
        String restaurante = catalogo.publicarRestaurante("Restaurante Ouzo");
        Map<String, Object> cuerpo = cuerpoTour(1, "50000");
        cuerpo.put("idServicio", restaurante);

        MvcResult resultado = reservar(ana, UUID.randomUUID(), cuerpo);

        assertCodigo(resultado, 400, "DATOS_INVALIDOS");
        assertThat((String) JsonPath.read(cuerpo(resultado), "$.campos.idServicio")).isEqualTo("CATEGORIA_NO_RESERVABLE");
    }

    @Test
    void losDatosInvalidosLlevanUnCodigoPorCampo() throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("idServicio", "hotel");
        cuerpo.put("fechaEntrada", "mañana");
        cuerpo.put("personas", 0);

        MvcResult resultado = reservar(ana, UUID.randomUUID(), cuerpo);

        assertCodigo(resultado, 400, "DATOS_INVALIDOS");
        assertThat(JsonPath.<Map<String, String>>read(cuerpo(resultado), "$.campos")).containsExactly(
                Map.entry("idServicio", "FORMATO"), Map.entry("fechaEntrada", "FORMATO"),
                Map.entry("personas", "VALOR_MINIMO"), Map.entry("precioTotalEsperado", "REQUERIDO"));
        assertCodigo(mvc.perform(conTurista(post("/api/v1/reservas"), ana).header("Idempotency-Key",
                UUID.randomUUID().toString()).contentType(MediaType.APPLICATION_JSON).content("{no es json"))
                .andReturn(), 400, "DATOS_INVALIDOS");
    }

    // ------------------------------------------------------------------ precio (RF32, RF37)

    @Test
    void rf32_laReservaConservaElPrecioYLosNombresAunqueCambieElCatalogo() throws Exception {
        String id = JsonPath.read(cuerpo(reservar(ana, UUID.randomUUID(), cuerpoAlojamiento(suite, 2, 1, 2,
                "520000"))), "$.idReserva");

        catalogo.publicarHotel(hotel, "Hotel Casa Tayrona Boutique", new Unidad(doble, "Doble", 12, 2, "170000"),
                new Unidad(suite, "Suite Premium", 3, 4, "300000"));
        String guardada = cuerpo(mvc.perform(conTurista(get("/api/v1/reservas/" + id), ana)).andReturn());

        assertThat((String) JsonPath.read(guardada, "$.nombreServicio")).isEqualTo("Hotel Casa Tayrona");
        assertThat((String) JsonPath.read(guardada, "$.nombreUnidad")).isEqualTo("Suite familiar");
        assertThat(decimal(JsonPath.read(guardada, "$.precioUnitario"))).isEqualByComparingTo("260000");
        assertThat(decimal(JsonPath.read(guardada, "$.precioTotal"))).isEqualByComparingTo("520000");
    }

    @Test
    void rf37_conElPrecioCambiadoResponde409ConElPrecioDeLaUnidadVigente() throws Exception {
        catalogo.publicarHotel(hotel, "Hotel Casa Tayrona", new Unidad(doble, "Doble", 12, 2, "150000"),
                new Unidad(suite, "Suite familiar", 3, 4, "280000"));

        MvcResult resultado = reservar(ana, UUID.randomUUID(), cuerpoAlojamiento(suite, 2, 1, 2, "520000"));

        assertCodigo(resultado, 409, "PRECIO_CAMBIADO");
        assertThat(decimal(JsonPath.read(cuerpo(resultado), "$.precioTotal"))).isEqualByComparingTo("560000");
        assertThat(decimal(JsonPath.read(cuerpo(resultado), "$.precioUnitario"))).isEqualByComparingTo("280000");
        assertThat(inventario.noche(hotel, suite, ENTRADA)).isEqualTo(3);
    }

    // ------------------------------------------------------------------ idempotencia (D6)

    @Test
    void laMismaClaveYElMismoCuerpoResponden201ConLaMismaReservaYUnSoloDescuento() throws Exception {
        UUID clave = UUID.randomUUID();
        MvcResult primera = reservar(ana, clave, cuerpoTour(2, "160000"));
        Map<String, Object> mismoPedido = cuerpoTour(2, "160000.00");
        mismoPedido.put("horaInicio", "08:00:00");

        MvcResult segunda = reservar(ana, clave, mismoPedido);

        assertThat(segunda.getResponse().getStatus()).isEqualTo(201);
        assertThat(segunda.getResponse().getHeader("Idempotent-Replayed")).isEqualTo("true");
        assertThat((String) JsonPath.read(cuerpo(segunda), "$.idReserva"))
                .isEqualTo(JsonPath.read(cuerpo(primera), "$.idReserva"));
        assertThat(inventario.horario(tour, ENTRADA, OCHO)).isEqualTo(10);
    }

    @Test
    void laMismaClaveConOtroCuerpoIncluidaOtraUnidadResponde422() throws Exception {
        UUID clave = UUID.randomUUID();
        reservar(ana, clave, cuerpoAlojamiento(suite, 1, 1, 2, "260000"));

        assertCodigo(reservar(ana, clave, cuerpoAlojamiento(doble, 1, 1, 2, "150000")), 422, "IDEMPOTENCIA_CONFLICTO");
        assertThat(inventario.noche(hotel, doble, ENTRADA)).isEqualTo(12);
    }

    @Test
    void laClaveEsDeCadaTurista() throws Exception {
        UUID clave = UUID.randomUUID();
        reservar(ana, clave, cuerpoTour(1, "80000"));

        MvcResult deOtro = reservar(UUID.randomUUID(), clave, cuerpoTour(1, "80000"));

        assertThat(deOtro.getResponse().getStatus()).isEqualTo(201);
        assertThat(deOtro.getResponse().getHeader("Idempotent-Replayed")).isNull();
    }

    @Test
    void sinClaveOConUnaClaveQueNoEsUuidResponde400() throws Exception {
        MvcResult sinClave = mvc.perform(conTurista(post("/api/v1/reservas"), ana)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpoTour(1, "80000"))))
                .andReturn();
        MvcResult claveInvalida = mvc.perform(conTurista(post("/api/v1/reservas"), ana)
                .header("Idempotency-Key", "abc").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpoTour(1, "80000")))).andReturn();

        assertCodigo(sinClave, 400, "IDEMPOTENCIA_REQUERIDA");
        assertCodigo(claveInvalida, 400, "IDEMPOTENCIA_REQUERIDA");
        assertThat(inventario.horario(tour, ENTRADA, OCHO)).isEqualTo(12);
    }

    @Test
    void unCupoAgotadoNoGuardaLaClave() throws Exception {
        UUID clave = UUID.randomUUID();
        assertCodigo(reservar(ana, clave, cuerpoAlojamiento(suite, 1, 4, 4, "1040000")), 400, "DATOS_INVALIDOS");
        reservar(UUID.randomUUID(), UUID.randomUUID(), cuerpoAlojamiento(suite, 1, 3, 4, "780000"));
        assertCodigo(reservar(ana, clave, cuerpoAlojamiento(suite, 1, 1, 2, "260000")), 409, "CUPO_AGOTADO");

        // La misma clave con otro pedido no es un conflicto: el intento fallido no quedó guardado.
        assertThat(reservar(ana, clave, cuerpoAlojamiento(doble, 1, 1, 2, "150000")).getResponse().getStatus())
                .isEqualTo(201);
    }

    // ------------------------------------------------------------------ consultar y cancelar (RF18, RF44)

    @Test
    void misReservasSoloTraeLasPropiasDeLaMasRecienteALaMasAntigua() throws Exception {
        UUID beto = UUID.randomUUID();
        String primera = JsonPath.read(cuerpo(reservar(beto, UUID.randomUUID(), cuerpoTour(1, "80000"))), "$.idReserva");
        reloj.fijar(RelojAjustable.INICIO.plusSeconds(60));
        String segunda = JsonPath.read(cuerpo(reservar(beto, UUID.randomUUID(), cuerpoAlojamiento(doble, 1, 1, 1,
                "150000"))), "$.idReserva");
        reservar(ana, UUID.randomUUID(), cuerpoTour(1, "80000"));

        String pagina = cuerpo(mvc.perform(conTurista(get("/api/v1/reservas/mias"), beto)).andReturn());

        assertThat(JsonPath.<List<String>>read(pagina, "$.elementos[*].idReserva")).containsExactly(segunda, primera);
        assertThat((Integer) JsonPath.read(pagina, "$.tamano")).isEqualTo(20);
        assertThat((Integer) JsonPath.read(pagina, "$.totalElementos")).isEqualTo(2);
    }

    @Test
    void laReservaDeOtroTuristaNoExisteNiParaConsultarNiParaCancelar() throws Exception {
        String id = JsonPath.read(cuerpo(reservar(ana, UUID.randomUUID(), cuerpoTour(1, "80000"))), "$.idReserva");
        UUID otro = UUID.randomUUID();

        assertCodigo(mvc.perform(conTurista(get("/api/v1/reservas/" + id), otro)).andReturn(), 404,
                "RESERVA_NO_ENCONTRADA");
        assertCodigo(mvc.perform(conTurista(post("/api/v1/reservas/" + id + "/cancelacion"), otro)).andReturn(), 404,
                "RESERVA_NO_ENCONTRADA");
        assertCodigo(mvc.perform(conTurista(get("/api/v1/reservas/no-es-un-uuid"), ana)).andReturn(), 404,
                "RESERVA_NO_ENCONTRADA");
        assertThat(inventario.horario(tour, ENTRADA, OCHO)).isEqualTo(11);
    }

    @Test
    void cancelarDevuelveLosCuposYCancelarOtraVezNoLiberaDosVeces() throws Exception {
        String id = JsonPath.read(cuerpo(reservar(ana, UUID.randomUUID(), cuerpoAlojamiento(suite, 2, 2, 4,
                "1040000"))), "$.idReserva");

        MvcResult cancelada = mvc.perform(conTurista(post("/api/v1/reservas/" + id + "/cancelacion"), ana)).andReturn();
        MvcResult otraVez = mvc.perform(conTurista(post("/api/v1/reservas/" + id + "/cancelacion"), ana)).andReturn();

        assertThat(cancelada.getResponse().getStatus()).isEqualTo(200);
        assertThat((String) JsonPath.read(cuerpo(cancelada), "$.estado")).isEqualTo("Cancelada");
        assertThat(otraVez.getResponse().getStatus()).isEqualTo(200);
        assertThat(inventario.noche(hotel, suite, ENTRADA)).isEqualTo(3);
        assertThat(inventario.noche(hotel, suite, ENTRADA.plusDays(1))).isEqualTo(3);
    }

    @Test
    void rf44_conExactamente24HorasSeCancelaYUnSegundoDespuesNo() throws Exception {
        String primera = cuerpo(reservar(ana, UUID.randomUUID(), cuerpoTour(1, "80000")));
        String segunda = cuerpo(reservar(ana, UUID.randomUUID(), cuerpoTour(1, "80000")));
        Instant limite = Instant.parse(JsonPath.read(primera, "$.cancelableHasta"));

        reloj.fijar(limite);
        MvcResult aTiempo = mvc.perform(conTurista(post("/api/v1/reservas/" + JsonPath.read(primera, "$.idReserva")
                + "/cancelacion"), ana)).andReturn();
        reloj.fijar(limite.plusSeconds(1));
        MvcResult tarde = mvc.perform(conTurista(post("/api/v1/reservas/" + JsonPath.read(segunda, "$.idReserva")
                + "/cancelacion"), ana)).andReturn();

        assertThat(aTiempo.getResponse().getStatus()).isEqualTo(200);
        assertCodigo(tarde, 409, "CANCELACION_FUERA_DE_PLAZO");
        assertThat((String) JsonPath.read(cuerpo(tarde), "$.detail")).contains("tiempo límite");
        assertThat(inventario.horario(tour, ENTRADA, OCHO)).isEqualTo(11);
    }

    // ------------------------------------------------------------------ catálogo caído

    @Test
    void conElCatalogoCaidoReservarResponde503PeroConsultarYCancelarFuncionan() throws Exception {
        String id = JsonPath.read(cuerpo(reservar(ana, UUID.randomUUID(), cuerpoTour(1, "80000"))), "$.idReserva");
        catalogo.fallar(tour, 503, -1);
        try {
            MvcResult caido = reservar(ana, UUID.randomUUID(), cuerpoTour(1, "80000"));

            assertCodigo(caido, 503, "SERVICIO_NO_DISPONIBLE");
            assertThat((String) JsonPath.read(cuerpo(caido), "$.servicio")).isEqualTo("ms-catalog");
            assertThat(mvc.perform(conTurista(get("/api/v1/reservas/mias"), ana)).andReturn().getResponse()
                    .getStatus()).isEqualTo(200);
            assertThat(mvc.perform(conTurista(post("/api/v1/reservas/" + id + "/cancelacion"), ana)).andReturn()
                    .getResponse().getStatus()).isEqualTo(200);
        } finally {
            catalogo.normalizar(tour);
        }
    }

    // ------------------------------------------------------------------ seguridad

    @Test
    void reservarExigeUnTokenDelRolTurista() throws Exception {
        String cuerpo = json.writeValueAsString(cuerpoTour(1, "80000"));

        assertCodigo(mvc.perform(post("/api/v1/reservas").header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn(), 401, "NO_AUTENTICADO");
        assertCodigo(mvc.perform(post("/api/v1/reservas").header(HttpHeaders.AUTHORIZATION,
                        bearer(prestador(UUID.randomUUID()))).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn(), 403, "ACCESO_DENEGADO");
        assertCodigo(mvc.perform(get("/api/v1/reservas/mias")).andReturn(), 401, "NO_AUTENTICADO");
        assertCodigo(mvc.perform(get("/api/v1/reservas/mias").header(HttpHeaders.AUTHORIZATION, "Bearer x.y.z"))
                .andReturn(), 401, "NO_AUTENTICADO");
        assertThat(inventario.horario(tour, ENTRADA, OCHO)).isEqualTo(12);
    }

    // ------------------------------------------------------------------ disponibilidad (pública)

    @Test
    void laDisponibilidadEsPublicaYSeAgrupaPorUnidad() throws Exception {
        reservar(ana, UUID.randomUUID(), cuerpoAlojamiento(suite, 1, 3, 4, "780000"));

        String respuesta = cuerpo(mvc.perform(get("/api/v1/reservas/disponibilidad").param("servicio", hotel)
                .param("desde", ENTRADA.toString()).param("hasta", ENTRADA.plusDays(1).toString())).andReturn());

        assertThat((String) JsonPath.read(respuesta, "$.categoria")).isEqualTo("Alojamiento");
        assertThat(JsonPath.<List<String>>read(respuesta, "$.unidades[*].idUnidad")).containsExactlyInAnyOrder(doble, suite);
        assertThat(JsonPath.<List<Integer>>read(respuesta,
                "$.unidades[?(@.idUnidad == '" + suite + "')].noches[*].cuposDisponibles")).containsExactly(0, 3);
        assertThat(JsonPath.<List<Integer>>read(respuesta,
                "$.unidades[?(@.idUnidad == '" + doble + "')].noches[*].cuposDisponibles")).containsExactly(12, 12);
        assertThat(JsonPath.<List<Object>>read(respuesta, "$.horarios")).isEmpty();
    }

    @Test
    void elResumenDeLasTarjetasEsPublicoYDiceConCuposMientrasQuedeAlgunaUnidad() throws Exception {
        reservar(ana, UUID.randomUUID(), cuerpoAlojamiento(suite, 3, 3, 4, "2340000"));
        String sinInventario = CatalogoSimulado.nuevoId();

        String resumen = cuerpo(mvc.perform(get("/api/v1/reservas/disponibilidad/resumen")
                .param("servicios", hotel + "," + sinInventario).param("desde", ENTRADA.toString())
                .param("hasta", ENTRADA.plusDays(2).toString())).andReturn());

        assertThat(JsonPath.<Map<String, String>>read(resumen, "$")).containsExactly(
                Map.entry(hotel, "CON_CUPOS"), Map.entry(sinInventario, "SIN_INVENTARIO"));
        MvcResult demasiados = mvc.perform(get("/api/v1/reservas/disponibilidad/resumen").param("servicios",
                String.join(",", java.util.Collections.nCopies(11, "x")))).andReturn();
        assertCodigo(demasiados, 400, "DATOS_INVALIDOS");
    }

    // ------------------------------------------------------------------ auxiliares

    private MvcResult reservar(UUID turista, UUID clave, Map<String, Object> cuerpo) throws Exception {
        return mvc.perform(conTurista(post("/api/v1/reservas"), turista)
                .header("Idempotency-Key", clave.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo))).andReturn();
    }

    private static MockHttpServletRequestBuilder conTurista(MockHttpServletRequestBuilder solicitud, UUID turista) {
        return solicitud.header(HttpHeaders.AUTHORIZATION, bearer(turista(turista)));
    }

    private Map<String, Object> cuerpoTour(int personas, String total) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("idServicio", tour);
        cuerpo.put("fechaEntrada", ENTRADA.toString());
        cuerpo.put("horaInicio", "08:00");
        cuerpo.put("personas", personas);
        cuerpo.put("precioTotalEsperado", new BigDecimal(total));
        return cuerpo;
    }

    private Map<String, Object> cuerpoAlojamiento(String unidad, int noches, int habitaciones, int personas,
                                                  String total) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("idServicio", hotel);
        cuerpo.put("idUnidad", unidad);
        cuerpo.put("fechaEntrada", ENTRADA.toString());
        cuerpo.put("fechaSalida", ENTRADA.plusDays(noches).toString());
        cuerpo.put("cantidad", habitaciones);
        cuerpo.put("personas", personas);
        cuerpo.put("precioTotalEsperado", new BigDecimal(total));
        return cuerpo;
    }

    private static void assertCodigo(MvcResult resultado, int estado, String codigo) throws Exception {
        assertThat(resultado.getResponse().getStatus()).as(cuerpo(resultado)).isEqualTo(estado);
        assertThat(resultado.getResponse().getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat((String) JsonPath.read(cuerpo(resultado), "$.codigo")).isEqualTo(codigo);
    }

    private static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static BigDecimal decimal(Object valor) {
        return new BigDecimal(valor.toString());
    }
}
