package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.infrastructure.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * «Más filtros» (WF-05, RF10, RF16) contra MongoDB real con la semilla de infra/mongo/init: GET /oferta con los
 * filtros de cada categoría y GET /filtros con sus opciones.
 */
@PruebaIntegracion
class MasFiltrosApiIntegracionTest {

    private static final String OFERTA = "/api/v1/catalogo/oferta";
    private static final String FILTROS = "/api/v1/catalogo/filtros";

    @Autowired
    private MockMvc mvc;

    // ------------------------------------------------------------------ un filtro a la vez

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "categoria=Alojamiento&serviciosIncluidos=Piscina                                | Ecocabañas Cañaveral",
            "categoria=Alojamiento&serviciosIncluidos=WiFi&serviciosIncluidos=desayuno       | Hotel Casa Tayrona;Cabañas El Roble",
            "categoria=Alojamiento&serviciosIncluidos=Parqueadero                            | Cabañas El Roble",
            "categoria=Alojamiento&huespedes=4                                               | Hotel Casa Tayrona;Cabañas El Roble;Hostal Bahía Taganga",
            "categoria=Alojamiento&huespedes=4&precioMax=200000                              | Cabañas El Roble;Hostal Bahía Taganga",
            "categoria=Alojamiento&huespedes=4&precioMax=260000                              | Hotel Casa Tayrona;Cabañas El Roble;Hostal Bahía Taganga",
            "categoria=Alojamiento&tipoHabitacion=suite                                      | Hotel Casa Tayrona",
            "categoria=Alojamiento&tipoHabitacion=Suite&precioMax=200000                     | ",
            "categoria=Alojamiento&huespedes=7                                               | ",
            "categoria=Alojamiento&tipoHabitacion=Cabaña                                     | Cabañas El Roble",
            "categoria=Alojamiento&tipoHabitacion=cabaña                                     | Cabañas El Roble",
            "categoria=Alojamiento&tipoHabitacion=Cabana                                     | ",
            "categoria=Alojamiento&tipoHabitacion=ecohabitación                              | Ecocabañas Cañaveral",
            "categoria=Gastronomia&menuVegetariano=true                                      | Restaurante Ouzo Santa Marta;Mercado Gastronómico La Plaza",
            "categoria=Gastronomia&menuVegetariano=false                                     | Restaurante Ouzo Santa Marta;Mercado Gastronómico La Plaza;Mirador Gourmet Taganga",
            "categoria=Gastronomia&tipoCocina=mediterránea                                   | Restaurante Ouzo Santa Marta",
            "categoria=Gastronomia&tipoCocina=Mediterranea                                   | ",
            "categoria=Gastronomia&tipoCocina=Caribeña                                       | ",
            "categoria=Tour&nivelDificultad=Media                                            | Tour de Café en Minca;Tour de café Samaria Alta",
            "categoria=Tour&nivelDificultad=Baja                                             | ",
            "categoria=Tour&duracionMaxHoras=6                                               | Tour de Café en Minca;Tour de café Samaria Alta",
            "categoria=Tour&duracionMaxHoras=5.5                                             | Tour de Café en Minca",
            "categoria=Tour&nivelDificultad=media&duracionMaxHoras=5                         | Tour de Café en Minca",
            "categoria=AlquilerEquipo&tipoVehiculoEquipo=bicicleta urbana                    | Bicicletas Centro Histórico",
            "categoria=AlquilerEquipo&tipoVehiculoEquipo=Kayak doble                         | ",
    })
    void cadaFiltroDejaSoloLoQueCumple(String parametros, String esperados) throws Exception {
        List<Map<String, Object>> elementos = todos(parametros);

        assertThat(nombres(elementos)).containsExactlyInAnyOrderElementsOf(lista(esperados));
        assertThat(elementos).allMatch(e -> "Servicio".equals(e.get("tipo")));
    }

    // ------------------------------------------------------------------ combinaciones

    @Test
    void seCombinaConTextoPrecioYAccesibilidad() throws Exception {
        assertThat(nombres(todos("categoria=Alojamiento&serviciosIncluidos=WiFi&precioMax=160000")))
                .containsExactlyInAnyOrder("Hotel Casa Tayrona", "Hostal Bahía Taganga");
        assertThat(nombres(todos("categoria=Alojamiento&serviciosIncluidos=WiFi&precioMax=160000&accesibilidad=Visual")))
                .containsExactly("Hostal Bahía Taganga");
        assertThat(nombres(todos("categoria=Alojamiento&huespedes=2&texto=cabañas")))
                .containsExactlyInAnyOrder("Cabañas El Roble", "Ecocabañas Cañaveral");
    }

    @Test
    void seCombinaConUbicacionDistanciaYOrden() throws Exception {
        List<Map<String, Object>> porPrecio = todos(
                "categoria=Gastronomia&menuVegetariano=true&orden=PrecioAscendente");
        List<Map<String, Object>> cerca = todos(
                "categoria=Gastronomia&menuVegetariano=true&lat=11.2420&lng=-74.2117&distanciaKm=50");

        assertThat(nombres(porPrecio)).containsExactly("Mercado Gastronómico La Plaza", "Restaurante Ouzo Santa Marta");
        assertThat(nombres(cerca)).containsExactlyInAnyOrder(
                "Mercado Gastronómico La Plaza", "Restaurante Ouzo Santa Marta");
        assertThat(cerca).allMatch(e -> e.get("distanciaKm") != null);
        List<Double> distancias = cerca.stream().map(e -> ((Number) e.get("distanciaKm")).doubleValue()).toList();
        assertThat(distancias).isSorted();
    }

    @Test
    void rf16_conMasFiltrosLaPaginaSigueSiendoDe10OMenos() throws Exception {
        MvcResult pagina = buscar("categoria=Alojamiento&huespedes=1");
        List<?> elementos = JsonPath.read(cuerpo(pagina), "$.elementos");

        assertThat(elementos).hasSizeLessThanOrEqualTo(10);
        assertThat((Integer) JsonPath.read(cuerpo(pagina), "$.tamano")).isEqualTo(10);
        assertThat(buscar("categoria=Alojamiento&huespedes=1&tamano=11").getResponse().getStatus()).isEqualTo(400);
    }

    // ------------------------------------------------------------------ errores

    @Test
    void unServicioIncluidoDesconocidoResponde400ConLosAceptados() throws Exception {
        MvcResult resultado = buscar("categoria=Alojamiento&serviciosIncluidos=Jacuzzi");

        assertCriteriosInvalidos(resultado, "serviciosIncluidos no admite «Jacuzzi»; valores aceptados: "
                + "WiFi, Desayuno, Piscina, Parqueadero");
    }

    @Test
    void unValorLargoNoSeRepiteCompletoEnElMensaje() throws Exception {
        MvcResult resultado = buscar("categoria=Alojamiento&serviciosIncluidos=" + "J".repeat(500));
        String detalle = JsonPath.read(cuerpo(resultado), "$.detail");

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(detalle).contains("«" + "J".repeat(40) + "…»").doesNotContain("J".repeat(41));
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
            "tipoCocina=Típica                          | tipoCocina requiere categoria=Gastronomia",
            "categoria=Tour&tipoCocina=Típica           | tipoCocina requiere categoria=Gastronomia",
            "serviciosIncluidos=WiFi                    | serviciosIncluidos requiere categoria=Alojamiento",
            "categoria=Gastronomia&huespedes=2          | huespedes requiere categoria=Alojamiento",
            "menuVegetariano=true                       | menuVegetariano requiere categoria=Gastronomia",
            "categoria=Alojamiento&nivelDificultad=Alta | nivelDificultad requiere categoria=Tour",
            "duracionMaxHoras=4                         | duracionMaxHoras requiere categoria=Tour",
            "categoria=Tour&tipoVehiculoEquipo=Kayak    | tipoVehiculoEquipo requiere categoria=AlquilerEquipo",
    })
    void unFiltroSinSuCategoriaResponde400ConLaQueExige(String parametros, String mensaje) throws Exception {
        assertCriteriosInvalidos(buscar(parametros), mensaje);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "categoria=Alojamiento&huespedes=0",
            "categoria=Alojamiento&huespedes=abc",
            "categoria=Tour&duracionMaxHoras=0",
            "categoria=Tour&duracionMaxHoras=-2",
            "categoria=Gastronomia&menuVegetariano=quizas",
    })
    void valoresInvalidosResponden400(String parametros) throws Exception {
        MvcResult resultado = buscar(parametros);

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat((String) JsonPath.read(cuerpo(resultado), "$.codigo")).isEqualTo("CRITERIOS_INVALIDOS");
    }

    @Test
    void menuVegetarianoFalsoSinCategoriaNoEsUnFiltro() throws Exception {
        assertThat(todos("menuVegetariano=false")).hasSize(18);
    }

    // ------------------------------------------------------------------ opciones

    @Test
    void opcionesDeAlojamiento() throws Exception {
        String json = opciones("Alojamiento");

        assertThat((List<String>) JsonPath.read(json, "$.serviciosIncluidos"))
                .containsExactly("WiFi", "Desayuno", "Piscina", "Parqueadero");
        assertThat((List<String>) JsonPath.read(json, "$.tiposHabitacion"))
                .containsExactly("Cabaña", "Compartida", "Doble", "Ecohabitación", "Suite");
        assertThat((Integer) JsonPath.read(json, "$.huespedes.minimo")).isEqualTo(2);
        assertThat((Integer) JsonPath.read(json, "$.huespedes.maximo")).isEqualTo(6);
        assertThat(claves(json)).containsExactlyInAnyOrder("categoria", "serviciosIncluidos", "tiposHabitacion",
                "huespedes");
    }

    @Test
    void opcionesDeGastronomiaSinLosServiciosPendientes() throws Exception {
        String json = opciones("Gastronomia");

        assertThat((Boolean) JsonPath.read(json, "$.opcionVegetariana")).isTrue();
        // «Caribeña» es de «Cocina Caribe El Rodadero», pendiente de revisión: no se ofrece.
        assertThat((List<String>) JsonPath.read(json, "$.tiposCocina"))
                .containsExactly("Colombiana", "Mariscos", "Mediterránea");
        assertThat(claves(json)).containsExactlyInAnyOrder("categoria", "opcionVegetariana", "tiposCocina");
    }

    @Test
    void opcionesDeTourSinLosDeUnPrestadorInactivo() throws Exception {
        String json = opciones("Tour");

        // «Baja» y 3,5 h son de «Snorkel en Bahía Concha», de un prestador inactivo.
        assertThat((List<String>) JsonPath.read(json, "$.nivelesDificultad")).containsExactly("Alta", "Media");
        assertThat(decimal(JsonPath.read(json, "$.duracionHoras.minimo"))).isEqualByComparingTo("4");
        assertThat(decimal(JsonPath.read(json, "$.duracionHoras.maximo"))).isEqualByComparingTo("8");
        assertThat(claves(json)).containsExactlyInAnyOrder("categoria", "nivelesDificultad", "duracionHoras");
    }

    @Test
    void opcionesDeAlquilerDeEquipos() throws Exception {
        String json = opciones("AlquilerEquipo");

        // Los kayaks están pendientes de revisión.
        assertThat((List<String>) JsonPath.read(json, "$.tiposVehiculoEquipo")).containsExactly("Bicicleta urbana");
        assertThat(claves(json)).containsExactlyInAnyOrder("categoria", "tiposVehiculoEquipo");
    }

    /** MockMvc codifica la consulta (tildes y espacios), igual que el navegador. */
    @Test
    void cadaOpcionOfrecidaDevuelveResultados() throws Exception {
        List<String> consultas = new ArrayList<>();
        String alojamiento = opciones("Alojamiento");
        for (String servicio : JsonPath.<List<String>>read(alojamiento, "$.serviciosIncluidos")) {
            consultas.add("categoria=Alojamiento&serviciosIncluidos=" + servicio);
        }
        for (String tipo : JsonPath.<List<String>>read(alojamiento, "$.tiposHabitacion")) {
            consultas.add("categoria=Alojamiento&tipoHabitacion=" + tipo);
        }
        consultas.add("categoria=Alojamiento&huespedes=" + JsonPath.read(alojamiento, "$.huespedes.maximo"));
        for (String tipo : JsonPath.<List<String>>read(opciones("Gastronomia"), "$.tiposCocina")) {
            consultas.add("categoria=Gastronomia&tipoCocina=" + tipo);
        }
        String tour = opciones("Tour");
        for (String nivel : JsonPath.<List<String>>read(tour, "$.nivelesDificultad")) {
            consultas.add("categoria=Tour&nivelDificultad=" + nivel);
        }
        consultas.add("categoria=Tour&duracionMaxHoras=" + JsonPath.read(tour, "$.duracionHoras.minimo"));
        for (String tipo : JsonPath.<List<String>>read(opciones("AlquilerEquipo"), "$.tiposVehiculoEquipo")) {
            consultas.add("categoria=AlquilerEquipo&tipoVehiculoEquipo=" + tipo);
        }

        for (String consulta : consultas) {
            assertThat(todos(consulta)).as(consulta).isNotEmpty();
        }
    }

    /** D1: con filtros de unidad, el precio «desde» es el de las unidades que cumplen, y el orden lo usa. */
    @Test
    void conTresHuespedesCasaTayronaMuestra260000YCambiaDePuestoAlOrdenarPorPrecio() throws Exception {
        List<Map<String, Object>> sinFiltro = todos("categoria=Alojamiento&orden=PrecioAscendente");
        List<Map<String, Object>> tresHuespedes = todos("categoria=Alojamiento&huespedes=3&orden=PrecioAscendente");

        assertThat(precioDe(sinFiltro, "Hotel Casa Tayrona")).isEqualByComparingTo("150000");
        assertThat(precioDe(tresHuespedes, "Hotel Casa Tayrona")).isEqualByComparingTo("260000");
        assertThat(nombres(sinFiltro)).containsExactly("Hostal Bahía Taganga", "Hotel Casa Tayrona",
                "Cabañas El Roble", "Ecocabañas Cañaveral");
        assertThat(nombres(tresHuespedes)).containsExactly("Hostal Bahía Taganga", "Cabañas El Roble",
                "Hotel Casa Tayrona", "Ecocabañas Cañaveral");
    }

    @Test
    void sinFiltrosDeUnidadLosAtractivosSiguenSinPrecioYLosServiciosConSuPrecioBase() throws Exception {
        List<Map<String, Object>> elementos = todos("orden=PrecioAscendente");

        assertThat(elementos).filteredOn(e -> "Atractivo".equals(e.get("tipo"))).isNotEmpty()
                .allSatisfy(e -> assertThat(e.get("precio")).isNull());
        assertThat(precioDe(elementos, "Hotel Casa Tayrona")).isEqualByComparingTo("150000");
    }

    @Test
    void lasOpcionesExigenUnaCategoriaValida() throws Exception {
        assertCriteriosInvalidos(mvc.perform(get(FILTROS)).andReturn(), "Las opciones de filtro exigen categoria");

        MvcResult inexistente = mvc.perform(get(FILTROS + "?categoria=Inexistente")).andReturn();
        assertThat(inexistente.getResponse().getStatus()).isEqualTo(400);
        assertThat((String) JsonPath.read(cuerpo(inexistente), "$.codigo")).isEqualTo("CRITERIOS_INVALIDOS");
    }

    @Test
    void lasOpcionesSonPublicas() throws Exception {
        MvcResult resultado = mvc.perform(get(FILTROS + "?categoria=Tour")).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(200);
        assertThat(resultado.getResponse().getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
    }

    // ------------------------------------------------------------------ auxiliares

    private void assertCriteriosInvalidos(MvcResult resultado, String mensaje) throws Exception {
        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(resultado.getResponse().getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat((String) JsonPath.read(cuerpo(resultado), "$.codigo")).isEqualTo("CRITERIOS_INVALIDOS");
        assertThat((String) JsonPath.read(cuerpo(resultado), "$.detail")).isEqualTo(mensaje);
    }

    private String opciones(String categoria) throws Exception {
        MvcResult resultado = mvc.perform(get(FILTROS + "?categoria=" + categoria)).andReturn();
        assertThat(resultado.getResponse().getStatus()).as(cuerpo(resultado)).isEqualTo(200);
        return cuerpo(resultado);
    }

    private MvcResult buscar(String parametros) throws Exception {
        return mvc.perform(get(OFERTA + "?" + parametros)).andReturn();
    }

    /** Recorre todas las páginas de 10 y devuelve los elementos en orden. */
    private List<Map<String, Object>> todos(String parametros) throws Exception {
        List<Map<String, Object>> elementos = new ArrayList<>();
        for (int pagina = 0; ; pagina++) {
            MvcResult resultado = buscar(parametros + "&pagina=" + pagina);
            assertThat(resultado.getResponse().getStatus()).as(cuerpo(resultado)).isEqualTo(200);
            elementos.addAll(JsonPath.<List<Map<String, Object>>>read(cuerpo(resultado), "$.elementos"));
            int totalPaginas = JsonPath.read(cuerpo(resultado), "$.totalPaginas");
            if (pagina + 1 >= totalPaginas) {
                return elementos;
            }
        }
    }

    private static BigDecimal precioDe(List<Map<String, Object>> elementos, String nombre) {
        return elementos.stream().filter(e -> nombre.equals(e.get("nombre"))).findFirst()
                .map(e -> decimal(e.get("precio"))).orElseThrow();
    }

    private static List<String> nombres(List<Map<String, Object>> elementos) {
        return elementos.stream().map(e -> (String) e.get("nombre")).toList();
    }

    private static List<String> lista(String valores) {
        return valores == null || valores.isBlank() ? List.of() : List.of(valores.trim().split(";"));
    }

    private static List<String> claves(String json) {
        return new ArrayList<>(JsonPath.<Map<String, Object>>read(json, "$").keySet());
    }

    private static BigDecimal decimal(Object valor) {
        return new BigDecimal(valor.toString());
    }

    private static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
