package co.edu.unimagdalena.samaria360.auth.application.service;

import co.edu.unimagdalena.samaria360.auth.application.fake.UsuarioRepositoryEnMemoria;
import co.edu.unimagdalena.samaria360.auth.application.port.in.UsuarioActual;
import co.edu.unimagdalena.samaria360.auth.domain.exception.UsuarioNoEncontradoException;
import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoCuenta;
import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;
import co.edu.unimagdalena.samaria360.auth.domain.model.Usuario;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultarUsuarioActualServiceTest {

    private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-000000000002");

    private final UsuarioRepositoryEnMemoria repositorio = new UsuarioRepositoryEnMemoria();
    private final ConsultarUsuarioActualService servicio = new ConsultarUsuarioActualService(repositorio);

    @Test
    void devuelveLosDatosBasicosDelUsuario() {
        repositorio.agregar(new Usuario(ID, "prestador@samaria360.test", "$2a$10$hash", Rol.Prestador,
                EstadoCuenta.Activo, 0, null));

        UsuarioActual actual = servicio.consultar(ID);

        assertThat(actual).isEqualTo(
                new UsuarioActual(ID, "prestador@samaria360.test", Rol.Prestador, EstadoCuenta.Activo));
    }

    @Test
    void unIdInexistenteLanzaUsuarioNoEncontrado() {
        assertThatThrownBy(() -> servicio.consultar(ID))
                .isInstanceOf(UsuarioNoEncontradoException.class)
                .extracting(e -> ((UsuarioNoEncontradoException) e).getIdUsuario())
                .isEqualTo(ID);
    }
}
