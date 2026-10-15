package co.edu.unimagdalena.samaria360.booking.domain.service;

import co.edu.unimagdalena.samaria360.booking.domain.exception.OfertaNoDisponibleException;
import co.edu.unimagdalena.samaria360.booking.domain.exception.PrecioCambiadoException;
import co.edu.unimagdalena.samaria360.booking.domain.exception.UnidadNoDisponibleException;
import co.edu.unimagdalena.samaria360.booking.domain.model.CategoriaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.DiaInventario;
import co.edu.unimagdalena.samaria360.booking.domain.model.HorarioReservas;
import co.edu.unimagdalena.samaria360.booking.domain.model.MotivoInvalido;
import co.edu.unimagdalena.samaria360.booking.domain.model.OfertaReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.ReservaPlanificada;
import co.edu.unimagdalena.samaria360.booking.domain.model.SolicitudReserva;
import co.edu.unimagdalena.samaria360.booking.domain.model.UnidadCobro;
import co.edu.unimagdalena.samaria360.booking.domain.model.UnidadReservable;
import co.edu.unimagdalena.samaria360.booking.domain.model.ValidadorCampos;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Reglas de «Reservar servicio» que dependen del catálogo (D1, D2, §5 del plan). Se aplican antes de abrir la
 * transacción, así que nunca hay filas bloqueadas mientras se valida. Orden de los errores:
 * <ol>
 *   <li>categoría y campos según la categoría (400 {@code DATOS_INVALIDOS}, todos a la vez);</li>
 *   <li>unidad del alojamiento (409 {@code UNIDAD_NO_DISPONIBLE});</li>
 *   <li>capacidad de la unidad o aforo del tour (400);</li>
 *   <li>precio que vio el turista (409 {@code PRECIO_CAMBIADO}, RF37).</li>
 * </ol>
 */
public final class PoliticaReserva {

    private final HorarioReservas horario;

    public PoliticaReserva(HorarioReservas horario) {
        this.horario = Objects.requireNonNull(horario);
    }

    public ReservaPlanificada planificar(SolicitudReserva solicitud, OfertaReservable oferta, Instant ahora) {
        CategoriaReservable categoria = CategoriaReservable.desde(oferta.categoria()).orElse(null);
        if (categoria == null) {
            ValidadorCampos validador = new ValidadorCampos();
            validador.error("idServicio", MotivoInvalido.CATEGORIA_NO_RESERVABLE);
            validador.lanzarSiHayErrores();
        }
        return categoria == CategoriaReservable.Alojamiento
                ? alojamiento(solicitud, oferta, ahora)
                : tour(solicitud, oferta, ahora);
    }

    // ------------------------------------------------------------------ alojamiento

    private ReservaPlanificada alojamiento(SolicitudReserva solicitud, OfertaReservable oferta, Instant ahora) {
        ValidadorCampos validador = new ValidadorCampos();
        LocalDate entrada = solicitud.fechaEntrada();
        LocalDate salida = solicitud.fechaSalida();
        LocalDate hoy = HorarioReservas.hoy(ahora);
        if (entrada.isBefore(hoy) || (entrada.equals(hoy)
                && ahora.isAfter(HorarioReservas.instante(entrada, horario.checkIn())))) {
            validador.error("fechaEntrada", MotivoInvalido.FECHA_PASADA);
        }
        if (salida == null) {
            validador.error("fechaSalida", MotivoInvalido.REQUERIDO);
        } else if (!salida.isAfter(entrada)) {
            validador.error("fechaSalida", MotivoInvalido.RANGO_INVALIDO);
        } else if (ChronoUnit.DAYS.between(entrada, salida) > HorarioReservas.MAXIMO_NOCHES) {
            validador.error("fechaSalida", MotivoInvalido.MAXIMO_NOCHES);
        }
        if (solicitud.horaInicio() != null) {
            validador.error("horaInicio", MotivoInvalido.NO_PERMITIDO);
        }
        if (solicitud.idUnidad() == null) {
            validador.error("idUnidad", MotivoInvalido.REQUERIDO);
        }
        if (solicitud.cantidad() == null) {
            validador.error("cantidad", MotivoInvalido.REQUERIDO);
        }
        validador.lanzarSiHayErrores();

        UnidadReservable unidad = oferta.unidad(solicitud.idUnidad())
                .orElseThrow(() -> new UnidadNoDisponibleException(solicitud.idUnidad()));
        int habitaciones = solicitud.cantidad();
        if (habitaciones > unidad.numHabitaciones()) {
            validador.error("cantidad", MotivoInvalido.EXCEDE_CAPACIDAD);
        }
        if ((long) solicitud.personas() > (long) habitaciones * unidad.capacidadMaxHuespedes()) {
            validador.error("personas", MotivoInvalido.EXCEDE_CAPACIDAD);
        }
        validador.lanzarSiHayErrores();

        long noches = ChronoUnit.DAYS.between(entrada, salida);
        BigDecimal total = unidad.precioNoche().multiply(BigDecimal.valueOf(noches))
                .multiply(BigDecimal.valueOf(habitaciones));
        verificarPrecio(solicitud, total, unidad.precioNoche());

        List<DiaInventario> dias = new ArrayList<>();
        for (LocalDate noche = entrada; noche.isBefore(salida); noche = noche.plusDays(1)) {
            dias.add(new DiaInventario(oferta.idServicio(), unidad.idUnidad(), noche, null));
        }
        return new ReservaPlanificada(oferta.idServicio(), oferta.idPrestador(), oferta.nombre(),
                CategoriaReservable.Alojamiento, unidad.idUnidad(), unidad.nombre(), habitaciones,
                solicitud.personas(), unidad.precioNoche(), UnidadCobro.Noche, total, entrada, salida, null,
                HorarioReservas.instante(entrada, horario.checkIn()),
                HorarioReservas.instante(salida, horario.checkOut()), dias);
    }

    // ------------------------------------------------------------------ tour

    private ReservaPlanificada tour(SolicitudReserva solicitud, OfertaReservable oferta, Instant ahora) {
        if (oferta.precioBase() == null || oferta.aforoMaxGrupo() == null || oferta.duracionHoras() == null) {
            throw new OfertaNoDisponibleException(oferta.idServicio());
        }
        ValidadorCampos validador = new ValidadorCampos();
        LocalDate fecha = solicitud.fechaEntrada();
        LocalTime hora = solicitud.horaInicio();
        if (hora == null) {
            validador.error("horaInicio", MotivoInvalido.REQUERIDO);
        }
        if (fecha.isBefore(HorarioReservas.hoy(ahora))
                || (hora != null && !HorarioReservas.instante(fecha, hora).isAfter(ahora))) {
            validador.error("fechaEntrada", MotivoInvalido.FECHA_PASADA);
        }
        if (solicitud.fechaSalida() != null) {
            validador.error("fechaSalida", MotivoInvalido.NO_PERMITIDO);
        }
        if (solicitud.idUnidad() != null) {
            validador.error("idUnidad", MotivoInvalido.NO_PERMITIDO);
        }
        if (solicitud.cantidad() != null) {
            validador.error("cantidad", MotivoInvalido.NO_PERMITIDO);
        }
        if (solicitud.personas() > oferta.aforoMaxGrupo()) {
            validador.error("personas", MotivoInvalido.EXCEDE_AFORO);
        }
        validador.lanzarSiHayErrores();

        BigDecimal total = oferta.precioBase().multiply(BigDecimal.valueOf(solicitud.personas()));
        verificarPrecio(solicitud, total, oferta.precioBase());

        Instant inicio = HorarioReservas.instante(fecha, hora);
        Duration duracion = Duration.ofMinutes(oferta.duracionHoras().multiply(BigDecimal.valueOf(60)).longValue());
        return new ReservaPlanificada(oferta.idServicio(), oferta.idPrestador(), oferta.nombre(),
                CategoriaReservable.Tour, null, null, solicitud.personas(), solicitud.personas(), oferta.precioBase(),
                UnidadCobro.Persona, total, fecha, null, hora, inicio, inicio.plus(duracion),
                List.of(new DiaInventario(oferta.idServicio(), null, fecha, hora)));
    }

    /** RF37: el precio lo calcula el servidor; si no es el que vio el turista, no se reserva. */
    private static void verificarPrecio(SolicitudReserva solicitud, BigDecimal total, BigDecimal precioUnitario) {
        if (total.compareTo(solicitud.precioTotalEsperado()) != 0) {
            throw new PrecioCambiadoException(total, precioUnitario);
        }
    }
}
