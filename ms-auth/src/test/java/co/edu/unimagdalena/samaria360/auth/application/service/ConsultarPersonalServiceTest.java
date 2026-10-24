package co.edu.unimagdalena.samaria360.auth.application.service;

import co.edu.unimagdalena.samaria360.auth.application.fake.CambioRolRepositoryEnMemoria;
import co.edu.unimagdalena.samaria360.auth.application.port.in.FiltroPersonal;
import co.edu.unimagdalena.samaria360.auth.domain.exception.DatosRegistroInvalidosException;
import co.edu.unimagdalena.samaria360.auth.domain.model.MotivoInvalido;
import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Filtros del listado del personal (RF45, DEC-103), con las mismas reglas que los demás listados. */
class ConsultarPersonalServiceTest {

    private final CambioRolRepositoryEnMemoria cuentas = new CambioRolRepositoryEnMemoria();
    private final ConsultarPersonalService servicio = new ConsultarPersonalService(cuentas);

    @Test
    void sinFiltrosPideLosDosRolesYRecortaElTexto() {
        servicio.personal(null, "  ana  ", 2);

        assertThat(cuentas.ultimoFiltro()).isEqualTo(new FiltroPersonal(null, "ana", 2));
        assertThat(cuentas.ultimoFiltro().desplazamiento()).isEqualTo(20);

        servicio.personal("Administrador", " ", 0);
        assertThat(cuentas.ultimoFiltro()).isEqualTo(new FiltroPersonal(Rol.Administrador, null, 0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Turista", "Prestador", "administrador"})
    void unRolQueNoEsDelPersonalEs400(String rol) {
        assertThatThrownBy(() -> servicio.personal(rol, null, 0))
                .isInstanceOfSatisfying(DatosRegistroInvalidosException.class,
                        e -> assertThat(e.getCampos()).containsEntry("rol", MotivoInvalido.NO_PERMITIDO));
    }

    @Test
    void unTextoLargoYUnaPaginaNegativaSon400() {
        assertThatThrownBy(() -> servicio.personal(null, "a".repeat(101), -1))
                .isInstanceOfSatisfying(DatosRegistroInvalidosException.class, e -> assertThat(e.getCampos())
                        .containsEntry("q", MotivoInvalido.LONGITUD_MAXIMA)
                        .containsEntry("pagina", MotivoInvalido.FORMATO));
        assertThat(cuentas.ultimoFiltro()).isNull();
    }
}
