package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Pregunta frecuente de la colección {@code faq} (RF26): un título y un cuerpo de texto, en el idioma en que los escribe
 * el Administrador (DEC-69). Los dos se guardan sin los espacios de los extremos: el título de 1 a 150 caracteres y el
 * cuerpo de 1 a 2000 (DEC-61). {@code fechaActualizacion} es la de la creación o la de la última edición.
 */
public record PreguntaFrecuente(String id, String titulo, String cuerpo, Instant fechaActualizacion) {

    public static final int TITULO_MAXIMO = 150;
    public static final int CUERPO_MAXIMO = 2000;
    /** Tope de la colección (DEC-62): el listado público va completo y el filtro de RF47 se hace en el cliente. */
    public static final int MAXIMO_PREGUNTAS = 200;

    public PreguntaFrecuente {
        if (!IdentificadorObjeto.esValido(id)) {
            throw new IllegalArgumentException("id no es un ObjectId: " + id);
        }
        TextoAcotado.exigir(titulo, TITULO_MAXIMO, "titulo");
        TextoAcotado.exigir(cuerpo, CUERPO_MAXIMO, "cuerpo");
        Objects.requireNonNull(fechaActualizacion, "fechaActualizacion");
    }

    /**
     * Valida los dos campos a la vez y los guarda limpios.
     *
     * @throws ContenidoInvalidoException con un motivo por cada campo inválido, con los nombres JSON
     */
    public static PreguntaFrecuente de(String id, String titulo, String cuerpo, Instant fechaActualizacion) {
        validar(titulo, cuerpo);
        return new PreguntaFrecuente(id, TextoAcotado.limpiar(titulo), TextoAcotado.limpiar(cuerpo),
                fechaActualizacion);
    }

    /** @throws ContenidoInvalidoException con un motivo por cada campo inválido */
    public static void validar(String titulo, String cuerpo) {
        Map<String, MotivoInvalido> campos = new LinkedHashMap<>();
        TextoAcotado.validar(titulo, TITULO_MAXIMO).ifPresent(motivo -> campos.put("titulo", motivo));
        TextoAcotado.validar(cuerpo, CUERPO_MAXIMO).ifPresent(motivo -> campos.put("cuerpo", motivo));
        if (!campos.isEmpty()) {
            throw new ContenidoInvalidoException(campos);
        }
    }
}
