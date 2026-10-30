package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ActualizacionPrecio.PrecioUnidad;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorServicio.DatosContacto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

/**
 * CU «Actualizar tarifas» (RF32; DEC-183, DEC-184) y la parte del servicio de «Actualizar datos comerciales» (RF43;
 * DEC-188, DEC-189), sin Spring: las reglas del registro (DEC-138), el «desde» derivado (DEC-127), la oferta intacta
 * (DEC-183) y la versión leída (DEC-186).
 */
class ActualizacionServicioTest {

    private static final UUID PRESTADOR = UUID.fromString("00000000-0000-4000-8000-0000000000e1");

    private static ServicioComercial registrado(BorradorServicio borrador) {
        return RegistroServicio.registrar(borrador, Unidades.nuevoId(), Unidades::nuevoId, PRESTADOR, "RNT-1", true,
                Instant.parse("2026-10-24T15:00:00Z"));
    }

    private static ServicioComercial alojamiento() {
        return registrado(Borradores.alojamiento(List.of(Borradores.unidad("Doble", 8, 2, "150000"),
                Borradores.unidad("Suite", 2, 4, "320000"))));
    }

    private static List<String> ids(ServicioComercial servicio) {
        return ((DetalleAlojamiento) servicio.getDetalle()).unidades().stream().map(UnidadAlojamiento::idUnidad)
                .toList();
    }

    private static void assertCampos(Runnable accion, java.util.Map.Entry<String, MotivoInvalido>... esperados) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(ContenidoInvalidoException.class,
                e -> assertThat(e.getCampos()).containsOnly(esperados));
    }

    // ------------------------------------------------------------------ RF32

    @Test
    void unTourCambiaSuPrecioBaseYConservaLaOfertaYLosDemasDatos() {
        ServicioComercial tour = registrado(Borradores.tour());

        ServicioEditado editado = ActualizacionPrecio.aplicar(tour, new BigDecimal("120000.50"), null, 3);

        ServicioComercial nuevo = editado.servicio();
        assertThat(editado.versionEsperada()).isEqualTo(3);
        assertThat(nuevo.getPrecioBase()).contains(new BigDecimal("120000.50"));
        assertThat(nuevo.getOferta()).isEqualTo(tour.getOferta());
        assertThat(nuevo.getDetalle()).isEqualTo(tour.getDetalle());
        assertThat(nuevo.getNombreComercial()).isEqualTo(tour.getNombreComercial());
        assertThat(nuevo.getEstadoPublicacion()).isEqualTo(tour.getEstadoPublicacion());
    }

    @Test
    void enAlojamientoCambianLosPreciosPorNocheYElDesdeSeVuelveADerivar() {
        ServicioComercial hotel = alojamiento();
        List<String> ids = ids(hotel);

        ServicioComercial nuevo = ActualizacionPrecio.aplicar(hotel, new BigDecimal("1"), List.of(
                new PrecioUnidad(ids.get(1), new BigDecimal("280000")),
                new PrecioUnidad(ids.get(0), new BigDecimal("410000"))), 0).servicio();

        DetalleAlojamiento detalle = (DetalleAlojamiento) nuevo.getDetalle();
        assertThat(detalle.unidades()).extracting(UnidadAlojamiento::idUnidad).containsExactlyElementsOf(ids);
        assertThat(detalle.unidades()).extracting(UnidadAlojamiento::precioNoche)
                .containsExactly(new BigDecimal("410000"), new BigDecimal("280000"));
        assertThat(detalle.unidades()).extracting(UnidadAlojamiento::nombre).containsExactly("Doble", "Suite");
        // El precioBase recibido se ignora: el «desde» es el menor precio por noche (DEC-127).
        assertThat(nuevo.getPrecioBase()).contains(new BigDecimal("280000"));
        assertThat(nuevo.getOferta()).isEqualTo(hotel.getOferta());
    }

    @Test
    void elPrecioSigueLasReglasDelRegistro() {
        ServicioComercial tour = registrado(Borradores.tour());

        assertCampos(() -> ActualizacionPrecio.aplicar(tour, null, null, 0), entry("precioBase", MotivoInvalido.REQUERIDO));
        assertCampos(() -> ActualizacionPrecio.aplicar(tour, BigDecimal.ZERO, null, 0),
                entry("precioBase", MotivoInvalido.VALOR_MINIMO));
        assertCampos(() -> ActualizacionPrecio.aplicar(tour, new BigDecimal("100000000"), null, 0),
                entry("precioBase", MotivoInvalido.VALOR_MAXIMO));
        assertCampos(() -> ActualizacionPrecio.aplicar(tour, new BigDecimal("10.001"), null, 0),
                entry("precioBase", MotivoInvalido.FORMATO));
    }

    @Test
    void laVersionEsObligatoriaYNoNegativa() {
        ServicioComercial tour = registrado(Borradores.tour());

        assertCampos(() -> ActualizacionPrecio.aplicar(tour, BigDecimal.TEN, null, null),
                entry("version", MotivoInvalido.REQUERIDO));
        assertCampos(() -> ActualizacionPrecio.aplicar(tour, BigDecimal.TEN, null, -1),
                entry("version", MotivoInvalido.VALOR_MINIMO));
    }

    @Test
    void enAlojamientoDebenVenirTodasLasUnidadesUnaVezCadaUna() {
        ServicioComercial hotel = alojamiento();
        List<String> ids = ids(hotel);
        BigDecimal precio = new BigDecimal("200000");

        assertCampos(() -> ActualizacionPrecio.aplicar(hotel, null, null, 0),
                entry("unidades", MotivoInvalido.REQUERIDO));
        assertCampos(() -> ActualizacionPrecio.aplicar(hotel, null, List.of(new PrecioUnidad(ids.get(0), precio)), 0),
                entry("unidades", MotivoInvalido.CANTIDAD_MINIMA));
        assertCampos(() -> ActualizacionPrecio.aplicar(hotel, null, List.of(new PrecioUnidad(ids.get(0), precio),
                new PrecioUnidad(ids.get(0), precio)), 0), entry("unidades[1].idUnidad", MotivoInvalido.DUPLICADO));
        assertCampos(() -> ActualizacionPrecio.aplicar(hotel, null, List.of(new PrecioUnidad(ids.get(0), precio),
                new PrecioUnidad(Unidades.nuevoId(), precio)), 0),
                entry("unidades[1].idUnidad", MotivoInvalido.NO_PERMITIDO));
        assertCampos(() -> ActualizacionPrecio.aplicar(hotel, null, Arrays.asList(new PrecioUnidad(null, precio),
                null, new PrecioUnidad(ids.get(1), BigDecimal.ZERO)), 0),
                entry("unidades[0].idUnidad", MotivoInvalido.REQUERIDO), entry("unidades[1]", MotivoInvalido.REQUERIDO),
                entry("unidades[2].precioNoche", MotivoInvalido.VALOR_MINIMO));
    }

    // ------------------------------------------------------------------ RF43

    @Test
    void losDatosComercialesCambianElNombreYElContactoRecortados() {
        ServicioComercial tour = registrado(Borradores.tour());

        ServicioComercial nuevo = ActualizacionDatosComerciales.aplicar(tour, "  Tour Nuevo  ",
                new DatosContacto(" +57 311 ", "reservas@nuevo.test"), 1).servicio();

        assertThat(nuevo.getNombreComercial()).isEqualTo("Tour Nuevo");
        assertThat(nuevo.getContacto()).contains(new Contacto("+57 311", "reservas@nuevo.test"));
        assertThat(nuevo.getPrecioBase()).isEqualTo(tour.getPrecioBase());
        assertThat(nuevo.getOferta()).isEqualTo(tour.getOferta());
    }

    @Test
    void losDatosComercialesSiguenLasReglasDelRegistro() {
        ServicioComercial tour = registrado(Borradores.tour());

        assertCampos(() -> ActualizacionDatosComerciales.aplicar(tour, " ", null, null),
                entry("version", MotivoInvalido.REQUERIDO), entry("nombreComercial", MotivoInvalido.REQUERIDO),
                entry("contacto", MotivoInvalido.REQUERIDO));
        assertCampos(() -> ActualizacionDatosComerciales.aplicar(tour, "x".repeat(151),
                        new DatosContacto("1".repeat(21), "sin-arroba"), 0),
                entry("nombreComercial", MotivoInvalido.LONGITUD_MAXIMA),
                entry("contacto.telefono", MotivoInvalido.LONGITUD_MAXIMA),
                entry("contacto.correo", MotivoInvalido.FORMATO));
    }

    @Test
    void soloUnServicioRechazadoNoEsEditable() {
        ServicioComercial tour = registrado(Borradores.tour());
        Revision revision = new Revision(UUID.randomUUID(), Instant.parse("2026-10-25T10:00:00Z"), null);

        assertThat(tour.esEditablePorElPrestador()).isTrue();
        assertThat(conEstado(tour, EstadoPublicacion.Publicado, revision).esEditablePorElPrestador()).isTrue();
        assertThat(conEstado(tour, EstadoPublicacion.Rechazado,
                new Revision(revision.revisadoPor(), revision.fechaRevision(), "Faltan datos"))
                .esEditablePorElPrestador()).isFalse();
    }

    private static ServicioComercial conEstado(ServicioComercial s, EstadoPublicacion estado, Revision revision) {
        return new ServicioComercial(s.getId().orElseThrow(), s.getPrestadorId(), s.getNombreComercial(),
                s.getCategoria(), s.getDetalle(), s.getDescripcion().orElse(null), s.getCoordenada(),
                s.getContacto().orElse(null), estado, true, s.getCondicionesAccesibilidad(), null,
                s.getPrecioBase().orElse(null), s.getImagenes(), s.getDireccion().orElse(null),
                s.getOferta().orElse(null), s.getRnt().orElse(null), s.getFechaRegistro().orElse(null), revision, 4);
    }
}
