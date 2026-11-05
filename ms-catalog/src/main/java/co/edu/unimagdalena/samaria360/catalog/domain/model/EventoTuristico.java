package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Evento de la agenda turística (RF25; clase {@code EventoTuristico} del Entregable 2, §2.1). Las fechas son días
 * (DEC-215): el evento ocurre de {@code fechaInicio} a {@code fechaFin}, los dos incluidos, en America/Bogota. Se
 * publica al registrarse y no se edita ni se borra (DEC-215), por eso no lleva versión (DEC-241).
 */
public record EventoTuristico(String id, UUID gestorId, String nombre, LocalDate fechaInicio, LocalDate fechaFin,
                              String ubicacion, String descripcion, Instant fechaRegistro) {

    /** Zona de las fechas de día y de «hoy» en la agenda (DEC-215). */
    public static final ZoneId ZONA = ZoneId.of("America/Bogota");
    public static final int NOMBRE_MAXIMO = 120;
    public static final int UBICACION_MAXIMA = 200;
    public static final int DESCRIPCION_MAXIMA = 1000;

    /** {@code yyyy-MM-dd} con año de cuatro cifras; el resto (p. ej. 2026-02-30) lo rechaza {@link LocalDate#parse}. */
    private static final Pattern FECHA = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");

    public EventoTuristico {
        if (!IdentificadorObjeto.esValido(id)) {
            throw new IllegalArgumentException("id no es un ObjectId: " + id);
        }
        Objects.requireNonNull(gestorId, "gestorId");
        TextoAcotado.exigir(nombre, NOMBRE_MAXIMO, "nombre");
        Objects.requireNonNull(fechaInicio, "fechaInicio");
        Objects.requireNonNull(fechaFin, "fechaFin");
        if (fechaFin.isBefore(fechaInicio)) {
            throw new IllegalArgumentException("fechaFin es anterior a fechaInicio");
        }
        TextoAcotado.exigir(ubicacion, UBICACION_MAXIMA, "ubicacion");
        TextoAcotado.exigir(descripcion, DESCRIPCION_MAXIMA, "descripcion");
        Objects.requireNonNull(fechaRegistro, "fechaRegistro");
    }

    /** El día de hoy en Bogotá para un instante (DEC-215). */
    public static LocalDate hoy(Instant ahora) {
        return LocalDate.ofInstant(ahora, ZONA);
    }

    /** Un evento está en la agenda hasta su último día incluido (DEC-215). */
    public boolean vigenteEl(LocalDate dia) {
        return !fechaFin.isBefore(dia);
    }

    /**
     * Un evento nuevo, con los textos limpios. {@code fechaFin} anterior a {@code fechaInicio} (DEC-237) o a hoy en
     * Bogotá (DEC-242; decisión del equipo, no del SRS) es {@code NO_PERMITIDO}.
     *
     * @throws ContenidoInvalidoException con un motivo por cada campo inválido, con los nombres JSON
     */
    public static EventoTuristico registrar(String id, UUID gestorId, BorradorEvento borrador, Instant fechaRegistro) {
        BorradorEvento datos = borrador == null ? new BorradorEvento(null, null, null, null, null) : borrador;
        Map<String, MotivoInvalido> campos = new LinkedHashMap<>();
        TextoAcotado.validar(datos.nombre(), NOMBRE_MAXIMO).ifPresent(motivo -> campos.put("nombre", motivo));
        LocalDate inicio = fecha(datos.fechaInicio(), "fechaInicio", campos);
        LocalDate fin = fecha(datos.fechaFin(), "fechaFin", campos);
        if (fin != null && ((inicio != null && fin.isBefore(inicio)) || fin.isBefore(hoy(fechaRegistro)))) {
            campos.put("fechaFin", MotivoInvalido.NO_PERMITIDO);
        }
        TextoAcotado.validar(datos.ubicacion(), UBICACION_MAXIMA).ifPresent(motivo -> campos.put("ubicacion", motivo));
        TextoAcotado.validar(datos.descripcion(), DESCRIPCION_MAXIMA)
                .ifPresent(motivo -> campos.put("descripcion", motivo));
        if (!campos.isEmpty()) {
            throw new ContenidoInvalidoException(campos);
        }
        return new EventoTuristico(id, gestorId, TextoAcotado.limpiar(datos.nombre()), inicio, fin,
                TextoAcotado.limpiar(datos.ubicacion()), TextoAcotado.limpiar(datos.descripcion()), fechaRegistro);
    }

    private static LocalDate fecha(String texto, String campo, Map<String, MotivoInvalido> campos) {
        String limpio = TextoAcotado.limpiar(texto);
        if (limpio.isEmpty()) {
            campos.put(campo, MotivoInvalido.REQUERIDO);
            return null;
        }
        if (!FECHA.matcher(limpio).matches()) {
            campos.put(campo, MotivoInvalido.FORMATO);
            return null;
        }
        try {
            return LocalDate.parse(limpio);
        } catch (DateTimeParseException e) {
            campos.put(campo, MotivoInvalido.FORMATO);
            return null;
        }
    }
}
