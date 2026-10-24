package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence;

import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaNoElegibleException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.RolCambiadoException;
import co.edu.unimagdalena.samaria360.auth.domain.model.CambioRol;
import co.edu.unimagdalena.samaria360.auth.domain.model.CuentaConRol;
import co.edu.unimagdalena.samaria360.auth.domain.model.MotivoNoElegible;
import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;
import co.edu.unimagdalena.samaria360.auth.infrastructure.PruebaIntegracion;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence.entity.UsuarioEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Adaptador JDBC del cambio de rol (RF45) contra PostgreSQL: lectura, bloqueo, UPDATE condicional y subtipos. */
@PruebaIntegracion
class CambioRolJdbcAdapterTest {

    @Autowired
    private CambioRolJdbcAdapter adaptador;

    @Autowired
    private UsuarioJpaRepository usuarios;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transaccion;

    @Test
    void buscarCuentaDiceSiTieneFilaDeTurista() {
        UUID turista = crear(1, true);
        UUID gestorSinTurista = crear(3, false);

        assertThat(adaptador.buscarCuenta(turista)).hasValueSatisfying(c -> {
            assertThat(c.getRol()).isEqualTo(Rol.Turista);
            assertThat(c.tieneRolBase()).isTrue();
            assertThat(c.getVersion()).isZero();
        });
        assertThat(adaptador.buscarCuenta(gestorSinTurista)).hasValueSatisfying(c -> {
            assertThat(c.getRol()).isEqualTo(Rol.Gestor);
            assertThat(c.tieneRolBase()).isFalse();
        });
        assertThat(adaptador.buscarCuenta(UUID.randomUUID())).isEmpty();
    }

    @Test
    void bloquearDevuelveSoloLosAdministradoresActivos() {
        UUID activo = crear(4, false);
        UUID inactivo = crear(4, false);
        jdbc.update("UPDATE usuario SET estado_cuenta = 'Inactivo' WHERE id_usuario = ?", inactivo);
        UUID gestor = crear(3, false);

        List<UUID> bloqueados = transaccion.execute(estado -> adaptador.bloquearAdministradoresActivos());

        assertThat(bloqueados).contains(activo).doesNotContain(inactivo, gestor);
    }

    @Test
    void aplicarCambiaElRolLaVersionYLosSubtiposSinTocarElTurista() {
        UUID id = crear(1, true);

        transaccion.executeWithoutResult(estado -> adaptador.aplicar(new CambioRol(id, Rol.Turista, Rol.Gestor, 0)));
        assertThat(rol(id)).isEqualTo(3);
        assertThat(cuenta("gestor", id)).isEqualTo(1);

        transaccion.executeWithoutResult(estado ->
                adaptador.aplicar(new CambioRol(id, Rol.Gestor, Rol.Administrador, 1)));
        assertThat(cuenta("gestor", id)).isZero();
        assertThat(cuenta("administrador", id)).isEqualTo(1);

        transaccion.executeWithoutResult(estado ->
                adaptador.aplicar(new CambioRol(id, Rol.Administrador, Rol.Turista, 2)));
        assertThat(rol(id)).isEqualTo(1);
        assertThat(cuenta("administrador", id)).isZero();
        assertThat(cuenta("turista", id)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT version_cuenta FROM usuario WHERE id_usuario = ?", Long.class, id))
                .isEqualTo(3);
    }

    @Test
    void conCeroFilasDistingueElRolCambiadoDeLaCuentaDesactivada() {
        UUID id = crear(1, true);

        // Versión vieja, mismo rol y cuenta Activa: otro cambio llegó antes.
        jdbc.update("UPDATE usuario SET version_cuenta = 2 WHERE id_usuario = ?", id);
        assertThatThrownBy(() -> transaccion.executeWithoutResult(estado ->
                adaptador.aplicar(new CambioRol(id, Rol.Turista, Rol.Gestor, 1))))
                .isInstanceOfSatisfying(RolCambiadoException.class,
                        e -> assertThat(e.getRolActual()).isEqualTo(Rol.Turista));

        // Mismo rol, pero la desactivaron mientras tanto (RF62).
        jdbc.update("UPDATE usuario SET estado_cuenta = 'Desactivado' WHERE id_usuario = ?", id);
        assertThatThrownBy(() -> transaccion.executeWithoutResult(estado ->
                adaptador.aplicar(new CambioRol(id, Rol.Turista, Rol.Gestor, 2))))
                .isInstanceOfSatisfying(CuentaNoElegibleException.class,
                        e -> assertThat(e.getMotivo()).isEqualTo(MotivoNoElegible.DESACTIVADA));
        assertThat(rol(id)).isEqualTo(1);
        assertThat(cuenta("gestor", id)).isZero();
    }

    /**
     * DEC-106: el login carga la entidad, la modifica y la guarda. Si se cargó antes de un cambio de rol, guardarla no
     * debe devolver el rol anterior: {@code id_rol} no es actualizable desde JPA.
     */
    @Test
    void guardarUnUsuarioCargadoAntesDelCambioDeRolNoRevierteElRol() {
        UUID id = crear(1, true);
        UsuarioEntity cargadaAntes = transaccion.execute(estado -> usuarios.findById(id).orElseThrow());

        transaccion.executeWithoutResult(estado -> adaptador.aplicar(new CambioRol(id, Rol.Turista, Rol.Gestor, 0)));
        cargadaAntes.setIntentosFallidos(2);
        transaccion.executeWithoutResult(estado -> usuarios.save(cargadaAntes));

        assertThat(rol(id)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT intentos_fallidos FROM usuario WHERE id_usuario = ?", Integer.class, id))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT version_cuenta FROM usuario WHERE id_usuario = ?", Long.class, id))
                .isEqualTo(1);
        assertThat(adaptador.buscarCuenta(id)).map(CuentaConRol::getRol).contains(Rol.Gestor);
    }

    @Test
    void elResumenTraeElNombreDelTuristaYSiEsRevocable() {
        UUID conTurista = crear(3, true);
        UUID sinTurista = crear(4, false);

        assertThat(adaptador.resumen(conTurista)).hasValueSatisfying(r -> {
            assertThat(r.nombre()).isEqualTo("Nombre Apellido");
            assertThat(r.revocable()).isTrue();
            assertThat(r.rol()).isEqualTo(Rol.Gestor);
        });
        assertThat(adaptador.resumen(sinTurista)).hasValueSatisfying(r -> {
            assertThat(r.nombre()).isNull();
            assertThat(r.revocable()).isFalse();
        });
    }

    private UUID crear(int idRol, boolean conTurista) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO usuario (id_usuario, id_rol, correo, contrasena_hash, fecha_registro,"
                + " intentos_fallidos, estado_cuenta) VALUES (?, ?, ?, 'x', CURRENT_DATE, 0, 'Activo')",
                id, idRol, "cr-" + id + "@mail.test");
        if (idRol == 3 || idRol == 4) {
            jdbc.update("INSERT INTO " + (idRol == 3 ? "gestor" : "administrador") + " (id_usuario) VALUES (?)", id);
        }
        if (conTurista) {
            jdbc.update("INSERT INTO turista (id_usuario, primer_nombre, primer_apellido, pais_origen,"
                    + " acepto_politicas) VALUES (?, 'Nombre', 'Apellido', 'CO', TRUE)", id);
        }
        return id;
    }

    private int rol(UUID id) {
        Integer rol = jdbc.queryForObject("SELECT id_rol FROM usuario WHERE id_usuario = ?", Integer.class, id);
        return rol == null ? -1 : rol;
    }

    private int cuenta(String tabla, UUID id) {
        Integer filas = jdbc.queryForObject("SELECT count(*) FROM " + tabla + " WHERE id_usuario = ?", Integer.class,
                id);
        return filas == null ? 0 : filas;
    }
}
