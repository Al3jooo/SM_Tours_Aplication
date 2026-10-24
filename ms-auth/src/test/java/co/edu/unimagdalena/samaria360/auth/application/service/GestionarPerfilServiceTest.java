package co.edu.unimagdalena.samaria360.auth.application.service;

import co.edu.unimagdalena.samaria360.auth.application.fake.PerfilRepositoryEnMemoria;
import co.edu.unimagdalena.samaria360.auth.application.port.in.ComandoPerfil;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PerfilActual;
import co.edu.unimagdalena.samaria360.auth.domain.exception.DatosRegistroInvalidosException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.UsuarioNoEncontradoException;
import co.edu.unimagdalena.samaria360.auth.domain.model.CategoriaInteres;
import co.edu.unimagdalena.samaria360.auth.domain.model.CodigoPais;
import co.edu.unimagdalena.samaria360.auth.domain.model.PerfilTurista;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GestionarPerfilServiceTest {

    private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    private final PerfilRepositoryEnMemoria perfiles = new PerfilRepositoryEnMemoria();
    private final GestionarPerfilService servicio = new GestionarPerfilService(perfiles);

    @Test
    void consultaElPerfilConElCorreo() {
        perfiles.agregar(ID, "ana@x.com", new PerfilTurista("Ana", null, "Pérez", null, new CodigoPais("CO"),
                Set.of(CategoriaInteres.Cultura)));

        PerfilActual actual = servicio.consultar(ID);

        assertThat(actual.correo()).isEqualTo("ana@x.com");
        assertThat(actual.perfil().primerNombre()).isEqualTo("Ana");
    }

    @Test
    void unIdQueNoEsDeUnTuristaNoTienePerfil() {
        assertThatThrownBy(() -> servicio.consultar(ID)).isInstanceOf(UsuarioNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.actualizar(ID, comando(List.of("Cultura"))))
                .isInstanceOf(UsuarioNoEncontradoException.class);
    }

    @Test
    void actualizarReemplazaElPerfilYDevuelveElResultado() {
        perfiles.agregar(ID, "ana@x.com", new PerfilTurista("Ana", "María", "Pérez", null, new CodigoPais("CO"),
                Set.of(CategoriaInteres.Cultura)));

        PerfilActual actual = servicio.actualizar(ID, new ComandoPerfil(" Lucía ", null, "Gómez", "Ruiz", "us",
                List.of("Aventura", "Naturaleza")));

        assertThat(actual.correo()).isEqualTo("ana@x.com");
        assertThat(actual.perfil().primerNombre()).isEqualTo("Lucía");
        assertThat(actual.perfil().segundoNombre()).as("el PUT reemplaza el perfil completo").isNull();
        assertThat(actual.perfil().paisOrigen().valor()).isEqualTo("US");
        assertThat(actual.perfil().categoriasOrdenadas())
                .containsExactly(CategoriaInteres.Naturaleza, CategoriaInteres.Aventura);
        assertThat(servicio.consultar(ID)).isEqualTo(actual);
    }

    @Test
    void unPerfilInvalidoNoSeGuarda() {
        perfiles.agregar(ID, "ana@x.com", new PerfilTurista("Ana", null, "Pérez", null, new CodigoPais("CO"),
                Set.of(CategoriaInteres.Cultura)));

        assertThatThrownBy(() -> servicio.actualizar(ID, comando(List.of("Cultura", "Cultura"))))
                .isInstanceOf(DatosRegistroInvalidosException.class);
        assertThat(perfiles.actualizaciones()).isZero();
    }

    private static ComandoPerfil comando(List<String> categorias) {
        return new ComandoPerfil("Ana", null, "Pérez", null, "CO", categorias);
    }
}
