package co.edu.unimagdalena.samaria360.booking.application.port.out;

import co.edu.unimagdalena.samaria360.booking.domain.model.CategoriaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.OfertaReservable;

import java.util.Optional;

/**
 * Consulta síncrona al endpoint público de ms-catalog (D4, ADR-007). Se llama siempre <b>antes</b> de abrir una
 * transacción: nunca hay filas bloqueadas durante una llamada HTTP.
 */
public interface CatalogoPort {

    /**
     * @return vacío si el servicio no existe o no es visible (404 del catálogo)
     * @throws CatalogoNoDisponibleException si ms-catalog no responde o el circuito está abierto
     */
    Optional<OfertaReservable> buscarReservable(String idServicio);

    /** Página {@code pagina} (desde 0) de los servicios publicados de la categoría. Para el generador (D5). */
    PaginaCatalogo listarPublicados(CategoriaReservable categoria, int pagina);
}
