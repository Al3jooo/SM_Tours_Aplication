package co.edu.unimagdalena.samaria360.auth.domain.service;

import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaInactivaException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaPendienteValidacionException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaRechazadaException;
import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoCuenta;
import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoValidacion;
import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;
import co.edu.unimagdalena.samaria360.auth.domain.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Estado de la cuenta (RF22) y validación del prestador (RF02, RF28; decisión D3), después de la contraseña. */
class PoliticaAccesoTest {

    private final PoliticaAcceso politica = new PoliticaAcceso();

    @ParameterizedTest
    @EnumSource(value = EstadoCuenta.class, names = {"Inactivo", "Desactivado"})
    void unaCuentaNoActivaDaCuentaInactiva(EstadoCuenta estado) {
        assertThatThrownBy(() -> politica.verificar(usuario(Rol.Turista, estado), Optional.empty()))
                .isInstanceOf(CuentaInactivaException.class);
    }

    @Test
    void unaCuentaActivaSinValidacionPuedeEntrar() {
        assertThatCode(() -> politica.verificar(usuario(Rol.Turista, EstadoCuenta.Activo), Optional.empty()))
                .doesNotThrowAnyException();
    }

    @Test
    void unPrestadorPendienteNoPuedeEntrar() {
        assertThatThrownBy(() -> politica.verificar(prestadorActivo(), Optional.of(EstadoValidacion.Pendiente)))
                .isInstanceOf(CuentaPendienteValidacionException.class);
    }

    @Test
    void unPrestadorRechazadoNoPuedeEntrar() {
        assertThatThrownBy(() -> politica.verificar(prestadorActivo(), Optional.of(EstadoValidacion.Rechazado)))
                .isInstanceOf(CuentaRechazadaException.class);
    }

    @Test
    void unPrestadorValidadoPuedeEntrar() {
        assertThatCode(() -> politica.verificar(prestadorActivo(), Optional.of(EstadoValidacion.Activo)))
                .doesNotThrowAnyException();
    }

    @Test
    void laCuentaInactivaSeEvaluaAntesQueLaValidacion() {
        Usuario inactivo = usuario(Rol.Prestador, EstadoCuenta.Inactivo);

        assertThatThrownBy(() -> politica.verificar(inactivo, Optional.of(EstadoValidacion.Pendiente)))
                .isInstanceOf(CuentaInactivaException.class);
    }

    private static Usuario prestadorActivo() {
        return usuario(Rol.Prestador, EstadoCuenta.Activo);
    }

    private static Usuario usuario(Rol rol, EstadoCuenta estado) {
        return new Usuario(UUID.randomUUID(), "cuenta@samaria360.test", "$2a$10$hash", rol, estado, 0, null);
    }
}
