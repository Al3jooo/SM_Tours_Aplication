package co.edu.unimagdalena.samaria360.auth.application.service;

import co.edu.unimagdalena.samaria360.auth.application.port.in.ConsultarCuentasUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.in.FiltroCuentas;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PaginaCuentas;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PrestadorDetalle;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PrestadorResumen;
import co.edu.unimagdalena.samaria360.auth.application.port.in.TuristaResumen;
import co.edu.unimagdalena.samaria360.auth.application.port.out.CuentasAdminRepositoryPort;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaNoEncontradaException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.DatosRegistroInvalidosException;
import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoAdministrativo;
import co.edu.unimagdalena.samaria360.auth.domain.model.MotivoInvalido;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Listados y detalle del Administrador (RF28): páginas de 10 (DEC-41), por fecha de registro. */
public class ConsultarCuentasService implements ConsultarCuentasUseCase {

    static final int TEXTO_MAXIMO = 100;
    private static final Set<EstadoAdministrativo> ESTADOS_PRESTADOR = Set.of(EstadoAdministrativo.Pendiente,
            EstadoAdministrativo.Activo, EstadoAdministrativo.Rechazado, EstadoAdministrativo.Inactivo);
    private static final Set<EstadoAdministrativo> ESTADOS_TURISTA = Set.of(EstadoAdministrativo.Activo,
            EstadoAdministrativo.Desactivado);

    private final CuentasAdminRepositoryPort cuentas;

    public ConsultarCuentasService(CuentasAdminRepositoryPort cuentas) {
        this.cuentas = Objects.requireNonNull(cuentas);
    }

    @Override
    public PaginaCuentas<PrestadorResumen> prestadores(String estado, String texto, int pagina) {
        return cuentas.listarPrestadores(filtro(estado, texto, pagina, ESTADOS_PRESTADOR));
    }

    @Override
    public PrestadorDetalle prestador(UUID idPrestador) {
        return cuentas.detallePrestador(idPrestador).orElseThrow(() -> new CuentaNoEncontradaException(idPrestador));
    }

    @Override
    public PaginaCuentas<TuristaResumen> turistas(String estado, String texto, int pagina) {
        return cuentas.listarTuristas(filtro(estado, texto, pagina, ESTADOS_TURISTA));
    }

    private static FiltroCuentas filtro(String estado, String texto, int pagina, Set<EstadoAdministrativo> permitidos) {
        Map<String, MotivoInvalido> campos = new LinkedHashMap<>();
        EstadoAdministrativo estadoFiltro = null;
        if (estado != null && !estado.isBlank()) {
            estadoFiltro = permitidos.stream().filter(e -> e.name().equals(estado)).findFirst().orElse(null);
            if (estadoFiltro == null) {
                campos.put("estado", MotivoInvalido.NO_PERMITIDO);
            }
        }
        String textoFiltro = texto == null || texto.isBlank() ? null : texto.strip();
        if (textoFiltro != null && textoFiltro.length() > TEXTO_MAXIMO) {
            campos.put("q", MotivoInvalido.LONGITUD_MAXIMA);
        }
        if (pagina < 0) {
            campos.put("pagina", MotivoInvalido.FORMATO);
        }
        if (!campos.isEmpty()) {
            throw new DatosRegistroInvalidosException(campos);
        }
        return new FiltroCuentas(estadoFiltro, textoFiltro, pagina);
    }
}
