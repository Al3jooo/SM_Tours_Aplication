package co.edu.unimagdalena.samaria360.booking.domain.service;

import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;

/** DEC-29: un token vale mientras su {@code ver} no sea menor que la versión conocida de la cuenta. */
class PoliticaSesionTest {

    private final PoliticaSesion politica = new PoliticaSesion();

    @Test
    void sinFilaEnLaProyeccionSeAceptaCualquierToken() {
        assertThat(politica.vigente(OptionalLong.empty(), 0)).isTrue();
        assertThat(politica.vigente(OptionalLong.empty(), 7)).isTrue();
    }

    @Test
    void conLaMismaVersionOMayorSeAcepta() {
        assertThat(politica.vigente(OptionalLong.of(3), 3)).isTrue();
        assertThat(politica.vigente(OptionalLong.of(3), 4)).isTrue();
    }

    @Test
    void conUnaVersionMenorSeRechazaYUnTokenSinVerCuentaComoCero() {
        assertThat(politica.vigente(OptionalLong.of(3), 2)).isFalse();
        assertThat(politica.vigente(OptionalLong.of(1), 0)).isFalse();
    }
}
