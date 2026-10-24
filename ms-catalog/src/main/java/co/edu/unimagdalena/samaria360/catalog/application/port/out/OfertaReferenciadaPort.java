package co.edu.unimagdalena.samaria360.catalog.application.port.out;

import co.edu.unimagdalena.samaria360.catalog.domain.model.TipoOferta;

import java.util.Collection;
import java.util.Map;

/** Servicios y atractivos que referencian los favoritos (DEC-86): se leen al listar, sin desnormalizar. */
public interface OfertaReferenciadaPort {

    /**
     * Los que existen, por id; los que no existen no aparecen en el mapa.
     *
     * @param ids ObjectId en texto
     */
    Map<String, Referencia> buscar(TipoOferta tipo, Collection<String> ids);

    /**
     * @param imagen  la primera de {@code imagenes}, o null
     * @param visible un servicio Publicado de un prestador activo (RF16, RF22); un atractivo, siempre (no tiene estado
     *                de publicación)
     */
    record Referencia(String nombre, String categoria, String imagen, boolean visible) {
    }
}
