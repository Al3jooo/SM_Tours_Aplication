package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence;

import co.edu.unimagdalena.samaria360.auth.application.port.in.FiltroCuentas;
import co.edu.unimagdalena.samaria360.auth.application.port.in.FiltroPersonal;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PaginaCuentas;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PersonalResumen;
import co.edu.unimagdalena.samaria360.auth.application.port.out.CambioRolRepositoryPort;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaNoElegibleException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaNoEncontradaException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.RolCambiadoException;
import co.edu.unimagdalena.samaria360.auth.domain.model.CambioRol;
import co.edu.unimagdalena.samaria360.auth.domain.model.CuentaConRol;
import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoCuenta;
import co.edu.unimagdalena.samaria360.auth.domain.model.MotivoNoElegible;
import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia del cambio de rol (RF45) con SQL nativo, como «Administrar cuentas»: un UPDATE condicional sobre el rol
 * y la versión (DEC-106) y nunca se carga la fila para modificarla en memoria. Corre dentro de la transacción que abre
 * {@code CambiarRolTransaccional}.
 */
@Component
public class CambioRolJdbcAdapter implements CambioRolRepositoryPort {

    /** El id de un rol por su nombre: el catálogo es de V10 y sus ids no se repiten en el código. */
    private static final String ID_ROL = "(SELECT id_rol FROM rol WHERE nombre = ?)";

    private static final String NOMBRE_TURISTA = "(t.primer_nombre || ' ' || t.primer_apellido)";

    private static final String DESDE = " FROM usuario u JOIN rol r ON r.id_rol = u.id_rol"
            + " LEFT JOIN turista t ON t.id_usuario = u.id_usuario";

    private static final String SELECCION = "SELECT u.id_usuario, " + NOMBRE_TURISTA + " AS nombre, u.correo,"
            + " r.nombre AS rol, u.fecha_registro, t.id_usuario IS NOT NULL AS revocable";

    private final JdbcTemplate jdbc;

    public CambioRolJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<CuentaConRol> buscarCuenta(UUID idUsuario) {
        return jdbc.query("SELECT r.nombre AS rol, u.estado_cuenta::text AS cuenta, u.version_cuenta,"
                + " t.id_usuario IS NOT NULL AS rol_base" + DESDE + " WHERE u.id_usuario = ?", rs -> {
            if (!rs.next()) {
                return Optional.<CuentaConRol>empty();
            }
            return Optional.of(new CuentaConRol(idUsuario, Rol.valueOf(rs.getString("rol")),
                    EstadoCuenta.valueOf(rs.getString("cuenta")), rs.getBoolean("rol_base"),
                    rs.getLong("version_cuenta")));
        }, idUsuario);
    }

    /**
     * Sin JOIN: el id del rol sale de una subconsulta que se evalúa una vez. Así, al obtener el bloqueo después de
     * esperar, PostgreSQL vuelve a evaluar el WHERE sobre la fila confirmada (READ COMMITTED) y deja fuera a quien la
     * otra transacción ya degradó.
     */
    @Override
    public List<UUID> bloquearAdministradoresActivos() {
        return jdbc.queryForList("SELECT u.id_usuario FROM usuario u WHERE u.id_rol = " + ID_ROL
                + " AND u.estado_cuenta = 'Activo' ORDER BY u.id_usuario FOR UPDATE", UUID.class,
                Rol.Administrador.name());
    }

    @Override
    public void aplicar(CambioRol cambio) {
        int filas = jdbc.update("UPDATE usuario SET id_rol = " + ID_ROL + ", version_cuenta = version_cuenta + 1"
                        + " WHERE id_usuario = ? AND id_rol = " + ID_ROL
                        + " AND version_cuenta = ? AND estado_cuenta = 'Activo'",
                cambio.rolNuevo().name(), cambio.idUsuario(), cambio.rolAnterior().name(), cambio.versionEsperada());
        if (filas == 0) {
            throw conflicto(cambio);
        }
        subtipo(cambio.rolAnterior()).ifPresent(tabla ->
                jdbc.update("DELETE FROM " + tabla + " WHERE id_usuario = ?", cambio.idUsuario()));
        subtipo(cambio.rolNuevo()).ifPresent(tabla ->
                jdbc.update("INSERT INTO " + tabla + " (id_usuario) VALUES (?)", cambio.idUsuario()));
    }

    /** 0 filas: otro cambio llegó antes. Se lee la cuenta para responder el 409 que corresponde (o el 404). */
    private RuntimeException conflicto(CambioRol cambio) {
        CuentaConRol actual = buscarCuenta(cambio.idUsuario())
                .orElseThrow(() -> new CuentaNoEncontradaException(cambio.idUsuario()));
        if (actual.getRol() == cambio.rolAnterior() && actual.getCuenta() != EstadoCuenta.Activo) {
            return new CuentaNoElegibleException(MotivoNoElegible.DESACTIVADA);
        }
        return new RolCambiadoException(actual.getRol());
    }

    /** La tabla del subtipo de personal; el Turista conserva siempre su fila en {@code turista}. */
    private static Optional<String> subtipo(Rol rol) {
        return switch (rol) {
            case Gestor -> Optional.of("gestor");
            case Administrador -> Optional.of("administrador");
            case Turista, Prestador -> Optional.empty();
        };
    }

    @Override
    public Optional<PersonalResumen> resumen(UUID idUsuario) {
        return jdbc.query(SELECCION + DESDE + " WHERE u.id_usuario = ?",
                rs -> rs.next() ? Optional.of(personal(rs)) : Optional.<PersonalResumen>empty(), idUsuario);
    }

    @Override
    public PaginaCuentas<PersonalResumen> listarPersonal(FiltroPersonal filtro) {
        List<Object> parametros = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE r.nombre IN ('Gestor', 'Administrador')");
        if (filtro.rol() != null) {
            where.append(" AND r.nombre = ?");
            parametros.add(filtro.rol().name());
        }
        if (filtro.texto() != null) {
            String patron = CuentasAdminJdbcAdapter.patron(filtro.texto());
            where.append(" AND (").append(NOMBRE_TURISTA).append(" ILIKE ? ESCAPE '\\' OR u.correo ILIKE ? ESCAPE '\\')");
            parametros.add(patron);
            parametros.add(patron);
        }
        Long total = jdbc.queryForObject("SELECT count(*)" + DESDE + where, Long.class, parametros.toArray());
        List<Object> conPagina = new ArrayList<>(parametros);
        conPagina.add(FiltroCuentas.TAMANO_PAGINA);
        conPagina.add(filtro.desplazamiento());
        List<PersonalResumen> elementos = jdbc.query(SELECCION + DESDE + where
                        + " ORDER BY u.fecha_registro, u.id_usuario LIMIT ? OFFSET ?",
                (rs, n) -> personal(rs), conPagina.toArray());
        return new PaginaCuentas<>(elementos, filtro.pagina(), FiltroCuentas.TAMANO_PAGINA, total == null ? 0 : total);
    }

    private static PersonalResumen personal(ResultSet rs) throws SQLException {
        return new PersonalResumen(rs.getObject("id_usuario", UUID.class), rs.getString("nombre"),
                rs.getString("correo"), Rol.valueOf(rs.getString("rol")), rs.getDate("fecha_registro").toLocalDate(),
                rs.getBoolean("revocable"));
    }
}
