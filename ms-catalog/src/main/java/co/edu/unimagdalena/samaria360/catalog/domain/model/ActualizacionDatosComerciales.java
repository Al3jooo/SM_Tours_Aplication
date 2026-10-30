package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorServicio.DatosContacto;

import java.util.Objects;

/**
 * CU «Actualizar datos comerciales» (RF43; DEC-188, DEC-189), parte del servicio: el nombre comercial (el «nombre del
 * establecimiento» que ve el Turista) y el contacto de atención al público, con las reglas del registro (RF09,
 * DEC-138). Las reservas ya hechas conservan su copia del nombre ({@code reserva.nombre_servicio} en ms-booking).
 */
public final class ActualizacionDatosComerciales {

    private final CamposServicio campos = new CamposServicio();

    private ActualizacionDatosComerciales() {
    }

    /**
     * @param version la versión del servicio que el Prestador leyó (DEC-186)
     * @throws ContenidoInvalidoException con un motivo por cada campo inválido
     */
    public static ServicioEditado aplicar(ServicioComercial servicio, String nombreComercial, DatosContacto contacto,
                                          Integer version) {
        return new ActualizacionDatosComerciales().validar(Objects.requireNonNull(servicio), nombreComercial, contacto,
                version);
    }

    private ServicioEditado validar(ServicioComercial servicio, String nombreComercial, DatosContacto datos,
                                    Integer version) {
        Integer esperada = campos.entero("version", version, 0, Integer.MAX_VALUE);
        String nombre = campos.texto("nombreComercial", nombreComercial, ServicioComercial.LONGITUD_MAXIMA_NOMBRE);
        Contacto contacto = campos.contacto(datos);
        campos.lanzarSiHayErrores();
        return new ServicioEditado(servicio.conDatosComerciales(nombre, contacto), esperada);
    }
}
