package co.edu.unimagdalena.samaria360.auth.application.port.in;

import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;

/**
 * Filtros del listado del personal, ya validados (ver {@code ConsultarPersonalService}): {@code rol} es Gestor,
 * Administrador o nulo (los dos); {@code texto} puede ser nulo. Páginas de 10, como los demás listados (DEC-41).
 */
public record FiltroPersonal(Rol rol, String texto, int pagina) {

    public int desplazamiento() {
        return pagina * FiltroCuentas.TAMANO_PAGINA;
    }
}
