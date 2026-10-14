package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Hotel u hostal (RF10): una o más unidades (tipos de habitación, decisión D1) y los servicios incluidos, que son
 * del alojamiento completo. Los servicios incluidos son un vocabulario cerrado ({@link ServicioIncluido}) y se
 * guardan con su nombre canónico. Equivale a la rama Alojamiento del esquema v2 de {@code servicios} (migración
 * M001), incluida la regla de {@code $expr}: {@code idUnidad} no se repite.
 */
public record DetalleAlojamiento(List<UnidadAlojamiento> unidades, List<String> serviciosIncluidos)
        implements DetalleCategoria {

    public DetalleAlojamiento {
        if (unidades == null || unidades.isEmpty()) {
            throw new IllegalArgumentException("Un alojamiento debe tener al menos una unidad");
        }
        unidades = List.copyOf(unidades);
        Set<String> ids = new HashSet<>();
        for (UnidadAlojamiento unidad : unidades) {
            if (!ids.add(unidad.idUnidad())) {
                throw new IllegalArgumentException("idUnidad repetido: " + unidad.idUnidad());
            }
        }
        serviciosIncluidos = Validaciones.lista(serviciosIncluidos).stream()
                .map(servicio -> ServicioIncluido.desdeTexto(servicio).name())
                .distinct()
                .toList();
    }

    /** Precio «desde» (RF37): el menor precio por noche de sus unidades. Es el {@code precioBase} del servicio. */
    public BigDecimal precioDesde() {
        return unidades.stream().map(UnidadAlojamiento::precioNoche).min(Comparator.naturalOrder()).orElseThrow();
    }

    public Optional<UnidadAlojamiento> unidad(String idUnidad) {
        return unidades.stream().filter(unidad -> unidad.idUnidad().equals(idUnidad)).findFirst();
    }

    @Override
    public Categoria categoria() {
        return Categoria.Alojamiento;
    }
}
