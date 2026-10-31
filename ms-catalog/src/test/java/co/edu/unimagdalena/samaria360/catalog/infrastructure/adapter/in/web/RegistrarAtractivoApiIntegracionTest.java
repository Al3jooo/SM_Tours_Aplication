package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.PruebaIntegracion;
import com.jayway.jsonpath.JsonPath;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.FIRMA_JPEG;
import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.FIRMA_PNG;
import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.con;
import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.datos;
import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.imagen;
import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.parteImagen;
import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.registro;
import static co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.SolicitudesAtractivo.tokenGestor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * CU «Registrar atractivo» de punta a punta contra MongoDB y GridFS (RF06, RF07, RF24, RF27, RF33, RF58; DEC-209,
 * DEC-216): el multipart, las imágenes servidas, el límite de 5 MB por imagen y la validación por campo. Cada prueba usa
 * su propio Gestor y borra lo que agregó.
 */
@PruebaIntegracion
class RegistrarAtractivoApiIntegracionTest {

    private static final Instant AHORA = Instant.parse("2026-10-30T15:00:00.321Z");
    private static final int CINCO_MB = 5 * 1024 * 1024;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MongoTemplate mongo;

    @Autowired
    private RelojFijo reloj;

    private final UUID gestor = UUID.randomUUID();
    private final String nombre = "Atractivo de registro " + UUID.randomUUID();

    @BeforeEach
    void preparar() {
        reloj.ajustar(AHORA);
        SolicitudesAtractivo.filaDeCuenta(base(), gestor, "Gestor", 1);
    }

    @AfterEach
    void limpiar() {
        SolicitudesAtractivo.limpiar(base(), gestor);
    }

    @Test
    void registraElAtractivoPublicadoConSusImagenesEnGridFs() throws Exception {
        byte[] jpeg = imagen(FIRMA_JPEG, 2048);
        byte[] png = imagen(FIRMA_PNG, 1024);

        MvcResult resultado = mvc.perform(registro(token(), datos(nombre), parteImagen("cascada.jpg", jpeg),
                parteImagen("sendero.png", png))).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        String json = cuerpo(resultado);
        String id = JsonPath.read(json, "$.id");
        assertThat(resultado.getResponse().getHeader(HttpHeaders.LOCATION))
                .isEqualTo(SolicitudesAtractivo.RUTA + "/" + id);
        assertThat(JsonPath.<String>read(json, "$.gestorId")).isEqualTo(gestor.toString());
        assertThat(JsonPath.<Integer>read(json, "$.version")).isZero();
        assertThat(JsonPath.<String>read(json, "$.fechaRegistro")).isEqualTo("2026-10-30T15:00:00.321Z");
        assertThat(JsonPath.<List<String>>read(json, "$.condicionesAccesibilidad"))
                .as("en el orden de RF27").containsExactly("MovilidadReducida", "Visual");

        Document guardado = base().getCollection("atractivos").find(Filters.eq("_id", new ObjectId(id))).first();
        assertThat(guardado.getString("estadoOperativo")).isEqualTo("Abierto");
        assertThat(guardado.getString("horarioAtencion")).isEqualTo("Martes a domingo, 8:00 a 16:00");
        assertThat(guardado.get("limiteCapacidadCarga")).isEqualTo(80);
        assertThat(guardado.get("version")).isEqualTo(0);
        assertThat(guardado.getDate("fechaRegistro")).isEqualTo(Date.from(AHORA));

        List<String> rutas = guardado.getList("imagenes", String.class);
        assertThat(rutas).hasSize(2).allMatch(ruta -> ruta.startsWith("/api/v1/catalogo/imagenes/"));
        MvcResult primera = mvc.perform(get(rutas.get(0))).andReturn();
        assertThat(primera.getResponse().getStatus()).isEqualTo(200);
        assertThat(primera.getResponse().getContentType()).isEqualTo("image/jpeg");
        assertThat(primera.getResponse().getContentAsByteArray()).isEqualTo(jpeg);
        assertThat(mvc.perform(get(rutas.get(1))).andReturn().getResponse().getContentType()).isEqualTo("image/png");
    }

    @Test
    void elAtractivoNuevoSeVeEnElDetallePublicoYEnLaBusqueda() throws Exception {
        String id = JsonPath.read(cuerpo(mvc.perform(registro(token(), datos(nombre),
                parteImagen("a.jpg", imagen(FIRMA_JPEG, 100)))).andReturn()), "$.id");

        String detalle = cuerpo(mvc.perform(get("/api/v1/catalogo/atractivos/" + id)).andReturn());
        String busqueda = cuerpo(mvc.perform(get("/api/v1/catalogo/oferta").param("texto", nombre)).andReturn());

        assertThat(JsonPath.<String>read(detalle, "$.nombre")).isEqualTo(nombre);
        assertThat(JsonPath.<List<String>>read(busqueda, "$.elementos[*].id")).containsExactly(id);
    }

    /** C3 (parte de ms-catalog): una imagen de 5 MB exactos se acepta y se sirve íntegra. */
    @Test
    void unaImagenDeCincoMegabytesExactosSeAceptaYSeSirveIntegra() throws Exception {
        byte[] grande = imagen(FIRMA_JPEG, CINCO_MB);
        grande[CINCO_MB - 1] = 0x7F;

        MvcResult resultado = mvc.perform(registro(token(), datos(nombre), parteImagen("grande.jpg", grande)))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(201);
        String ruta = JsonPath.read(cuerpo(resultado), "$.imagenes[0]");
        assertThat(mvc.perform(get(ruta)).andReturn().getResponse().getContentAsByteArray()).isEqualTo(grande);
    }

    @Test
    void cadaImagenInvalidaTieneSuMotivoYNoSeGuardaNada() throws Exception {
        long archivosAntes = archivos();

        MvcResult resultado = mvc.perform(registro(token(), datos(nombre),
                parteImagen("ok.jpg", imagen(FIRMA_JPEG, 100)),
                parteImagen("falsa.jpg", "no soy una imagen".getBytes(StandardCharsets.UTF_8)),
                parteImagen("grande.jpg", imagen(FIRMA_JPEG, CINCO_MB + 1)))).andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<Map<String, String>>read(cuerpo(resultado), "$.campos")).containsOnly(
                Map.entry("imagenes[1]", "FORMATO"), Map.entry("imagenes[2]", "TAMANO_MAXIMO"));
        assertThat(archivos()).isEqualTo(archivosAntes);
        assertThat(base().getCollection("atractivos").countDocuments(Filters.eq("nombre", nombre))).isZero();
    }

    @Test
    void sinImagenesOConMasDeCincoEsUn400DelCampoImagenes() throws Exception {
        MockMultipartFile[] seis = new MockMultipartFile[6];
        for (int i = 0; i < 6; i++) {
            seis[i] = parteImagen(i + ".jpg", imagen(FIRMA_JPEG, 50));
        }

        String sin = cuerpo(mvc.perform(registro(token(), datos(nombre))).andReturn());
        String demasiadas = cuerpo(mvc.perform(registro(token(), datos(nombre), seis)).andReturn());

        assertThat(JsonPath.<Map<String, String>>read(sin, "$.campos")).containsOnly(Map.entry("imagenes", "REQUERIDO"));
        assertThat(JsonPath.<Map<String, String>>read(demasiadas, "$.campos"))
                .containsOnly(Map.entry("imagenes", "CANTIDAD_MAXIMA"));
    }

    @Test
    void losDatosInvalidosSonDatosInvalidosConSusNombresJson() throws Exception {
        Map<String, Object> malos = con(datos(" "), d -> {
            d.put("categoria", "Aventura");
            d.put("coordenadas", Map.of("longitud", -200, "latitud", 11.2));
            d.put("limiteCapacidadCarga", 0);
            d.put("estadoOperativo", "Cerrado");
            d.put("normasAmbientales", "n".repeat(1001));
            d.put("condicionesAccesibilidad", List.of("Braille"));
        });

        MvcResult resultado = mvc.perform(registro(token(), malos, parteImagen("a.jpg", imagen(FIRMA_JPEG, 10))))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<String>read(cuerpo(resultado), "$.codigo")).isEqualTo("DATOS_INVALIDOS");
        assertThat(JsonPath.<Map<String, String>>read(cuerpo(resultado), "$.campos")).containsOnly(
                Map.entry("nombre", "REQUERIDO"), Map.entry("categoria", "NO_PERMITIDO"),
                Map.entry("coordenadas.longitud", "VALOR_MINIMO"), Map.entry("limiteCapacidadCarga", "VALOR_MINIMO"),
                Map.entry("estadoOperativo", "NO_PERMITIDO"), Map.entry("normasAmbientales", "LONGITUD_MAXIMA"),
                Map.entry("condicionesAccesibilidad[0]", "NO_PERMITIDO"));
    }

    @Test
    void sinLaParteDatosCadaObligatorioEsRequerido() throws Exception {
        MvcResult resultado = mvc.perform(registro(token(), null, parteImagen("a.jpg", imagen(FIRMA_JPEG, 10))))
                .andReturn();

        assertThat(resultado.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<Map<String, String>>read(cuerpo(resultado), "$.campos")).containsKeys("nombre",
                "categoria", "descripcion", "coordenadas", "limiteCapacidadCarga", "estadoOperativo");
    }

    @Test
    void unaParteDatosQueNoEsJsonEsDatosInvalidosYUnCuerpoJsonNoEsMultipart() throws Exception {
        MockMultipartFile roto = new MockMultipartFile("datos", "", MediaType.APPLICATION_JSON_VALUE,
                "{nombre:".getBytes(StandardCharsets.UTF_8));

        MvcResult malFormado = mvc.perform(multipart(SolicitudesAtractivo.RUTA).file(roto)
                .header(HttpHeaders.AUTHORIZATION, token())).andReturn();
        MvcResult json = mvc.perform(post(SolicitudesAtractivo.RUTA).header(HttpHeaders.AUTHORIZATION, token())
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andReturn();

        assertThat(malFormado.getResponse().getStatus()).isEqualTo(400);
        assertThat(JsonPath.<String>read(cuerpo(malFormado), "$.codigo")).isEqualTo("DATOS_INVALIDOS");
        assertThat(json.getResponse().getStatus()).isEqualTo(415);
    }

    @Test
    void loQueFijaElServidorSeIgnora() throws Exception {
        Map<String, Object> conExtras = con(datos(nombre), d -> {
            d.put("gestorId", UUID.randomUUID().toString());
            d.put("version", 9);
            d.put("imagenes", List.of("/otra/ruta.jpg"));
        });

        String json = cuerpo(mvc.perform(registro(token(), conExtras, parteImagen("a.jpg", imagen(FIRMA_JPEG, 10))))
                .andReturn());

        assertThat(JsonPath.<String>read(json, "$.gestorId")).isEqualTo(gestor.toString());
        assertThat(JsonPath.<Integer>read(json, "$.version")).isZero();
        assertThat(JsonPath.<List<String>>read(json, "$.imagenes")).singleElement()
                .satisfies(ruta -> assertThat(ruta).startsWith("/api/v1/catalogo/imagenes/"));
    }

    // ---- auxiliares ------------------------------------------------------------------------------------------

    private String token() {
        return tokenGestor(gestor, 1);
    }

    private long archivos() {
        return base().getCollection("imagenes.files").countDocuments();
    }

    private MongoDatabase base() {
        return mongo.getDb();
    }

    private static String cuerpo(MvcResult resultado) throws Exception {
        return resultado.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }
}
