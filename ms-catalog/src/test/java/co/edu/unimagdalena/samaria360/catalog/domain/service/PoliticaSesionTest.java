package co.edu.unimagdalena.samaria360.catalog.domain.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DEC-29: un token vale mientras su {@code ver} no sea menor que la versión conocida de la cuenta. DEC-110: sin fila, se
 * acepta un Turista o un Prestador, pero no un Gestor ni un Administrador (cierre de L3).
 */
class PoliticaSesionTest {

    private final PoliticaSesion politica = new PoliticaSesion();

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"Turista", "Prestador", "Desconocido"})
    void sinFilaEnLaProyeccionSeAceptaElTokenDeUnRolNoPrivilegiado(String rol) {
        assertThat(politica.vigente(OptionalLong.empty(), 0, rol)).isTrue();
        assertThat(politica.vigente(OptionalLong.empty(), 7, rol)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Gestor", "Administrador"})
    void sinFilaEnLaProyeccionSeRechazaElTokenDeUnRolPrivilegiado(String rol) {
        assertThat(politica.vigente(OptionalLong.empty(), 0, rol)).isFalse();
        assertThat(politica.vigente(OptionalLong.empty(), 7, rol)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Turista", "Administrador"})
    void conLaMismaVersionOMayorSeAcepta(String rol) {
        assertThat(politica.vigente(OptionalLong.of(3), 3, rol)).isTrue();
        assertThat(politica.vigente(OptionalLong.of(3), 4, rol)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Turista", "Gestor"})
    void conUnaVersionMenorSeRechazaYUnTokenSinVerCuentaComoCero(String rol) {
        assertThat(politica.vigente(OptionalLong.of(3), 2, rol)).isFalse();
        assertThat(politica.vigente(OptionalLong.of(1), 0, rol)).isFalse();
    }
}
