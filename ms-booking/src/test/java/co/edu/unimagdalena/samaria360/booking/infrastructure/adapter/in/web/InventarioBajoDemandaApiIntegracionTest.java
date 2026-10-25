package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.booking.infrastructure.CatalogoSimulado;
import co.edu.unimagdalena.samaria360.booking.infrastructure.CatalogoSimulado.Unidad;
import co.edu.unimagdalena.samaria360.booking.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.booking.infrastructure.RelojAjustable;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.circuitbreaker.resilience4j.Resilience4JCircuitBreakerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static co.edu.unimagdalena.samaria360.booking.infrastructure.RelojAjustable.HOY;
import static co.edu.unimagdalena.samaria360.booking.infrastructure.TokensDePrueba.bearer;
import static co.edu.unimagdalena.samaria360.booking.infrastructure.TokensDePrueba.turista;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Inventario bajo demanda de extremo a extremo (DEC-152 a DEC-155): la disponibilidad, el resumen de las tarjetas y
 * reservar materializan la oferta del catálogo; con el catálogo caído o lento no se responde «sin fechas».
 */
@PruebaIntegracion
class InventarioBajoDemandaApiIntegracionTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private CatalogoSimulado catalogo;
    @Autowired
    private RelojAjustable reloj;
    @Autowired
    private Resilience4JCircuitBreakerFactory circuitos;
    @Autowired
    private ObjectMapper json;

    /**
     * Solo si el circuito ya existe: {@code circuitBreaker("catalogo")} lo crearía con la configuración por defecto
     * antes del primer {@code run()}, y las clases siguientes (que comparten el contexto) heredarían ese circuito.
     */
    @BeforeEach
    @AfterEach
    void reiniciar() {
        reloj.reiniciar();
        circuitos.getCircuitBreakerRegistry().find("catalogo").ifPresent(CircuitBreaker::reset);
    }

    @Test
    void laDisponibilidadDeUnTourNuevoTraeSusSalidasDeLosProximosSieteDias() throws Exception {
        String tour = catalogo.publicarTour("Tour de Café en Minca", 15, "150000", CatalogoSimulado.TODOS_LOS_DIAS,
                List.of("08:00", "14:00"));

        String respuesta = cuerpo(disponibilidad(tour, HOY, HOY.plusDays(6)));

        assertThat((String) JsonPath.read(respuesta, "$.categoria")).isEqualTo("Tour");
        assertThat(JsonPath.<List<Object>>read(respuesta, "$.horarios")).hasSize(7 * 2);
        assertThat(JsonPath.<List<Integer>>read(respuesta, "$.horarios[*].cuposDisponibles")).containsOnly(15);
        assertThat(JsonPath.<List<String>>read(respuesta, "$.horarios[*].hora")).containsOnly("08:00:00", "14:00:00");
    }

    @Test
    void unDiaQueNoOperaEs409YUnoQueOperaSeReserva() throws Exception {
        String tour = catalogo.publicarTour("Tour fin de semana", 12, "80000", List.of("Sabado", "Domingo"),
                List.of("08:00"));
        LocalDate miercoles = LocalDate.of(2030, 3, 20);
        LocalDate sabado = LocalDate.of(2030, 3, 23);

        assertCodigo(reservar(tour, miercoles), 409, "FECHA_NO_DISPONIBLE");
        MvcResult reservada = reservar(tour, sabado);

        assertThat(reservada.getResponse().getStatus()).as(cuerpo(reservada)).isEqualTo(201);
        String respuesta = cuerpo(disponibilidad(tour, miercoles, sabado));
        assertThat(JsonPath.<List<String>>read(respuesta, "$.horarios[*].fecha")).containsExactly("2030-03-23");
        assertThat(JsonPath.<List<Integer>>read(respuesta, "$.horarios[*].cuposDisponibles")).containsExactly(10);
    }

    @Test
    void unServicioQueElCatalogoNoMuestraNoSeMaterializa() throws Exception {
        String retirado = CatalogoSimulado.nuevoId();

        String primera = cuerpo(disponibilidad(retirado, HOY, HOY.plusDays(6)));
        disponibilidad(retirado, HOY, HOY.plusDays(6));

        assertThat((Object) JsonPath.read(primera, "$.categoria")).isNull();
        assertThat(JsonPath.<List<Object>>read(primera, "$.horarios")).isEmpty();
        assertThat(catalogo.llamadas(retirado)).as("sin marca: se vuelve a preguntar").isEqualTo(2);
    }

    @Test
    void conElCatalogoCaidoSinCoberturaResponde503YConCoberturaSirveLasFilas() throws Exception {
        String cubierto = catalogo.publicarHotel("Hotel", CatalogoSimulado.TODOS_LOS_DIAS,
                new Unidad(CatalogoSimulado.nuevoId(), "Doble", 4, 2, "150000"));
        String nuevo = catalogo.publicarTour("Tour", 12, "80000", CatalogoSimulado.TODOS_LOS_DIAS, List.of("08:00"));
        disponibilidad(cubierto, HOY, HOY.plusDays(59));
        catalogo.fallar(cubierto, 503, -1);
        catalogo.fallar(nuevo, 503, -1);
        try {
            MvcResult sinCobertura = disponibilidad(nuevo, HOY, HOY.plusDays(6));
            MvcResult conCobertura = disponibilidad(cubierto, HOY, HOY.plusDays(6));

            assertCodigo(sinCobertura, 503, "SERVICIO_NO_DISPONIBLE");
            assertThat((String) JsonPath.read(cuerpo(sinCobertura), "$.servicio")).isEqualTo("ms-catalog");
            assertThat(conCobertura.getResponse().getStatus()).isEqualTo(200);
            assertThat(JsonPath.<List<Object>>read(cuerpo(conCobertura), "$.unidades[0].noches")).hasSize(7);
            assertThat(catalogo.llamadas(cubierto)).as("cubierto: no se vuelve a llamar").isEqualTo(1);
            assertCodigo(resumen(List.of(cubierto, nuevo)), 503, "SERVICIO_NO_DISPONIBLE");
        } finally {
            catalogo.normalizar(cubierto);
            catalogo.normalizar(nuevo);
        }
    }

    @Test
    void elResumenMaterializaLosServiciosNuevosParaLasTarjetas() throws Exception {
        String tour = catalogo.publicarTour("Tour", 12, "80000", CatalogoSimulado.TODOS_LOS_DIAS, List.of("08:00"));
        String restaurante = catalogo.publicarRestaurante("Restaurante");

        String respuesta = cuerpo(resumen(List.of(tour, restaurante)));

        assertThat(JsonPath.<Map<String, String>>read(respuesta, "$")).containsExactly(
                Map.entry(tour, "CON_CUPOS"), Map.entry(restaurante, "SIN_INVENTARIO"));
    }

    /** DEC-155: varias respuestas exitosas cerca del timeout de lectura (1,5 s) agotan el plazo de 2,5 s. */
    @Test
    void conElCatalogoLentoElResumenResponde503AntesDelGatewayYNuncaSinInventario() throws Exception {
        List<String> lentos = List.of(
                catalogo.publicarTour("Lento 1", 12, "80000", CatalogoSimulado.TODOS_LOS_DIAS, List.of("08:00")),
                catalogo.publicarTour("Lento 2", 12, "80000", CatalogoSimulado.TODOS_LOS_DIAS, List.of("08:00")),
                catalogo.publicarTour("Lento 3", 12, "80000", CatalogoSimulado.TODOS_LOS_DIAS, List.of("08:00")));
        lentos.forEach(id -> catalogo.demorar(id, Duration.ofMillis(1200)));
        long inicio = System.nanoTime();
        MvcResult resultado;
        try {
            resultado = resumen(lentos);
        } finally {
            lentos.forEach(catalogo::normalizar);
        }
        Duration tardo = Duration.ofNanos(System.nanoTime() - inicio);

        assertCodigo(resultado, 503, "SERVICIO_NO_DISPONIBLE");
        assertThat(cuerpo(resultado)).doesNotContain("SIN_INVENTARIO");
        assertThat(tardo).isBetween(Duration.ofMillis(2400), Duration.ofMillis(3000));
    }

    @Test
    void losIdsQueNoSonObjectIdSonDatosInvalidosSinLlamarAlCatalogo() throws Exception {
        String valido = CatalogoSimulado.nuevoId();

        MvcResult consulta = mvc.perform(get("/api/v1/reservas/disponibilidad").param("servicio", "no-es-un-id"))
                .andReturn();
        MvcResult tarjetas = resumen(List.of(valido, "6A00000000000000000000ZZ"));

        assertCodigo(consulta, 400, "DATOS_INVALIDOS");
        assertThat((String) JsonPath.read(cuerpo(consulta), "$.campos.servicio")).isEqualTo("FORMATO");
        assertCodigo(tarjetas, 400, "DATOS_INVALIDOS");
        assertThat((String) JsonPath.read(cuerpo(tarjetas), "$.campos.servicios")).isEqualTo("FORMATO");
        assertThat(catalogo.llamadas("no-es-un-id")).isZero();
        assertThat(catalogo.llamadas(valido)).isZero();
    }

    // ------------------------------------------------------------------ auxiliares

    private MvcResult disponibilidad(String servicio, LocalDate desde, LocalDate hasta) throws Exception {
        return mvc.perform(get("/api/v1/reservas/disponibilidad").param("servicio", servicio)
                .param("desde", desde.toString()).param("hasta", hasta.toString())).andReturn();
    }

    private MvcResult resumen(List<String> servicios) throws Exception {
        return mvc.perform(get("/api/v1/reservas/disponibilidad/resumen")
                .param("servicios", String.join(",", servicios))).andReturn();
    }

    private MvcResult reservar(String tour, LocalDate fecha) throws Exception {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("idServicio", tour);
        cuerpo.put("fechaEntrada", fecha.toString());
        cuerpo.put("horaInicio", "08:00");
        cuerpo.put("personas", 2);
        cuerpo.put("precioTotalEsperado", new BigDecimal("160000"));
        return mvc.perform(post("/api/v1/reservas")
                .header(HttpHeaders.AUTHORIZATION, bearer(turista(UUID.randomUUID())))
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cuerpo))).andReturn();
    }

    private static void assertCodigo(MvcResult resultado, int estado, String codigo) throws Exception {
        assertThat(resultado.getResponse().getStatus()).as(cuerpo(resultado)).isEqualTo(estado);
        assertThat(resultado.getResponse().getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat((String) JsonPath.read(cuerpo(resultado), "$.codigo")).isEqualTo(codigo);
    }

    private static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
