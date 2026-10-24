package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence;

import co.edu.unimagdalena.samaria360.auth.domain.model.CategoriaInteres;
import co.edu.unimagdalena.samaria360.auth.domain.model.CodigoPais;
import co.edu.unimagdalena.samaria360.auth.domain.model.PerfilTurista;
import co.edu.unimagdalena.samaria360.auth.infrastructure.PruebaIntegracion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Perfil del turista contra PostgreSQL real: el reemplazo de las categorías es atómico (DEC-85). */
@PruebaIntegracion
class PerfilPersistenceAdapterTest {

    @Autowired
    private PerfilPersistenceAdapter adaptador;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transaccion;

    @Test
    void actualizarSinTransaccionFallaDeInmediato() {
        assertThatThrownBy(() -> adaptador.actualizar(UUID.randomUUID(), perfil(Set.of(CategoriaInteres.Cultura))))
                .isInstanceOf(IllegalTransactionStateException.class);
    }

    @Test
    void siLaTransaccionFallaDespuesDelReemplazoNoQuedaNadaDelCambio() {
        UUID id = crearTurista();

        assertThatThrownBy(() -> transaccion.executeWithoutResult(estado -> {
            adaptador.actualizar(id, new PerfilTurista("Otro", null, "Distinto", null, new CodigoPais("US"),
                    Set.of(CategoriaInteres.Aventura)));
            throw new IllegalStateException("fallo simulado después del reemplazo");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(jdbc.queryForObject("SELECT primer_nombre FROM turista WHERE id_usuario = ?", String.class, id))
                .isEqualTo("Ana");
        assertThat(jdbc.queryForList("SELECT categoria FROM turista_categoria_interes WHERE id_usuario = ?"
                + " ORDER BY categoria", String.class, id)).containsExactly("Cultura", "Naturaleza");
    }

    @Test
    void reemplazaElConjuntoDeCategoriasYDevuelveFalseSiNoEsTurista() {
        UUID id = crearTurista();

        Boolean actualizado = transaccion.execute(estado ->
                adaptador.actualizar(id, perfil(Set.of(CategoriaInteres.Aventura))));

        assertThat(actualizado).isTrue();
        assertThat(adaptador.buscar(id)).hasValueSatisfying(actual ->
                assertThat(actual.perfil().categoriasOrdenadas()).containsExactly(CategoriaInteres.Aventura));
        Boolean otro = transaccion.execute(estado -> adaptador.actualizar(UUID.randomUUID(), perfil(Set.of(
                CategoriaInteres.Cultura))));
        assertThat(otro).isFalse();
    }

    private static PerfilTurista perfil(Set<CategoriaInteres> categorias) {
        return new PerfilTurista("Ana", null, "Pérez", null, new CodigoPais("CO"), categorias);
    }

    private UUID crearTurista() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario (id_usuario, id_rol, correo, contrasena_hash, fecha_registro,"
                + " intentos_fallidos, estado_cuenta) VALUES (?, 1, ?, 'x', CURRENT_DATE, 0, 'Activo')",
                id, id + "@perfil.test");
        jdbc.update("INSERT INTO turista (id_usuario, primer_nombre, primer_apellido, pais_origen, acepto_politicas)"
                + " VALUES (?, 'Ana', 'Pérez', 'CO', TRUE)", id);
        jdbc.update("INSERT INTO turista_categoria_interes (id_usuario, categoria) VALUES (?, 'Cultura'),"
                + " (?, 'Naturaleza')", id, id);
        return id;
    }
}
