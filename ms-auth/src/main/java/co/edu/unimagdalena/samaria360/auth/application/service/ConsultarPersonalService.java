package co.edu.unimagdalena.samaria360.auth.application.service;

import co.edu.unimagdalena.samaria360.auth.application.port.in.ConsultarPersonalUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.in.FiltroPersonal;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PaginaCuentas;
import co.edu.unimagdalena.samaria360.auth.application.port.in.PersonalResumen;
import co.edu.unimagdalena.samaria360.auth.application.port.out.CambioRolRepositoryPort;
import co.edu.unimagdalena.samaria360.auth.domain.exception.DatosRegistroInvalidosException;
import co.edu.unimagdalena.samaria360.auth.domain.model.MotivoInvalido;
import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Listado del personal (RF45, DEC-103): páginas de 10, por fecha de registro, con las reglas de los demás listados. */
public class ConsultarPersonalService implements ConsultarPersonalUseCase {

    private static final Set<Rol> ROLES = Set.of(Rol.Gestor, Rol.Administrador);

    private final CambioRolRepositoryPort cuentas;

    public ConsultarPersonalService(CambioRolRepositoryPort cuentas) {
        this.cuentas = Objects.requireNonNull(cuentas);
    }

    @Override
    public PaginaCuentas<PersonalResumen> personal(String rol, String texto, int pagina) {
        Map<String, MotivoInvalido> campos = new LinkedHashMap<>();
        Rol rolFiltro = null;
        if (rol != null && !rol.isBlank()) {
            rolFiltro = ROLES.stream().filter(r -> r.name().equals(rol)).findFirst().orElse(null);
            if (rolFiltro == null) {
                campos.put("rol", MotivoInvalido.NO_PERMITIDO);
            }
        }
        String textoFiltro = texto == null || texto.isBlank() ? null : texto.strip();
        if (textoFiltro != null && textoFiltro.length() > ConsultarCuentasService.TEXTO_MAXIMO) {
            campos.put("q", MotivoInvalido.LONGITUD_MAXIMA);
        }
        if (pagina < 0) {
            campos.put("pagina", MotivoInvalido.FORMATO);
        }
        if (!campos.isEmpty()) {
            throw new DatosRegistroInvalidosException(campos);
        }
        return cuentas.listarPersonal(new FiltroPersonal(rolFiltro, textoFiltro, pagina));
    }
}
