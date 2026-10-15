package co.edu.unimagdalena.samaria360.booking.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Lo que la reserva necesita del detalle público de ms-catalog (ADR-007): solo servicios Publicados de prestadores
 * activos llegan hasta aquí.
 *
 * @param categoria     nombre de la categoría en el catálogo (puede no ser reservable)
 * @param precioBase    en un tour, el precio por persona; en un alojamiento, el «desde» (no se usa para cobrar)
 * @param aforoMaxGrupo solo en tours
 * @param duracionHoras solo en tours
 * @param unidades      solo en alojamientos
 */
public record OfertaReservable(String idServicio, UUID idPrestador, String nombre, String categoria,
                               BigDecimal precioBase, Integer aforoMaxGrupo, BigDecimal duracionHoras,
                               List<UnidadReservable> unidades) {

    public OfertaReservable {
        unidades = unidades == null ? List.of() : List.copyOf(unidades);
    }

    public Optional<UnidadReservable> unidad(String idUnidad) {
        return unidades.stream().filter(unidad -> unidad.idUnidad().equals(idUnidad)).findFirst();
    }
}
