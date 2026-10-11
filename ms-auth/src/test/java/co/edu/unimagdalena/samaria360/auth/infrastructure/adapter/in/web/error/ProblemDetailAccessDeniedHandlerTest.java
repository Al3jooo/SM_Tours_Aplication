package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.error;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/** Todavía no hay endpoints restringidos por rol: se prueba el handler directamente. */
class ProblemDetailAccessDeniedHandlerTest {

    private final ProblemDetailAccessDeniedHandler handler =
            new ProblemDetailAccessDeniedHandler(Jackson2ObjectMapperBuilder.json().build());

    @Test
    void responde403ConProblemDetailYCodigoAccesoDenegado() throws Exception {
        MockHttpServletRequest solicitud = new MockHttpServletRequest("GET", "/api/v1/auth/solo-admin");
        MockHttpServletResponse respuesta = new MockHttpServletResponse();

        handler.handle(solicitud, respuesta, new AccessDeniedException("rol insuficiente"));

        assertThat(respuesta.getStatus()).isEqualTo(403);
        assertThat(respuesta.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String cuerpo = respuesta.getContentAsString(StandardCharsets.UTF_8);
        assertThat(JsonPath.<String>read(cuerpo, "$.codigo")).isEqualTo("ACCESO_DENEGADO");
        assertThat(JsonPath.<Integer>read(cuerpo, "$.status")).isEqualTo(403);
        assertThat(JsonPath.<String>read(cuerpo, "$.instance")).isEqualTo("/api/v1/auth/solo-admin");
    }
}
