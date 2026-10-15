package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

/**
 * Elemento de la lista de resultados de WF-05: un servicio o un atractivo con los datos de su tarjeta y del
 * marcador del mapa (RF23).
 *
 * @param categoria       la categoría del servicio o el texto libre del atractivo
 * @param precio          nulo en los atractivos
 * @param distanciaKm     solo si la búsqueda tiene ubicación (RF38)
 * @param estadoOperativo solo en los atractivos (RF33)
 */
public record ResultadoOferta(TipoOferta tipo, String id, String nombre, String categoria, String descripcionCorta,
                              BigDecimal precio, String imagenPrincipal, Coordenada coordenada, Double distanciaKm,
                              Set<CondicionAccesibilidad> accesibilidad, EstadoOperativo estadoOperativo) {

    public static final int LONGITUD_DESCRIPCION_CORTA = 140;

    public ResultadoOferta {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(id, "id");
        Validaciones.texto(nombre, "nombre");
        Objects.requireNonNull(coordenada, "coordenada");
        accesibilidad = accesibilidad == null ? Set.of() : Set.copyOf(accesibilidad);
        descripcionCorta = acortar(descripcionCorta);
        if (tipo == TipoOferta.Atractivo && precio != null) {
            throw new IllegalArgumentException("Los atractivos no tienen precio");
        }
        if (tipo == TipoOferta.Servicio && estadoOperativo != null) {
            throw new IllegalArgumentException("El estado operativo es solo de los atractivos");
        }
    }

    public static ResultadoOferta deServicio(ServicioComercial servicio, Double distanciaKm) {
        return deServicio(servicio, distanciaKm, servicio.getPrecioBase().orElse(null));
    }

    /** @param precio el precio «desde» que se muestra (con filtros de unidad, el de las unidades que cumplen) */
    public static ResultadoOferta deServicio(ServicioComercial servicio, Double distanciaKm, BigDecimal precio) {
        return new ResultadoOferta(TipoOferta.Servicio, servicio.getId().orElseThrow(), servicio.getNombreComercial(),
                servicio.getCategoria().name(), servicio.getDescripcion().orElse(null),
                precio, servicio.getImagenes().stream().findFirst().orElse(null),
                servicio.getCoordenada(), distanciaKm, servicio.getCondicionesAccesibilidad(), null);
    }

    public static ResultadoOferta deAtractivo(AtractivoPublico atractivo, Double distanciaKm) {
        return new ResultadoOferta(TipoOferta.Atractivo, atractivo.getId().orElseThrow(), atractivo.getNombre(),
                atractivo.getCategoria().orElse(null), atractivo.getDescripcion().orElse(null), null,
                atractivo.getImagenes().stream().findFirst().orElse(null), atractivo.getCoordenada(),
                distanciaKm, atractivo.getCondicionesAccesibilidad(), atractivo.getEstadoOperativo().orElse(null));
    }

    /** Hasta 140 caracteres, cortando en un límite de palabra y terminando en «…». */
    static String acortar(String descripcion) {
        if (descripcion == null || descripcion.length() <= LONGITUD_DESCRIPCION_CORTA) {
            return descripcion;
        }
        String corte = descripcion.substring(0, LONGITUD_DESCRIPCION_CORTA - 1);
        int espacio = corte.lastIndexOf(' ');
        if (espacio > LONGITUD_DESCRIPCION_CORTA / 2) {
            corte = corte.substring(0, espacio);
        }
        return corte.stripTrailing() + "…";
    }
}
