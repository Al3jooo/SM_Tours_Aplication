package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.EstadoCuenta;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Revision;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Un servicio en cualquier estado, para su dueño («Mis servicios», DEC-143) y para el Administrador que lo revisa
 * (DEC-144). Además de los datos públicos de {@link RespuestaServicio}, trae las coordenadas, el estado de publicación,
 * la copia del estado del prestador, la fecha de registro y la revisión.
 *
 * @param coordenadas           el pin del mapa (DEC-132)
 * @param motivoRechazo         la justificación del rechazo (RF21); null si no está Rechazado
 * @param fechaRevision         cuándo se aprobó o rechazó; null mientras está Pendiente
 * @param revisadoPor           solo en la vista del Administrador: quién decidió (DEC-145)
 * @param estadoCuentaPrestador solo en la vista del Administrador: el estado de la cuenta según la proyección
 *                              {@code cuentas} (DEC-144); se omite si no hay fila
 */
public record RespuestaServicioGestion(String id, String idPrestador, String nombreComercial, String categoria,
                                       String descripcion, String direccion, Coordenadas coordenadas,
                                       RespuestaServicio.Contacto contacto, List<String> condicionesAccesibilidad,
                                       String horarioAtencion, BigDecimal precioBase, String moneda,
                                       Object detalleCategoria, RespuestaServicio.Oferta oferta, List<String> imagenes,
                                       String estadoPublicacion, boolean prestadorActivo, String rnt,
                                       Instant fechaRegistro, String motivoRechazo, Instant fechaRevision,
                                       @JsonInclude(JsonInclude.Include.NON_NULL) String revisadoPor,
                                       @JsonInclude(JsonInclude.Include.NON_NULL) String estadoCuentaPrestador) {

    public record Coordenadas(double longitud, double latitud) {
    }

    public static RespuestaServicioGestion paraPrestador(ServicioComercial servicio) {
        return de(servicio, false, Optional.empty());
    }

    public static RespuestaServicioGestion paraAdministrador(ServicioComercial servicio,
                                                             Optional<EstadoCuenta> estadoCuentaPrestador) {
        return de(servicio, true, estadoCuentaPrestador);
    }

    private static RespuestaServicioGestion de(ServicioComercial servicio, boolean administrador,
                                               Optional<EstadoCuenta> estadoCuenta) {
        Optional<Revision> revision = servicio.getRevision();
        return new RespuestaServicioGestion(servicio.getId().orElseThrow(), servicio.getPrestadorId().toString(),
                servicio.getNombreComercial(), servicio.getCategoria().name(), servicio.getDescripcion().orElse(null),
                servicio.getDireccion().orElse(null),
                new Coordenadas(servicio.getCoordenada().longitud(), servicio.getCoordenada().latitud()),
                servicio.getContacto()
                        .map(contacto -> new RespuestaServicio.Contacto(contacto.telefono(), contacto.correo()))
                        .orElse(null),
                RespuestaServicio.accesibilidad(servicio), servicio.getHorarioAtencion().orElse(null),
                servicio.getPrecioBase().orElse(null), RespuestaServicio.MONEDA,
                RespuestaServicio.detalle(servicio.getDetalle()),
                servicio.getOferta().map(RespuestaServicio.Oferta::de).orElse(null), servicio.getImagenes(),
                servicio.getEstadoPublicacion().name(), servicio.isPrestadorActivo(), servicio.getRnt().orElse(null),
                servicio.getFechaRegistro().orElse(null), revision.map(Revision::motivoRechazo).orElse(null),
                revision.map(Revision::fechaRevision).orElse(null),
                administrador ? revision.map(r -> r.revisadoPor().toString()).orElse(null) : null,
                administrador ? estadoCuenta.map(Enum::name).orElse(null) : null);
    }
}
