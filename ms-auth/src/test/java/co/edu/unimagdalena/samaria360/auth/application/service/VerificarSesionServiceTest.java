package co.edu.unimagdalena.samaria360.auth.application.service;

import co.edu.unimagdalena.samaria360.auth.application.fake.UsuarioRepositoryEnMemoria;
import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoCuenta;
import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;
import co.edu.unimagdalena.samaria360.auth.domain.model.Usuario;
import co.edu.unimagdalena.samaria360.auth.domain.service.PoliticaSesion;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** ms-auth compara el claim ver contra su propia tabla (DEC-29). */
class VerificarSesionServiceTest {

    private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private final UsuarioRepositoryEnMemoria usuarios = new UsuarioRepositoryEnMemoria();
    private final VerificarSesionService servicio = new VerificarSesionService(usuarios, new PoliticaSesion());

    @Test
    void conLaMismaVersionLaSesionVale() {
        usuarios.agregar(new Usuario(ID, "a@b.test", "hash", Rol.Turista, EstadoCuenta.Activo, 0, null, 3));

        assertThat(servicio.sesionVigente(ID, 3)).isTrue();
    }

    @Test
    void conUnaVersionMenorLaSesionNoVale() {
        usuarios.agregar(new Usuario(ID, "a@b.test", "hash", Rol.Turista, EstadoCuenta.Activo, 0, null, 3));

        assertThat(servicio.sesionVigente(ID, 2)).isFalse();
    }

    @Test
    void unUsuarioInexistenteSeAceptaYLoDecideQuienLoConsulta() {
        assertThat(servicio.sesionVigente(ID, 0)).isTrue();
    }
}
