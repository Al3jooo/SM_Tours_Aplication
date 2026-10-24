package co.edu.unimagdalena.samaria360.auth.application.fake;

import co.edu.unimagdalena.samaria360.auth.application.port.in.FiltroCuentas;
import co.edu.unimagdalena.samaria360.auth.application.port.in.FiltroPersonal;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PaginaCuentas;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PersonalResumen;
import co.edu.unimagdalena.samaria360.auth.application.port.out.CambioRolRepositoryPort;
import co.edu.unimagdalena.samaria360.auth.domain.exception.RolCambiadoException;
import co.edu.unimagdalena.samaria360.auth.domain.model.CambioRol;
import co.edu.unimagdalena.samaria360.auth.domain.model.CuentaConRol;
import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoCuenta;
import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Cuentas en memoria con el UPDATE condicional del adaptador: solo aplica si el rol y la versión coinciden. */
public class CambioRolRepositoryEnMemoria implements CambioRolRepositoryPort {

    private final Map<UUID, CuentaConRol> cuentas = new LinkedHashMap<>();
    private final List<String> llamadas = new ArrayList<>();
    private FiltroPersonal ultimoFiltro;

    public void agregar(CuentaConRol cuenta) {
        cuentas.put(cuenta.getId(), cuenta);
    }

    public CuentaConRol persistida(UUID id) {
        return cuentas.get(id);
    }

    /** Orden de las operaciones: «buscar», «bloquear», «aplicar» y «resumen». */
    public List<String> llamadas() {
        return llamadas;
    }

    public FiltroPersonal ultimoFiltro() {
        return ultimoFiltro;
    }

    @Override
    public Optional<CuentaConRol> buscarCuenta(UUID idUsuario) {
        llamadas.add("buscar");
        return Optional.ofNullable(cuentas.get(idUsuario));
    }

    @Override
    public List<UUID> bloquearAdministradoresActivos() {
        llamadas.add("bloquear");
        return cuentas.values().stream()
                .filter(c -> c.getRol() == Rol.Administrador && c.getCuenta() == EstadoCuenta.Activo)
                .map(CuentaConRol::getId).toList();
    }

    @Override
    public void aplicar(CambioRol cambio) {
        llamadas.add("aplicar");
        CuentaConRol actual = cuentas.get(cambio.idUsuario());
        if (actual.getRol() != cambio.rolAnterior() || actual.getVersion() != cambio.versionEsperada()) {
            throw new RolCambiadoException(actual.getRol());
        }
        cuentas.put(cambio.idUsuario(), new CuentaConRol(cambio.idUsuario(), cambio.rolNuevo(), actual.getCuenta(),
                actual.tieneRolBase(), cambio.versionNueva()));
    }

    @Override
    public Optional<PersonalResumen> resumen(UUID idUsuario) {
        llamadas.add("resumen");
        return Optional.ofNullable(cuentas.get(idUsuario)).map(c -> new PersonalResumen(c.getId(),
                c.tieneRolBase() ? "Marta Lee" : null, "marta@mail.test", c.getRol(), LocalDate.of(2026, 10, 18),
                c.tieneRolBase()));
    }

    @Override
    public PaginaCuentas<PersonalResumen> listarPersonal(FiltroPersonal filtro) {
        ultimoFiltro = filtro;
        return new PaginaCuentas<>(List.of(), filtro.pagina(), FiltroCuentas.TAMANO_PAGINA, 0);
    }
}
