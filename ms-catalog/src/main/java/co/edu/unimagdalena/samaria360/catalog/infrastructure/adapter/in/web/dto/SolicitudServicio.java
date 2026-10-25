package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorServicio;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cuerpo de {@code POST /api/v1/catalogo/prestador/servicios} (RF09, RF10, RF37; DEC-130 a DEC-133; contrato en
 * {@code docs/sad/api/ms-catalog.md}). Lo que fija el servidor no está aquí y, si llega, se ignora (DEC-141):
 * {@code prestadorId}, {@code estadoPublicacion}, {@code prestadorActivo}, {@code rnt}, {@code idUnidad} y el
 * {@code precioBase} de un alojamiento (se acepta, pero el dominio lo deriva de las unidades).
 */
public record SolicitudServicio(String nombreComercial, String categoria, String descripcion, String direccion,
                                Coordenadas coordenadas, Contacto contacto, List<String> condicionesAccesibilidad,
                                String horarioAtencion, BigDecimal precioBase, Detalle detalleCategoria,
                                Oferta oferta) {

    public record Coordenadas(Double longitud, Double latitud) {
    }

    public record Contacto(String telefono, String correo) {
    }

    public record Unidad(String nombre, String tipoHabitacion, Integer numHabitaciones, Integer capacidadMaxHuespedes,
                         BigDecimal precioNoche) {
    }

    /** Los campos de todas las categorías: solo se leen los de {@code categoria}. */
    public record Detalle(List<Unidad> unidades, List<String> serviciosIncluidos, String tipoCocina,
                          Integer aforoMaxTurno, Boolean menuVegetariano, String rangoPrecioPlato,
                          String puntoEncuentro, BigDecimal duracionHoras, String nivelDificultad,
                          Integer aforoMaxGrupo, String tipoVehiculoEquipo, Integer unidadesDisponibles,
                          String condicionesAlquiler) {
    }

    public record Oferta(List<String> diasOperacion, List<String> salidas) {
    }

    public BorradorServicio aBorrador() {
        return new BorradorServicio(nombreComercial, categoria, descripcion, direccion,
                coordenadas == null ? null
                        : new BorradorServicio.DatosCoordenadas(coordenadas.longitud(), coordenadas.latitud()),
                contacto == null ? null : new BorradorServicio.DatosContacto(contacto.telefono(), contacto.correo()),
                condicionesAccesibilidad, horarioAtencion, precioBase, detalle(),
                oferta == null ? null : new BorradorServicio.DatosOferta(oferta.diasOperacion(), oferta.salidas()));
    }

    private BorradorServicio.DatosDetalle detalle() {
        if (detalleCategoria == null) {
            return null;
        }
        Detalle d = detalleCategoria;
        List<BorradorServicio.DatosUnidad> unidades = d.unidades() == null ? null : d.unidades().stream()
                .map(u -> u == null ? null : new BorradorServicio.DatosUnidad(u.nombre(), u.tipoHabitacion(),
                        u.numHabitaciones(), u.capacidadMaxHuespedes(), u.precioNoche()))
                .toList();
        return new BorradorServicio.DatosDetalle(unidades, d.serviciosIncluidos(), d.tipoCocina(), d.aforoMaxTurno(),
                d.menuVegetariano(), d.rangoPrecioPlato(), d.puntoEncuentro(), d.duracionHoras(), d.nivelDificultad(),
                d.aforoMaxGrupo(), d.tipoVehiculoEquipo(), d.unidadesDisponibles(), d.condicionesAlquiler());
    }
}
