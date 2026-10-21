package co.edu.unimagdalena.samaria360.auth.application.fake;

import co.edu.unimagdalena.samaria360.auth.application.port.in.FiltroCuentas;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PaginaCuentas;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PrestadorDetalle;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PrestadorResumen;
import co.edu.unimagdalena.samaria360.auth.application.port.in.TuristaResumen;
import co.edu.unimagdalena.samaria360.auth.application.port.out.CuentasAdminRepositoryPort;
import co.edu.unimagdalena.samaria360.auth.domain.exception.EstadoCambiadoException;
import co.edu.unimagdalena.samaria360.auth.domain.model.CambioEstadoCuenta;
import co.edu.unimagdalena.samaria360.auth.domain.model.CuentaAdministrada;
import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoAdministrativo;
import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Cuentas en memoria con el mismo UPDATE condicional que la base: solo aplica si la versión y el estado coinciden. */
public class CuentasAdminEnMemoria implements CuentasAdminRepositoryPort {

    private final Map<UUID, CuentaAdministrada> cuentas = new HashMap<>();
    private final Map<UUID, String> motivos = new HashMap<>();
    private final Map<UUID, UUID> revisadoPor = new HashMap<>();
    private FiltroCuentas ultimoFiltro;

    public void agregar(CuentaAdministrada cuenta) {
        cuentas.put(cuenta.getId(), cuenta);
    }

    public CuentaAdministrada persistida(UUID id) {
        return cuentas.get(id);
    }

    public String motivo(UUID id) {
        return motivos.get(id);
    }

    public UUID revisadoPor(UUID id) {
        return revisadoPor.get(id);
    }

    public FiltroCuentas ultimoFiltro() {
        return ultimoFiltro;
    }

    @Override
    public Optional<CuentaAdministrada> buscarCuenta(UUID idUsuario) {
        return Optional.ofNullable(cuentas.get(idUsuario));
    }

    @Override
    public void aplicar(CambioEstadoCuenta cambio, UUID revisor, Instant ahora) {
        CuentaAdministrada actual = cuentas.get(cambio.idUsuario());
        if (actual == null || actual.getVersion() != cambio.versionEsperada()
                || actual.getCuenta() != cambio.cuentaEsperada()) {
            throw new EstadoCambiadoException(actual == null ? EstadoAdministrativo.Pendiente : actual.estado());
        }
        cuentas.put(cambio.idUsuario(), new CuentaAdministrada(cambio.idUsuario(), cambio.rol(),
                cambio.cuentaNueva(), cambio.validacionNueva(), cambio.versionNueva()));
        if (cambio.cambiaValidacion()) {
            motivos.put(cambio.idUsuario(), cambio.motivoRechazo());
            revisadoPor.put(cambio.idUsuario(), revisor);
        }
    }

    @Override
    public PaginaCuentas<PrestadorResumen> listarPrestadores(FiltroCuentas filtro) {
        ultimoFiltro = filtro;
        return new PaginaCuentas<>(List.of(), filtro.pagina(), FiltroCuentas.TAMANO_PAGINA, 0);
    }

    @Override
    public PaginaCuentas<TuristaResumen> listarTuristas(FiltroCuentas filtro) {
        ultimoFiltro = filtro;
        return new PaginaCuentas<>(List.of(), filtro.pagina(), FiltroCuentas.TAMANO_PAGINA, 0);
    }

    @Override
    public Optional<PrestadorDetalle> detallePrestador(UUID idPrestador) {
        return buscarCuenta(idPrestador).filter(c -> c.getRol() == Rol.Prestador)
                .map(c -> new PrestadorDetalle(c.getId(), "Ana Pérez", null, "RNT-1", "ana@hotel.test", "+57 300",
                        true, LocalDate.of(2026, 10, 18), c.estado(), motivos.get(c.getId()), null));
    }

    @Override
    public Optional<TuristaResumen> detalleTurista(UUID idTurista) {
        return buscarCuenta(idTurista).filter(c -> c.getRol() == Rol.Turista)
                .map(c -> new TuristaResumen(c.getId(), "Marta Lee", "marta@mail.test", "US",
                        LocalDate.of(2026, 10, 18), c.estado()));
    }
}
