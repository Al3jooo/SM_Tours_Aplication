package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Servicios incluidos de un alojamiento (RF10: WiFi, desayuno, piscina; WF-08 agrega parqueadero). Vocabulario
 * cerrado: el validador de MongoDB lo restringe con el mismo {@code enum}.
 */
public enum ServicioIncluido {
    WiFi,
    Desayuno,
    Piscina,
    Parqueadero;

    /** Longitud máxima con la que un valor recibido se repite en un mensaje de error. */
    static final int LONGITUD_MAXIMA_EN_MENSAJE = 40;

    /** Lee el nombre sin distinguir mayúsculas ({@code wifi} → {@code WiFi}). */
    public static ServicioIncluido desdeTexto(String texto) {
        String limpio = texto == null ? "" : texto.trim();
        for (ServicioIncluido servicio : values()) {
            if (servicio.name().equalsIgnoreCase(limpio)) {
                return servicio;
            }
        }
        throw new IllegalArgumentException("serviciosIncluidos no admite «" + recortar(limpio)
                + "»; valores aceptados: " + aceptados());
    }

    /** Lista de valores aceptados, para los mensajes de error. */
    public static String aceptados() {
        return Arrays.stream(values()).map(Enum::name).collect(Collectors.joining(", "));
    }

    /** El valor viene del usuario: se recorta (por caracteres, sin partir un emoji) antes de repetirlo. */
    static String recortar(String valor) {
        if (valor.codePointCount(0, valor.length()) <= LONGITUD_MAXIMA_EN_MENSAJE) {
            return valor;
        }
        return valor.substring(0, valor.offsetByCodePoints(0, LONGITUD_MAXIMA_EN_MENSAJE)) + "…";
    }
}
