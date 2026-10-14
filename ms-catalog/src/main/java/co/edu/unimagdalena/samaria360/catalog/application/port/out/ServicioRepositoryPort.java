package co.edu.unimagdalena.samaria360.catalog.application.port.out;

import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;

import java.util.Optional;

/** Lectura de un servicio por su id. */
public interface ServicioRepositoryPort {

    /**
     * @param id ObjectId en texto (24 hexadecimales en minúscula), ya validado
     * @return el servicio solo si es visible en la búsqueda pública: Publicado y de un prestador activo (RF16, RF22)
     */
    Optional<ServicioComercial> buscarVisible(String id);
}
