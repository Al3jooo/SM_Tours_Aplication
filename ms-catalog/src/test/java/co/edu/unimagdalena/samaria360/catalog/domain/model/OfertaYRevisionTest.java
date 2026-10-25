package co.edu.unimagdalena.samaria360.catalog.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Invariantes de la oferta (DEC-136), de la revisión (RF21, DEC-145) y del RNT de la proyección (DEC-146), y su
 * coherencia con el servicio: el validador v3 de M008 exige lo mismo.
 */
class OfertaYRevisionTest {

    private static final UUID PRESTADOR = UUID.fromString("00000000-0000-4000-8000-0000000000c2");
    private static final UUID ADMIN = UUID.fromString("00000000-0000-4000-8000-0000000000c3");
    private static final Instant AHORA = Instant.parse("2026-10-24T15:00:00Z");
    private static final Coordenada CENTRO = new Coordenada(-74.2125, 11.2425);

    @Test
    void laOfertaOrdenaLosDiasYLasSalidasYNoAdmiteRepetidos() {
        Oferta oferta = new Oferta(List.of(DiaSemana.Domingo, DiaSemana.Lunes),
                List.of(LocalTime.of(14, 0), LocalTime.of(8, 0)));

        assertThat(oferta.diasOperacion()).containsExactly(DiaSemana.Lunes, DiaSemana.Domingo);
        assertThat(oferta.salidas()).containsExactly(LocalTime.of(8, 0), LocalTime.of(14, 0));
        assertThatThrownBy(() -> new Oferta(List.of(), List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Oferta(List.of(DiaSemana.Lunes, DiaSemana.Lunes), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Oferta(List.of(DiaSemana.Lunes), List.of(LocalTime.of(8, 0, 30))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void laOfertaDebeCorresponderALaCategoria() {
        Oferta conSalidas = new Oferta(List.of(DiaSemana.Lunes), List.of(LocalTime.of(8, 0)));
        Oferta sinSalidas = new Oferta(List.of(DiaSemana.Lunes), List.of());

        assertThat(tour(conSalidas, EstadoPublicacion.Pendiente, null).getOferta()).contains(conSalidas);
        assertThatThrownBy(() -> tour(sinSalidas, EstadoPublicacion.Pendiente, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ServicioComercial(null, PRESTADOR, "Restaurante", Categoria.Gastronomia,
                new DetalleGastronomia("Caribeña", 40, null, "$$"), null, CENTRO, null, EstadoPublicacion.Pendiente,
                true, Set.of(), null, new BigDecimal("20000"), List.of(), null, sinSalidas, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elMotivoDeRechazoExisteSiYSoloSiElServicioEstaRechazado() {
        Oferta oferta = new Oferta(List.of(DiaSemana.Lunes), List.of(LocalTime.of(8, 0)));
        Revision aprobada = new Revision(ADMIN, AHORA, null);
        Revision rechazada = new Revision(ADMIN, AHORA, "Faltan datos de contacto");

        assertThat(tour(oferta, EstadoPublicacion.Publicado, aprobada).getRevision()).contains(aprobada);
        assertThat(tour(oferta, EstadoPublicacion.Rechazado, rechazada).getRevision()).contains(rechazada);
        // Un Publicado de la semilla no tiene revisión.
        assertThat(tour(oferta, EstadoPublicacion.Publicado, null).getRevision()).isEmpty();
        assertThatThrownBy(() -> tour(oferta, EstadoPublicacion.Pendiente, aprobada))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tour(oferta, EstadoPublicacion.Rechazado, aprobada))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> tour(oferta, EstadoPublicacion.Publicado, rechazada))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void conPrestadorActivoSoloCambiaEsaCopia() {
        ServicioComercial servicio = tour(new Oferta(List.of(DiaSemana.Lunes), List.of(LocalTime.of(8, 0))),
                EstadoPublicacion.Pendiente, null);

        ServicioComercial oculto = servicio.conPrestadorActivo(false);

        assertThat(oculto.isPrestadorActivo()).isFalse();
        assertThat(oculto.getId()).isEqualTo(servicio.getId());
        assertThat(oculto.getOferta()).isEqualTo(servicio.getOferta());
        assertThat(oculto.getRnt()).contains("RNT-1");
        assertThat(oculto.getFechaRegistro()).contains(AHORA);
    }

    @Test
    void soloUnPrestadorActivoConRntPuedeRegistrarServicios() {
        assertThat(new Cuenta(PRESTADOR, RolCuenta.Prestador, EstadoCuenta.Activo, 2, "RNT-1")
                .puedeRegistrarServicios()).isTrue();
        assertThat(new Cuenta(PRESTADOR, RolCuenta.Prestador, EstadoCuenta.Activo, 2).puedeRegistrarServicios())
                .isFalse();
        assertThat(new Cuenta(PRESTADOR, RolCuenta.Prestador, EstadoCuenta.Inactivo, 2, "RNT-1")
                .puedeRegistrarServicios()).isFalse();
        assertThat(new Cuenta(PRESTADOR, RolCuenta.Prestador, EstadoCuenta.Pendiente, 2, "RNT-1")
                .puedeRegistrarServicios()).isFalse();
        assertThatThrownBy(() -> new Cuenta(PRESTADOR, RolCuenta.Turista, EstadoCuenta.Activo, 2, "RNT-1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Cuenta(PRESTADOR, RolCuenta.Prestador, EstadoCuenta.Activo, 2, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ServicioComercial tour(Oferta oferta, EstadoPublicacion estado, Revision revision) {
        return new ServicioComercial(Unidades.nuevoId(), PRESTADOR, "Tour", Categoria.Tour,
                new DetalleTour("Muelle", new BigDecimal("3"), null, 10), "Descripción", CENTRO, null, estado, true,
                Set.of(), null, new BigDecimal("50000"), List.of(), "Calle 1", oferta, "RNT-1", AHORA, revision);
    }
}
