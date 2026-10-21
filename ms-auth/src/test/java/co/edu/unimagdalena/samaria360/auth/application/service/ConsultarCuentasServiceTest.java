package co.edu.unimagdalena.samaria360.auth.application.service;

import co.edu.unimagdalena.samaria360.auth.application.fake.CuentasAdminEnMemoria;
import co.edu.unimagdalena.samaria360.auth.application.port.in.FiltroCuentas;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaNoEncontradaException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.DatosRegistroInvalidosException;
import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoAdministrativo;
import co.edu.unimagdalena.samaria360.auth.domain.model.MotivoInvalido;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultarCuentasServiceTest {

    private final CuentasAdminEnMemoria cuentas = new CuentasAdminEnMemoria();
    private final ConsultarCuentasService servicio = new ConsultarCuentasService(cuentas);

    @Test
    void pasaElEstadoElTextoSinEspaciosYLaPaginaAlRepositorio() {
        servicio.prestadores("Pendiente", "  ana  ", 2);

        assertThat(cuentas.ultimoFiltro())
                .isEqualTo(new FiltroCuentas(EstadoAdministrativo.Pendiente, "ana", 2));
        assertThat(cuentas.ultimoFiltro().desplazamiento()).isEqualTo(20);
    }

    @Test
    void sinFiltrosPideLaPrimeraPaginaDeDiezSinEstadoNiTexto() {
        servicio.turistas(null, " ", 0);

        assertThat(cuentas.ultimoFiltro()).isEqualTo(new FiltroCuentas(null, null, 0));
        assertThat(FiltroCuentas.TAMANO_PAGINA).isEqualTo(10);
    }

    @Test
    void unEstadoQueNoCorrespondeAlRolEsNoPermitido() {
        assertThatThrownBy(() -> servicio.turistas("Pendiente", null, 0))
                .isInstanceOfSatisfying(DatosRegistroInvalidosException.class,
                        e -> assertThat(e.getCampos()).containsEntry("estado", MotivoInvalido.NO_PERMITIDO));
        assertThatThrownBy(() -> servicio.prestadores("Desactivado", null, 0))
                .isInstanceOf(DatosRegistroInvalidosException.class);
        assertThatThrownBy(() -> servicio.prestadores("activo", null, 0))
                .isInstanceOf(DatosRegistroInvalidosException.class);
    }

    @Test
    void unTextoDeMasDe100CaracteresYUnaPaginaNegativaSonInvalidos() {
        assertThatThrownBy(() -> servicio.prestadores(null, "a".repeat(101), -1))
                .isInstanceOfSatisfying(DatosRegistroInvalidosException.class, e -> assertThat(e.getCampos())
                        .containsEntry("q", MotivoInvalido.LONGITUD_MAXIMA)
                        .containsEntry("pagina", MotivoInvalido.FORMATO));
    }

    @Test
    void unPrestadorInexistenteEsCuentaNoEncontrada() {
        assertThatThrownBy(() -> servicio.prestador(UUID.randomUUID())).isInstanceOf(CuentaNoEncontradaException.class);
    }
}
