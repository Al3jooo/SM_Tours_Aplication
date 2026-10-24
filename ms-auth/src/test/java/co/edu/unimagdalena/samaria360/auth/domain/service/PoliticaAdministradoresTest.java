package co.edu.unimagdalena.samaria360.auth.domain.service;

import co.edu.unimagdalena.samaria360.auth.domain.exception.UltimoAdministradorException;
import co.edu.unimagdalena.samaria360.auth.domain.model.CambioRol;
import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Siempre queda al menos un Administrador activo (DEC-82, DEC-106). */
class PoliticaAdministradoresTest {

    private static final UUID A = UUID.fromString("00000000-0000-4000-8000-0000000000a1");
    private static final UUID B = UUID.fromString("00000000-0000-4000-8000-0000000000b1");

    private final PoliticaAdministradores politica = new PoliticaAdministradores();

    @Test
    void degradarAUnoDeDosAdministradoresEsValido() {
        assertThatCode(() -> politica.verificarQueQuedeUno(List.of(A, B), new CambioRol(B, Rol.Administrador,
                Rol.Gestor, 1))).doesNotThrowAnyException();
    }

    @Test
    void degradarAlUnicoAdministradorActivoNoEsValido() {
        assertThatThrownBy(() -> politica.verificarQueQuedeUno(List.of(B), new CambioRol(B, Rol.Administrador,
                Rol.Turista, 1))).isInstanceOf(UltimoAdministradorException.class);
        // Con el conjunto bloqueado vacío (la cuenta ya no estaba activa), tampoco.
        assertThatThrownBy(() -> politica.verificarQueQuedeUno(List.of(), new CambioRol(B, Rol.Administrador,
                Rol.Gestor, 1))).isInstanceOf(UltimoAdministradorException.class);
    }

    @Test
    void unCambioQueNoQuitaUnAdministradorNoSeRevisa() {
        assertThatCode(() -> politica.verificarQueQuedeUno(List.of(), new CambioRol(B, Rol.Gestor,
                Rol.Turista, 1))).doesNotThrowAnyException();
    }
}
