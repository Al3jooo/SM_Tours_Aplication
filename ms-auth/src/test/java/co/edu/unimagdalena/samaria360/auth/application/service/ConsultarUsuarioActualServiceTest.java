package co.edu.unimagdalena.samaria360.auth.application.service;

import co.edu.unimagdalena.samaria360.auth.application.fake.UsuarioRepositoryEnMemoria;
import co.edu.unimagdalena.samaria360.auth.application.port.in.UsuarioActual;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaInactivaException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.UsuarioNoEncontradoException;
import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoCuenta;
import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;
import co.edu.unimagdalena.samaria360.auth.domain.model.Usuario;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultarUsuarioActualServiceTest {

    private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-000000000002");

    private final UsuarioRepositoryEnMemoria repositorio = new UsuarioRepositoryEnMemoria();
    private final ConsultarUsuarioActualService servicio = new ConsultarUsuarioActualService(repositorio);

    @Test
    void devuelveLosDatosBasicosConElNombreDelSubtipo() {
        repositorio.agregar(usuario("prestador@samaria360.test", Rol.Prestador, EstadoCuenta.Activo), "Carlos Gómez");

        UsuarioActual actual = servicio.consultar(ID);

        assertThat(actual).isEqualTo(new UsuarioActual(
                ID, "prestador@samaria360.test", Rol.Prestador, EstadoCuenta.Activo, "Carlos Gómez"));
    }

    @Test
    void sinNombreEnElSubtipoUsaLaParteDelCorreoAntesDeLaArroba() {
        repositorio.agregar(usuario("gestor@samaria360.test", Rol.Gestor, EstadoCuenta.Activo));

        assertThat(servicio.consultar(ID).nombreVisible()).isEqualTo("gestor");
    }

    @Test
    void unNombreEnBlancoTambienUsaLaParteDelCorreo() {
        repositorio.agregar(usuario("admin@samaria360.test", Rol.Administrador, EstadoCuenta.Activo), "  ");

        assertThat(servicio.consultar(ID).nombreVisible()).isEqualTo("admin");
    }

    @ParameterizedTest
    @EnumSource(value = EstadoCuenta.class, names = {"Inactivo", "Desactivado"})
    void unaCuentaNoActivaNoObtieneSusDatosAunqueElTokenSigaVigente(EstadoCuenta estado) {
        repositorio.agregar(usuario("turista@samaria360.test", Rol.Turista, estado), "Ana Pérez");

        assertThatThrownBy(() -> servicio.consultar(ID))
                .isInstanceOf(CuentaInactivaException.class)
                .extracting(e -> ((CuentaInactivaException) e).getEstadoCuenta())
                .isEqualTo(estado);
    }

    @Test
    void unIdInexistenteLanzaUsuarioNoEncontrado() {
        assertThatThrownBy(() -> servicio.consultar(ID))
                .isInstanceOf(UsuarioNoEncontradoException.class)
                .extracting(e -> ((UsuarioNoEncontradoException) e).getIdUsuario())
                .isEqualTo(ID);
    }

    private static Usuario usuario(String correo, Rol rol, EstadoCuenta estado) {
        return new Usuario(ID, correo, "$2a$10$hash", rol, estado, 0, null);
    }
}
