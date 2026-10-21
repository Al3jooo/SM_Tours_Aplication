package co.edu.unimagdalena.samaria360.booking.infrastructure.adapter.out.persistence;

import co.edu.unimagdalena.samaria360.booking.application.port.out.CuentaRepositoryPort;
import co.edu.unimagdalena.samaria360.booking.domain.model.Cuenta;
import co.edu.unimagdalena.samaria360.booking.domain.model.EstadoCuenta;
import co.edu.unimagdalena.samaria360.booking.domain.model.RolCuenta;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

/** {@code cuenta} con SQL nativo, sin entidades, como el inventario: un solo UPDATE condicional por versión. */
@Component
public class CuentaJdbcAdapter implements CuentaRepositoryPort {

    private final NamedParameterJdbcTemplate jdbc;

    public CuentaJdbcAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<Cuenta> buscar(UUID id) {
        List<Cuenta> cuentas = jdbc.query("SELECT rol, estado, version FROM cuenta WHERE id_usuario = :id",
                new MapSqlParameterSource("id", id), (rs, n) -> new Cuenta(id, RolCuenta.valueOf(rs.getString("rol")),
                        EstadoCuenta.valueOf(rs.getString("estado")), rs.getLong("version")));
        return cuentas.stream().findFirst();
    }

    @Override
    public OptionalLong versionDe(UUID id) {
        List<Long> versiones = jdbc.queryForList("SELECT version FROM cuenta WHERE id_usuario = :id",
                new MapSqlParameterSource("id", id), Long.class);
        return versiones.isEmpty() ? OptionalLong.empty() : OptionalLong.of(versiones.get(0));
    }

    /** INSERT ... ON CONFLICT DO UPDATE ... WHERE version menor: 0 filas es «ya había una versión igual o mayor». */
    @Override
    public boolean guardarSiEsMasNueva(Cuenta cuenta, Instant ahora) {
        return jdbc.update("""
                        INSERT INTO cuenta (id_usuario, rol, estado, version, actualizado_en)
                        VALUES (:id, :rol, :estado, :version, :ahora)
                        ON CONFLICT (id_usuario) DO UPDATE
                        SET rol = EXCLUDED.rol, estado = EXCLUDED.estado, version = EXCLUDED.version,
                            actualizado_en = EXCLUDED.actualizado_en
                        WHERE cuenta.version < EXCLUDED.version""",
                new MapSqlParameterSource("id", cuenta.id()).addValue("rol", cuenta.rol().name())
                        .addValue("estado", cuenta.estado().name()).addValue("version", cuenta.version())
                        .addValue("ahora", Timestamp.from(ahora))) == 1;
    }
}
