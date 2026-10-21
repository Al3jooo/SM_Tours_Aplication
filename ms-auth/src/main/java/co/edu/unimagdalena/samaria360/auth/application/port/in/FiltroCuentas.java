package co.edu.unimagdalena.samaria360.auth.application.port.in;

import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoAdministrativo;

/** Filtros de los listados: estado y texto ya validados (ver {@code ConsultarCuentasService}). Ambos pueden ser nulos. */
public record FiltroCuentas(EstadoAdministrativo estado, String texto, int pagina) {

    /** DEC-41: el mismo tamaño que la búsqueda pública (RF16). */
    public static final int TAMANO_PAGINA = 10;

    public int desplazamiento() {
        return pagina * TAMANO_PAGINA;
    }
}
