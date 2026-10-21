package co.edu.unimagdalena.samaria360.auth.domain.service;

import org.junit.jupiter.api.Test;

import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;

/** DEC-29: un token vale mientras su {@code ver} no sea menor que la versión conocida de la cuenta. */
class PoliticaSesionTest {

    private final PoliticaSesion politica = new PoliticaSesion();

    @Test
    void sinVersionConocidaSeAceptaCualquierToken() {
        assertThat(politica.vigente(OptionalLong.empty(), 0)).isTrue();
        assertThat(politica.vigente(OptionalLong.empty(), 99)).isTrue();
    }

    @Test
    void conLaMismaVersionOMayorSeAcepta() {
        assertThat(politica.vigente(OptionalLong.of(5), 5)).isTrue();
        assertThat(politica.vigente(OptionalLong.of(5), 6)).isTrue();
    }

    @Test
    void conUnaVersionMenorSeRechaza() {
        assertThat(politica.vigente(OptionalLong.of(5), 4)).isFalse();
    }

    @Test
    void unTokenSinVerCuentaComoCeroYSeRechazaSiLaCuentaYaCambio() {
        assertThat(politica.vigente(OptionalLong.of(0), 0)).isTrue();
        assertThat(politica.vigente(OptionalLong.of(1), 0)).isFalse();
    }
}
