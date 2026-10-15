package co.edu.unimagdalena.samaria360.booking.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** Ofertas y fechas de las pruebas: un hotel con dos unidades (como Casa Tayrona con M002) y un tour. */
public final class Ofertas {

    public static final UUID PRESTADOR = UUID.fromString("00000000-0000-4000-8000-000000000002");
    public static final String HOTEL = "6a0000000000000000000001";
    public static final String TOUR = "6a0000000000000000000002";
    public static final String RESTAURANTE = "6a0000000000000000000003";
    public static final String DOBLE = "6b0000000000000000000001";
    public static final String SUITE = "6b0000000000000000000002";
    /** Domingo 10 de marzo de 2030, 12:00 en Santa Marta. */
    public static final Instant AHORA = LocalDate.of(2030, 3, 10).atTime(12, 0).atZone(HorarioReservas.ZONA).toInstant();
    public static final LocalDate HOY = LocalDate.of(2030, 3, 10);

    private Ofertas() {
    }

    public static OfertaReservable hotel() {
        return hotel(new BigDecimal("150000"), new BigDecimal("260000"));
    }

    public static OfertaReservable hotel(BigDecimal precioDoble, BigDecimal precioSuite) {
        return new OfertaReservable(HOTEL, PRESTADOR, "Hotel Casa Tayrona", "Alojamiento", precioDoble.min(precioSuite),
                null, null, List.of(
                new UnidadReservable(DOBLE, "Doble", 12, 2, precioDoble),
                new UnidadReservable(SUITE, "Suite familiar", 3, 4, precioSuite)));
    }

    public static OfertaReservable tour() {
        return new OfertaReservable(TOUR, PRESTADOR, "Tour de Café en Minca", "Tour", new BigDecimal("80000"), 12,
                new BigDecimal("4"), List.of());
    }

    public static OfertaReservable restaurante() {
        return new OfertaReservable(RESTAURANTE, PRESTADOR, "Restaurante Ouzo", "Gastronomia", new BigDecimal("50000"),
                null, null, List.of());
    }

    /** Alojamiento: {@code noches} desde {@code entrada}, {@code habitaciones} de la unidad. */
    public static SolicitudReserva alojamiento(String unidad, LocalDate entrada, int noches, int habitaciones,
                                               int personas, String precioEsperado) {
        return new SolicitudReserva(HOTEL, unidad, entrada, entrada.plusDays(noches), null, habitaciones, personas,
                new BigDecimal(precioEsperado));
    }

    public static SolicitudReserva tour(LocalDate fecha, LocalTime hora, int personas, String precioEsperado) {
        return new SolicitudReserva(TOUR, null, fecha, null, hora, null, personas, new BigDecimal(precioEsperado));
    }
}
