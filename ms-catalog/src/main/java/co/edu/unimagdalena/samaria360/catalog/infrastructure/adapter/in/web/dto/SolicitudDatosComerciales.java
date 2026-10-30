package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorServicio.DatosContacto;

/**
 * Cuerpo de {@code PUT /api/v1/catalogo/prestador/servicios/{id}/datos-comerciales} (RF43; DEC-188, DEC-189): el
 * nombre comercial, el contacto de atención al público y la {@code version} leída (DEC-186). Cualquier otro campo se
 * ignora (DEC-141).
 */
public record SolicitudDatosComerciales(String nombreComercial, SolicitudServicio.Contacto contacto,
                                        Integer version) {

    public DatosContacto datosContacto() {
        return contacto == null ? null : new DatosContacto(contacto.telefono(), contacto.correo());
    }
}
