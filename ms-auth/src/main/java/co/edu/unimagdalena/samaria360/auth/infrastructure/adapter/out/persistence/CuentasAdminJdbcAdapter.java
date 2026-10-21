package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence;

import co.edu.unimagdalena.samaria360.auth.application.port.in.FiltroCuentas;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PaginaCuentas;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PrestadorDetalle;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PrestadorResumen;
import co.edu.unimagdalena.samaria360.auth.application.port.in.TuristaResumen;
import co.edu.unimagdalena.samaria360.auth.application.port.out.CuentasAdminRepositoryPort;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaNoEncontradaException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.EstadoCambiadoException;
import co.edu.unimagdalena.samaria360.auth.domain.model.CambioEstadoCuenta;
import co.edu.unimagdalena.samaria360.auth.domain.model.CuentaAdministrada;
import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoAdministrativo;
import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoCuenta;
import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoValidacion;
import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistencia de «Administrar cuentas» con SQL nativo, como el inventario de ms-booking: el cambio de estado es un
 * UPDATE condicional (DEC-39) y nunca se carga la fila para modificarla en memoria. Las acciones corren dentro de la
 * transacción que abre {@code AdministrarCuentasTransaccional}.
 */
@Component
public class CuentasAdminJdbcAdapter implements CuentasAdminRepositoryPort {

    private static final Logger LOG = LoggerFactory.getLogger(CuentasAdminJdbcAdapter.class);

    /** Estado derivado del prestador en SQL: la misma precedencia que {@code EstadoAdministrativo.dePrestador}. */
    private static final String ESTADO_PRESTADOR = """
            CASE WHEN p.estado_validacion = 'Pendiente' THEN 'Pendiente'
                 WHEN p.estado_validacion = 'Rechazado' THEN 'Rechazado'
                 WHEN u.estado_cuenta = 'Activo' THEN 'Activo'
                 ELSE 'Inactivo' END""";

    /** Para el turista, todo lo que no es Activo (incluido el «Inactivo» heredado) es Desactivado. */
    private static final String ESTADO_TURISTA =
            "CASE WHEN u.estado_cuenta = 'Activo' THEN 'Activo' ELSE 'Desactivado' END";

    private static final String NOMBRE_TURISTA = "(t.primer_nombre || ' ' || t.primer_apellido)";

    private final JdbcTemplate jdbc;

    public CuentasAdminJdbcAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<CuentaAdministrada> buscarCuenta(UUID idUsuario) {
        return jdbc.query("""
                SELECT r.nombre AS rol, u.estado_cuenta::text AS cuenta, p.estado_validacion, u.version_cuenta
                FROM usuario u
                JOIN rol r ON r.id_rol = u.id_rol
                LEFT JOIN prestador p ON p.id_usuario = u.id_usuario
                WHERE u.id_usuario = ? AND r.nombre IN ('Turista', 'Prestador')
                """, rs -> {
            if (!rs.next()) {
                return Optional.<CuentaAdministrada>empty();
            }
            String validacion = rs.getString("estado_validacion");
            return Optional.of(new CuentaAdministrada(idUsuario, Rol.valueOf(rs.getString("rol")),
                    EstadoCuenta.valueOf(rs.getString("cuenta")),
                    validacion == null ? null : EstadoValidacion.desdeTexto(validacion),
                    rs.getLong("version_cuenta")));
        }, idUsuario);
    }

    @Override
    public void aplicar(CambioEstadoCuenta cambio, UUID revisadoPor, Instant ahora) {
        int filas = jdbc.update("""
                UPDATE usuario
                SET version_cuenta = version_cuenta + 1, estado_cuenta = ?::estado_cuenta
                WHERE id_usuario = ? AND version_cuenta = ? AND estado_cuenta = ?::estado_cuenta
                """, cambio.cuentaNueva().name(), cambio.idUsuario(), cambio.versionEsperada(),
                cambio.cuentaEsperada().name());
        if (filas == 0) {
            throw estadoCambiado(cambio.idUsuario());
        }
        if (cambio.cambiaValidacion()) {
            int validadas = jdbc.update("""
                    UPDATE prestador
                    SET estado_validacion = ?, motivo_rechazo = ?, revisado_en = ?, revisado_por = ?
                    WHERE id_usuario = ? AND estado_validacion = ?
                    """, cambio.validacionNueva().texto(), cambio.motivoRechazo(), Timestamp.from(ahora), revisadoPor,
                    cambio.idUsuario(), cambio.validacionEsperada().texto());
            if (validadas == 0) {
                throw estadoCambiado(cambio.idUsuario());
            }
        }
    }

    /** 0 filas: otro cambio llegó antes. Se lee el estado actual para devolverlo (409 ESTADO_CAMBIADO). */
    private EstadoCambiadoException estadoCambiado(UUID idUsuario) {
        CuentaAdministrada actual = buscarCuenta(idUsuario)
                .orElseThrow(() -> new CuentaNoEncontradaException(idUsuario));
        return new EstadoCambiadoException(actual.estado());
    }

    @Override
    public PaginaCuentas<PrestadorResumen> listarPrestadores(FiltroCuentas filtro) {
        List<Object> parametros = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE r.nombre = 'Prestador'");
        if (filtro.estado() != null) {
            where.append(" AND ").append(ESTADO_PRESTADOR).append(" = ?");
            parametros.add(filtro.estado().name());
        }
        if (filtro.texto() != null) {
            String patron = patron(filtro.texto());
            where.append(" AND (p.nombre_representante_legal ILIKE ? ESCAPE '\\' OR p.correo_corporativo ILIKE ?"
                    + " ESCAPE '\\' OR p.rnt ILIKE ? ESCAPE '\\')");
            parametros.add(patron);
            parametros.add(patron);
            parametros.add(patron);
        }
        String desde = " FROM usuario u JOIN rol r ON r.id_rol = u.id_rol"
                + " JOIN prestador p ON p.id_usuario = u.id_usuario" + where;
        long total = contar(desde, parametros);
        List<Object> conPagina = new ArrayList<>(parametros);
        conPagina.add(FiltroCuentas.TAMANO_PAGINA);
        conPagina.add(filtro.desplazamiento());
        List<PrestadorResumen> elementos = jdbc.query("SELECT u.id_usuario, p.nombre_representante_legal, p.rnt,"
                + " p.correo_corporativo, u.fecha_registro, " + ESTADO_PRESTADOR + " AS estado" + desde
                + " ORDER BY u.fecha_registro, u.id_usuario LIMIT ? OFFSET ?",
                (rs, n) -> new PrestadorResumen(rs.getObject("id_usuario", UUID.class),
                        rs.getString("nombre_representante_legal"), rs.getString("rnt"),
                        rs.getString("correo_corporativo"), rs.getDate("fecha_registro").toLocalDate(),
                        EstadoAdministrativo.desdeTexto(rs.getString("estado"))),
                conPagina.toArray());
        return new PaginaCuentas<>(elementos, filtro.pagina(), FiltroCuentas.TAMANO_PAGINA, total);
    }

    @Override
    public PaginaCuentas<TuristaResumen> listarTuristas(FiltroCuentas filtro) {
        List<Object> parametros = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE r.nombre = 'Turista'");
        if (filtro.estado() != null) {
            where.append(" AND ").append(ESTADO_TURISTA).append(" = ?");
            parametros.add(filtro.estado().name());
        }
        if (filtro.texto() != null) {
            String patron = patron(filtro.texto());
            where.append(" AND (").append(NOMBRE_TURISTA).append(" ILIKE ? ESCAPE '\\' OR u.correo ILIKE ? ESCAPE '\\')");
            parametros.add(patron);
            parametros.add(patron);
        }
        String desde = " FROM usuario u JOIN rol r ON r.id_rol = u.id_rol"
                + " JOIN turista t ON t.id_usuario = u.id_usuario" + where;
        long total = contar(desde, parametros);
        List<Object> conPagina = new ArrayList<>(parametros);
        conPagina.add(FiltroCuentas.TAMANO_PAGINA);
        conPagina.add(filtro.desplazamiento());
        List<TuristaResumen> elementos = jdbc.query("SELECT u.id_usuario, " + NOMBRE_TURISTA + " AS nombre, u.correo,"
                + " t.pais_origen, u.fecha_registro, u.estado_cuenta::text AS cuenta" + desde
                + " ORDER BY u.fecha_registro, u.id_usuario LIMIT ? OFFSET ?",
                (rs, n) -> turista(rs.getObject("id_usuario", UUID.class), rs.getString("nombre"),
                        rs.getString("correo"), rs.getString("pais_origen"), rs.getDate("fecha_registro").toLocalDate(),
                        rs.getString("cuenta")),
                conPagina.toArray());
        return new PaginaCuentas<>(elementos, filtro.pagina(), FiltroCuentas.TAMANO_PAGINA, total);
    }

    @Override
    public Optional<PrestadorDetalle> detallePrestador(UUID idPrestador) {
        return jdbc.query("SELECT u.id_usuario, p.nombre_representante_legal, p.razon_social, p.rnt,"
                + " p.correo_corporativo, p.telefono, p.acepto_politicas, u.fecha_registro, " + ESTADO_PRESTADOR
                + " AS estado, p.motivo_rechazo, p.revisado_en"
                + " FROM usuario u JOIN prestador p ON p.id_usuario = u.id_usuario WHERE u.id_usuario = ?", rs -> {
            if (!rs.next()) {
                return Optional.<PrestadorDetalle>empty();
            }
            Timestamp revisadoEn = rs.getTimestamp("revisado_en");
            return Optional.of(new PrestadorDetalle(idPrestador, rs.getString("nombre_representante_legal"),
                    rs.getString("razon_social"), rs.getString("rnt"), rs.getString("correo_corporativo"),
                    rs.getString("telefono"), rs.getBoolean("acepto_politicas"),
                    rs.getDate("fecha_registro").toLocalDate(),
                    EstadoAdministrativo.desdeTexto(rs.getString("estado")), rs.getString("motivo_rechazo"),
                    revisadoEn == null ? null : revisadoEn.toInstant()));
        }, idPrestador);
    }

    @Override
    public Optional<TuristaResumen> detalleTurista(UUID idTurista) {
        return jdbc.query("SELECT " + NOMBRE_TURISTA + " AS nombre, u.correo, t.pais_origen, u.fecha_registro,"
                + " u.estado_cuenta::text AS cuenta FROM usuario u JOIN turista t ON t.id_usuario = u.id_usuario"
                + " WHERE u.id_usuario = ?", rs -> {
            if (!rs.next()) {
                return Optional.<TuristaResumen>empty();
            }
            return Optional.of(turista(idTurista, rs.getString("nombre"), rs.getString("correo"),
                    rs.getString("pais_origen"), rs.getDate("fecha_registro").toLocalDate(), rs.getString("cuenta")));
        }, idTurista);
    }

    /** Un turista «Inactivo» es un dato heredado (Inactivo es el estado de RF22, del prestador): se muestra Desactivado. */
    private static TuristaResumen turista(UUID id, String nombre, String correo, String pais,
                                          java.time.LocalDate fechaRegistro, String cuenta) {
        EstadoCuenta estadoCuenta = EstadoCuenta.valueOf(cuenta);
        if (estadoCuenta == EstadoCuenta.Inactivo) {
            LOG.warn("El turista {} tiene estado_cuenta Inactivo, propio de un prestador; se muestra como Desactivado", id);
        }
        return new TuristaResumen(id, nombre, correo, pais, fechaRegistro, EstadoAdministrativo.deTurista(estadoCuenta));
    }

    private long contar(String desde, List<Object> parametros) {
        Long total = jdbc.queryForObject("SELECT count(*)" + desde, Long.class, parametros.toArray());
        return total == null ? 0 : total;
    }

    /** Contiene el texto, con los comodines de LIKE escapados: «50%» busca esos caracteres, no cualquier cosa. */
    static String patron(String texto) {
        return "%" + texto.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
    }
}
