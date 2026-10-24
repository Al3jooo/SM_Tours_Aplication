package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence;

import co.edu.unimagdalena.samaria360.auth.application.port.in.PerfilActual;
import co.edu.unimagdalena.samaria360.auth.application.port.out.PerfilRepositoryPort;
import co.edu.unimagdalena.samaria360.auth.domain.model.CategoriaInteres;
import co.edu.unimagdalena.samaria360.auth.domain.model.CodigoPais;
import co.edu.unimagdalena.samaria360.auth.domain.model.PerfilTurista;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Perfil del turista con SQL nativo (RF53): {@code TuristaEntity} no tiene setters y el reemplazo de las categorías es
 * un DELETE más un INSERT por categoría. La actualización exige la transacción que abre
 * {@code GestionarPerfilTransaccional}: los datos y las categorías se confirman juntos o no se confirma nada (DEC-85).
 */
@Component
public class PerfilPersistenceAdapter implements PerfilRepositoryPort {

    private final JdbcTemplate jdbc;

    public PerfilPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<PerfilActual> buscar(UUID idTurista) {
        Set<CategoriaInteres> categorias = EnumSet.noneOf(CategoriaInteres.class);
        jdbc.queryForList("SELECT categoria FROM turista_categoria_interes WHERE id_usuario = ?", String.class,
                idTurista).forEach(categoria -> categorias.add(CategoriaInteres.valueOf(categoria)));
        List<PerfilActual> filas = jdbc.query("""
                SELECT u.correo, t.primer_nombre, t.segundo_nombre, t.primer_apellido, t.segundo_apellido,
                       t.pais_origen
                FROM turista t
                JOIN usuario u ON u.id_usuario = t.id_usuario
                WHERE t.id_usuario = ?
                """, (rs, n) -> new PerfilActual(rs.getString("correo"), new PerfilTurista(
                rs.getString("primer_nombre"), rs.getString("segundo_nombre"), rs.getString("primer_apellido"),
                rs.getString("segundo_apellido"), new CodigoPais(rs.getString("pais_origen")), categorias)),
                idTurista);
        return filas.stream().findFirst();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean actualizar(UUID idTurista, PerfilTurista perfil) {
        int filas = jdbc.update("""
                UPDATE turista
                SET primer_nombre = ?, segundo_nombre = ?, primer_apellido = ?, segundo_apellido = ?, pais_origen = ?
                WHERE id_usuario = ?
                """, perfil.primerNombre(), perfil.segundoNombre(), perfil.primerApellido(), perfil.segundoApellido(),
                perfil.paisOrigen().valor(), idTurista);
        if (filas == 0) {
            return false;
        }
        jdbc.update("DELETE FROM turista_categoria_interes WHERE id_usuario = ?", idTurista);
        jdbc.batchUpdate("INSERT INTO turista_categoria_interes (id_usuario, categoria) VALUES (?, ?)",
                perfil.categoriasOrdenadas().stream().map(c -> new Object[]{idTurista, c.name()}).toList());
        return true;
    }
}
