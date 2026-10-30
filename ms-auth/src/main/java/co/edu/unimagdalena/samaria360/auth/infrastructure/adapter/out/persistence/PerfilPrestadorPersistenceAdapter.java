package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence;

import co.edu.unimagdalena.samaria360.auth.application.port.in.PerfilPrestador;
import co.edu.unimagdalena.samaria360.auth.application.port.out.PerfilPrestadorRepositoryPort;
import co.edu.unimagdalena.samaria360.auth.domain.model.DatosComercialesPrestador;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Perfil comercial del Prestador con SQL nativo (RF43): {@code PrestadorEntity} no tiene setters. Solo cambian
 * {@code correo_corporativo} y {@code telefono}; {@code usuario.correo}, la credencial, no (decisión D2, DEC-188).
 */
@Component
public class PerfilPrestadorPersistenceAdapter implements PerfilPrestadorRepositoryPort {

    private final JdbcTemplate jdbc;

    public PerfilPrestadorPersistenceAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<PerfilPrestador> buscar(UUID idPrestador) {
        return jdbc.query("""
                SELECT u.correo, p.nombre_representante_legal, p.razon_social, p.rnt, p.correo_corporativo,
                       p.telefono
                FROM prestador p
                JOIN usuario u ON u.id_usuario = p.id_usuario
                WHERE p.id_usuario = ?
                """, (rs, n) -> new PerfilPrestador(rs.getString("correo"), rs.getString("nombre_representante_legal"),
                rs.getString("razon_social"), rs.getString("rnt"), rs.getString("correo_corporativo"),
                rs.getString("telefono")), idPrestador).stream().findFirst();
    }

    @Override
    public boolean actualizar(UUID idPrestador, DatosComercialesPrestador datos) {
        return jdbc.update("UPDATE prestador SET correo_corporativo = ?, telefono = ? WHERE id_usuario = ?",
                datos.correoCorporativo().valor(), datos.telefono(), idPrestador) > 0;
    }
}
