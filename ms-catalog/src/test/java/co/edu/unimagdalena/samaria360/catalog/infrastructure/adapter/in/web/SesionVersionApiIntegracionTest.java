package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.catalog.infrastructure.TokensDePrueba;
import com.jayway.jsonpath.JsonPath;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * DEC-29: ms-catalog compara el claim {@code ver} con la proyección {@code cuentas}. La búsqueda es pública, pero Spring
 * valida cualquier Bearer que llegue: un token de antes de un cambio de estado recibe 401 SESION_INVALIDADA.
 */
@PruebaIntegracion
class SesionVersionApiIntegracionTest {

    private static final String OFERTA = "/api/v1/catalogo/oferta";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private MongoTemplate mongo;

    private final UUID usuario = UUID.randomUUID();

    @AfterEach
    void limpiar() {
        mongo.getCollection("cuentas").deleteOne(new Document("_id", usuario.toString()));
    }

    private void proyeccion(long version) {
        proyeccion("Turista", "Desactivado", version);
    }

    private void proyeccion(String rol, String estado, long version) {
        mongo.getCollection("cuentas").insertOne(new Document("_id", usuario.toString()).append("rol", rol)
                .append("estadoCuenta", estado).append("version", version).append("actualizadoEn", new Date()));
    }

    private MvcResult oferta(String token) throws Exception {
        var solicitud = get(OFERTA);
        if (token != null) {
            solicitud.header(HttpHeaders.AUTHORIZATION, TokensDePrueba.bearer(token));
        }
        return mvc.perform(solicitud).andReturn();
    }

    @Test
    void sinTokenLaOfertaResponde200() throws Exception {
        assertThat(oferta(null).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void sinFilaEnLaProyeccionSeAceptaUnTokenConOSinVer() throws Exception {
        assertThat(oferta(TokensDePrueba.usuario(usuario, "Turista", 7)).getResponse().getStatus()).isEqualTo(200);
        assertThat(oferta(TokensDePrueba.usuarioSinVer(usuario, "Turista")).getResponse().getStatus()).isEqualTo(200);
        assertThat(oferta(TokensDePrueba.usuario(usuario, "Prestador", 0)).getResponse().getStatus()).isEqualTo(200);
    }

    /** DEC-110, cierre de L3: un Gestor o un Administrador sin fila en la proyección no tiene sesión. */
    @ParameterizedTest
    @ValueSource(strings = {"Gestor", "Administrador"})
    void sinFilaEnLaProyeccionUnRolPrivilegiadoResponde401SesionInvalidada(String rol) throws Exception {
        for (String token : List.of(TokensDePrueba.usuario(usuario, rol, 5), TokensDePrueba.usuarioSinVer(usuario, rol))) {
            MvcResult resultado = oferta(token);
            assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
            assertThat(JsonPath.<String>read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8),
                    "$.codigo")).isEqualTo("SESION_INVALIDADA");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"Gestor", "Administrador"})
    void conFilaUnRolPrivilegiadoConVerVigenteSeAceptaYConVerViejoResponde401(String rol) throws Exception {
        proyeccion(rol, "Activo", 2);

        assertThat(oferta(TokensDePrueba.usuario(usuario, rol, 2)).getResponse().getStatus()).isEqualTo(200);
        MvcResult viejo = oferta(TokensDePrueba.usuario(usuario, rol, 1));
        assertThat(viejo.getResponse().getStatus()).isEqualTo(401);
        assertThat(JsonPath.<String>read(viejo.getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$.codigo")).isEqualTo("SESION_INVALIDADA");
    }

    @Test
    void conUnVerMenorQueLaVersionDeLaProyeccionResponde401SesionInvalidada() throws Exception {
        proyeccion(3);

        MvcResult resultado = oferta(TokensDePrueba.usuario(usuario, "Turista", 2));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(JsonPath.<String>read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$.codigo")).isEqualTo("SESION_INVALIDADA");
    }

    @Test
    void conUnVerIgualOMayorSeAcepta() throws Exception {
        proyeccion(3);

        assertThat(oferta(TokensDePrueba.usuario(usuario, "Turista", 3)).getResponse().getStatus()).isEqualTo(200);
        assertThat(oferta(TokensDePrueba.usuario(usuario, "Turista", 4)).getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    void unTokenSinVerCuentaComoCeroYSeRechazaSiLaCuentaYaCambio() throws Exception {
        proyeccion(1);

        MvcResult resultado = oferta(TokensDePrueba.usuarioSinVer(usuario, "Turista"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(JsonPath.<String>read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$.codigo")).isEqualTo("SESION_INVALIDADA");
    }

    @Test
    void unTokenDeServicioNoSirveEnLaApiPublica() throws Exception {
        MvcResult resultado = oferta(TokensDePrueba.servicio("ms-catalog"));

        assertThat(resultado.getResponse().getStatus()).isEqualTo(401);
        assertThat(JsonPath.<String>read(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8),
                "$.codigo")).isEqualTo("NO_AUTENTICADO");
    }
}
