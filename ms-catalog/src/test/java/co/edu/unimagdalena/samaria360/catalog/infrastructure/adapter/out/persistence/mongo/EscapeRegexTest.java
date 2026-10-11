package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.out.persistence.mongo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/** El texto del usuario se usa como literal dentro de $regex: ningún metacarácter conserva su significado. */
class EscapeRegexTest {

    @ParameterizedTest
    @ValueSource(strings = {".*", "(", "a+b", "[café]", "^inicio$", "x|y", "\\d{3}", "¿qué?"})
    void elTextoEscapadoSoloCoincideConsigoMismo(String texto) {
        Pattern patron = Pattern.compile(OfertaMongoAdapter.escaparRegex(texto), Pattern.CASE_INSENSITIVE);

        assertThat(patron.matcher("antes " + texto + " después").find()).isTrue();
        assertThat(patron.matcher("cualquier otra cosa").find()).isFalse();
    }

    @Test
    void unTextoSinMetacaracteresNoCambia() {
        assertThat(OfertaMongoAdapter.escaparRegex("Tour de Café")).isEqualTo("Tour de Café");
    }
}
