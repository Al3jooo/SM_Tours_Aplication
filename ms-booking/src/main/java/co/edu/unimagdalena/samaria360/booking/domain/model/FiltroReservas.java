package co.edu.unimagdalena.samaria360.booking.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.Set;

/**
 * Filtros del CU «Consultar historial» (RF12): rango de fechas de la <b>actividad</b> y estados, para el Turista
 * («Mis reservas») y para el Prestador (las reservas de sus servicios, que además puede filtrar por servicio; DEC-192).
 * <p>
 * Con {@code desde} y {@code hasta} como fechas de Santa Marta, inclusivas, una reserva aparece si su actividad cruza
 * el rango: {@code fecha_inicio < (hasta + 1 día) 00:00} y {@code fecha_fin > desde 00:00}, ambos en
 * {@code America/Bogota}. Una estadía del 30 de noviembre al 2 de diciembre aparece al filtrar diciembre; una que
 * termina exactamente a las 00:00 de {@code desde}, no. Cada extremo es opcional: sin uno, el rango queda abierto.
 *
 * @param estados    vacío = todos
 * @param idServicio solo del Prestador: un servicio suyo (ObjectId en texto); null = todos
 */
public record FiltroReservas(LocalDate desde, LocalDate hasta, Set<EstadoReserva> estados, String idServicio) {

    /** El rango puede abarcar como máximo 366 días, contados con los dos extremos. */
    public static final int MAXIMO_DIAS = 366;

    public FiltroReservas {
        estados = estados == null || estados.isEmpty() ? Set.of() : Set.copyOf(estados);
        if (idServicio != null && !IdentificadorObjeto.esValido(idServicio)) {
            throw new IllegalArgumentException("idServicio debe ser un ObjectId en texto");
        }
    }

    /** Sin filtro de servicio: el del Turista. */
    public FiltroReservas(LocalDate desde, LocalDate hasta, Set<EstadoReserva> estados) {
        this(desde, hasta, estados, null);
    }

    public static FiltroReservas sinFiltros() {
        return new FiltroReservas(null, null, Set.of());
    }

    /**
     * Errores en {@code desde} y {@code hasta} (FORMATO, RANGO_INVALIDO, MAXIMO_DIAS) y en {@code estado}
     * (NO_PERMITIDO). {@code estados}: uno o varios separados por coma.
     */
    public static FiltroReservas validar(String desde, String hasta, String estados, ValidadorCampos validador) {
        LocalDate inicio = validador.fecha("desde", desde, false);
        LocalDate fin = validador.fecha("hasta", hasta, false);
        if (inicio != null && fin != null) {
            if (fin.isBefore(inicio)) {
                validador.error("hasta", MotivoInvalido.RANGO_INVALIDO);
            } else if (ChronoUnit.DAYS.between(inicio, fin) + 1 > MAXIMO_DIAS) {
                validador.error("hasta", MotivoInvalido.MAXIMO_DIAS);
            }
        }
        Set<EstadoReserva> conjunto = EnumSet.noneOf(EstadoReserva.class);
        String texto = ValidadorCampos.texto(estados);
        if (texto != null) {
            for (String parte : texto.split(",")) {
                String nombre = parte.trim();
                EstadoReserva estado = estado(nombre);
                if (estado == null) {
                    validador.error("estado", MotivoInvalido.NO_PERMITIDO);
                } else {
                    conjunto.add(estado);
                }
            }
        }
        return new FiltroReservas(inicio, fin, conjunto);
    }

    /**
     * Los del Turista más {@code servicio} (DEC-192): opcional; si no es un ObjectId, FORMATO. Un servicio que no es
     * del Prestador no da error: no tiene reservas suyas.
     */
    public static FiltroReservas validarDelPrestador(String desde, String hasta, String estados, String servicio,
                                                     ValidadorCampos validador) {
        FiltroReservas filtro = validar(desde, hasta, estados, validador);
        String id = ValidadorCampos.texto(servicio);
        if (id != null && !IdentificadorObjeto.esValido(id)) {
            validador.error("servicio", MotivoInvalido.FORMATO);
            id = null;
        }
        return new FiltroReservas(filtro.desde(), filtro.hasta(), filtro.estados(), id);
    }

    /** {@code desde} a las 00:00 en Santa Marta: la actividad debe terminar después. Null si no hay {@code desde}. */
    public Instant terminaDespuesDe() {
        return desde == null ? null : HorarioReservas.instante(desde, LocalTime.MIDNIGHT);
    }

    /** {@code hasta + 1 día} a las 00:00 en Santa Marta: la actividad debe empezar antes. Null si no hay {@code hasta}. */
    public Instant empiezaAntesDe() {
        return hasta == null ? null : HorarioReservas.instante(hasta.plusDays(1), LocalTime.MIDNIGHT);
    }

    /** La regla del cruce, la misma que aplica la consulta. */
    public boolean incluye(Reserva reserva) {
        Instant despues = terminaDespuesDe();
        Instant antes = empiezaAntesDe();
        return (despues == null || reserva.fechaFin().isAfter(despues))
                && (antes == null || reserva.fechaInicio().isBefore(antes))
                && (estados.isEmpty() || estados.contains(reserva.estado()))
                && (idServicio == null || idServicio.equals(reserva.idServicio()));
    }

    private static EstadoReserva estado(String nombre) {
        for (EstadoReserva estado : EstadoReserva.values()) {
            if (estado.name().equals(nombre)) {
                return estado;
            }
        }
        return null;
    }
}
