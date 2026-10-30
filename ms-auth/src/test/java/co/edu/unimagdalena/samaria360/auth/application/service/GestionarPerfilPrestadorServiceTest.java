package co.edu.unimagdalena.samaria360.auth.application.service;

import co.edu.unimagdalena.samaria360.auth.application.port.in.PerfilPrestador;
import co.edu.unimagdalena.samaria360.auth.application.port.out.PerfilPrestadorRepositoryPort;
import co.edu.unimagdalena.samaria360.auth.domain.exception.DatosRegistroInvalidosException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.UsuarioNoEncontradoException;
import co.edu.unimagdalena.samaria360.auth.domain.model.DatosComercialesPrestador;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CU «Actualizar datos comerciales» (RF43, DEC-188), parte de la cuenta, con un doble del puerto: solo cambian el
 * correo corporativo y el teléfono; el RNT llega al Prestador (O16).
 */
class GestionarPerfilPrestadorServiceTest {

    private static final UUID PRESTADOR = UUID.fromString("00000000-0000-4000-8000-0000000000e1");

    private final PerfilesEnMemoria perfiles = new PerfilesEnMemoria();
    private final GestionarPerfilPrestadorService servicio = new GestionarPerfilPrestadorService(perfiles);

    @Test
    void consultarTraeElRntYLosDatosDeSoloLectura() {
        perfiles.agregar(PRESTADOR);

        PerfilPrestador perfil = servicio.consultar(PRESTADOR);

        assertThat(perfil.rnt()).isEqualTo("RNT-00042");
        assertThat(perfil.correo()).isEqualTo("acceso@kayak.test");
        assertThat(perfil.representanteLegal()).isEqualTo("Marta Rojas");
    }

    @Test
    void actualizarCambiaSoloElCorreoCorporativoYElTelefono() {
        perfiles.agregar(PRESTADOR);

        PerfilPrestador perfil = servicio.actualizar(PRESTADOR, " Reservas@Kayak.test ", "+57 311 000 0000");

        assertThat(perfil.correoCorporativo()).isEqualTo("reservas@kayak.test");
        assertThat(perfil.telefono()).isEqualTo("+57 311 000 0000");
        assertThat(perfil.correo()).isEqualTo("acceso@kayak.test");
        assertThat(perfil.rnt()).isEqualTo("RNT-00042");
        assertThat(perfil.razonSocial()).isEqualTo("Kayak Taganga S.A.S.");
    }

    @Test
    void losDatosInvalidosNoEscribenNada() {
        perfiles.agregar(PRESTADOR);

        assertThatThrownBy(() -> servicio.actualizar(PRESTADOR, "sin-arroba", ""))
                .isInstanceOf(DatosRegistroInvalidosException.class);
        assertThat(servicio.consultar(PRESTADOR).correoCorporativo()).isEqualTo("contacto@kayak.test");
    }

    @Test
    void unIdQueNoEsDeUnPrestadorNoExiste() {
        assertThatThrownBy(() -> servicio.consultar(PRESTADOR)).isInstanceOf(UsuarioNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.actualizar(PRESTADOR, "a@b.co", "300"))
                .isInstanceOf(UsuarioNoEncontradoException.class);
    }

    /** Perfiles en memoria; el correo de acceso y el RNT nunca cambian, como en la base. */
    private static final class PerfilesEnMemoria implements PerfilPrestadorRepositoryPort {

        private final Map<UUID, PerfilPrestador> perfiles = new HashMap<>();

        void agregar(UUID id) {
            perfiles.put(id, new PerfilPrestador("acceso@kayak.test", "Marta Rojas", "Kayak Taganga S.A.S.",
                    "RNT-00042", "contacto@kayak.test", "+57 300 000 0000"));
        }

        @Override
        public Optional<PerfilPrestador> buscar(UUID idPrestador) {
            return Optional.ofNullable(perfiles.get(idPrestador));
        }

        @Override
        public boolean actualizar(UUID idPrestador, DatosComercialesPrestador datos) {
            PerfilPrestador actual = perfiles.get(idPrestador);
            if (actual == null) {
                return false;
            }
            perfiles.put(idPrestador, new PerfilPrestador(actual.correo(), actual.representanteLegal(),
                    actual.razonSocial(), actual.rnt(), datos.correoCorporativo().valor(), datos.telefono()));
            return true;
        }
    }
}
