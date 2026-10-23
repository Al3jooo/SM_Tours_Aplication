package co.edu.unimagdalena.samaria360.catalog.application.port.in;

import co.edu.unimagdalena.samaria360.catalog.domain.model.Aviso;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;

import java.util.List;

/**
 * CU «Crear y publicar aviso general» (RF40) y su CU secundario «Consultar avisos generales». Los activos son públicos;
 * el listado completo, publicar y despublicar son del Administrador (la autorización la aplica la infraestructura).
 */
public interface GestionarAvisosUseCase {

    int TAMANO_PAGINA = 10;

    /** Los avisos activos, del más reciente al más antiguo: todos los muestra el banner (DEC-58). */
    List<Aviso> activos();

    /** Todos los avisos, activos o no, del más reciente al más antiguo, en páginas de 10 desde 0 (DEC-41). */
    PaginaResultados<Aviso> pagina(int pagina);

    Aviso publicar(String mensaje, String tipo);

    /** Activo → inactivo. Un aviso ya inactivo es un 409 (DEC-64). */
    Aviso despublicar(String id);
}
