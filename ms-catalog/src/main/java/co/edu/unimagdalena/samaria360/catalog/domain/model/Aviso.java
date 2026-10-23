package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Aviso general de la colección {@code avisos} (RF40, DEC-58): mensaje de 1 a 300 caracteres sin los espacios de los
 * extremos, tipo y estado. Se publica activo y solo cambia una vez, al despublicarlo: no tiene vigencia automática, ni
 * se edita, ni se borra (DEC-64). Mientras está activo lo ven todos los usuarios.
 */
public record Aviso(String id, String mensaje, TipoAviso tipo, boolean activo, Instant fechaPublicacion) {

    public static final int MENSAJE_MAXIMO = 300;

    public Aviso {
        if (!IdentificadorObjeto.esValido(id)) {
            throw new IllegalArgumentException("id no es un ObjectId: " + id);
        }
        TextoAcotado.exigir(mensaje, MENSAJE_MAXIMO, "mensaje");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(fechaPublicacion, "fechaPublicacion");
    }

    /**
     * Un aviso nuevo, activo, con el mensaje limpio.
     *
     * @throws ContenidoInvalidoException con un motivo por cada campo inválido, con los nombres JSON
     */
    public static Aviso publicar(String id, String mensaje, String tipo, Instant fechaPublicacion) {
        Map<String, MotivoInvalido> campos = new LinkedHashMap<>();
        TextoAcotado.validar(mensaje, MENSAJE_MAXIMO).ifPresent(motivo -> campos.put("mensaje", motivo));
        TipoAviso tipoAviso = null;
        if (tipo == null || tipo.isBlank()) {
            campos.put("tipo", MotivoInvalido.REQUERIDO);
        } else {
            try {
                tipoAviso = TipoAviso.valueOf(tipo);
            } catch (IllegalArgumentException e) {
                campos.put("tipo", MotivoInvalido.NO_PERMITIDO);
            }
        }
        if (!campos.isEmpty()) {
            throw new ContenidoInvalidoException(campos);
        }
        return new Aviso(id, TextoAcotado.limpiar(mensaje), tipoAviso, true, fechaPublicacion);
    }
}
