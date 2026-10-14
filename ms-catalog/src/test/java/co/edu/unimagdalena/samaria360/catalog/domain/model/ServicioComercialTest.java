package co.edu.unimagdalena.samaria360.catalog.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServicioComercialTest {

    private static final UUID PRESTADOR = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final Coordenada CENTRO = new Coordenada(-74.2125, 11.2425);
    private static final DetalleAlojamiento ALOJAMIENTO = Unidades.alojamiento("Doble", 12, 2, "150000", List.of("WiFi"));
    private static final DetalleTour TOUR = new DetalleTour("Muelle", new BigDecimal("3"), "Media", 10);

    @Test
    void creaUnServicioValido() {
        ServicioComercial hotel = servicio("Hotel Casa Tayrona", Categoria.Alojamiento, ALOJAMIENTO,
                EstadoPublicacion.Publicado, true, new BigDecimal("150000"));

        assertThat(hotel.getId()).isEmpty();
        assertThat(hotel.getCategoria()).isEqualTo(Categoria.Alojamiento);
        assertThat(hotel.getPrecioBase()).contains(new BigDecimal("150000"));
        assertThat(hotel.getCondicionesAccesibilidad()).containsExactly(CondicionAccesibilidad.MovilidadReducida);
    }

    @Test
    void rechazaUnDetalleDeOtraCategoria() {
        DetalleGastronomia gastronomia = new DetalleGastronomia("Mediterránea", 60, true, "$50.000");

        assertThatThrownBy(() -> servicio("Hotel", Categoria.Alojamiento, gastronomia,
                EstadoPublicacion.Publicado, true, BigDecimal.TEN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no corresponde");
    }

    @Test
    void elNombreComercialAdmiteHasta150Caracteres() {
        String nombre150 = "a".repeat(ServicioComercial.LONGITUD_MAXIMA_NOMBRE);

        assertThat(servicio(nombre150, Categoria.Alojamiento, ALOJAMIENTO, EstadoPublicacion.Publicado, true, null)
                .getNombreComercial()).hasSize(150);
        assertThatThrownBy(() -> servicio(nombre150 + "a", Categoria.Alojamiento, ALOJAMIENTO,
                EstadoPublicacion.Publicado, true, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> servicio("  ", Categoria.Alojamiento, ALOJAMIENTO, EstadoPublicacion.Publicado, true, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaUnPrecioNegativo() {
        assertThatThrownBy(() -> servicio("Tour", Categoria.Tour, TOUR, EstadoPublicacion.Publicado,
                true, new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void elPrecioDeUnTourEsOpcional() {
        assertThat(servicio("Tour", Categoria.Tour, TOUR, EstadoPublicacion.Publicado, true, null).getPrecioBase())
                .isEmpty();
    }

    @Test
    void elPrecioDeUnAlojamientoEsElMenorDeSusUnidades() {
        DetalleAlojamiento hotel = new DetalleAlojamiento(List.of(
                Unidades.unidad("Suite familiar", "Suite", 3, 4, "260000"),
                Unidades.unidad("Doble", 12, 2, "150000")), List.of());

        assertThat(servicio("Hotel", Categoria.Alojamiento, hotel, EstadoPublicacion.Publicado, true, null)
                .getPrecioBase()).hasValueSatisfying(precio -> assertThat(precio).isEqualByComparingTo("150000"));
        assertThat(servicio("Hotel", Categoria.Alojamiento, hotel, EstadoPublicacion.Publicado, true,
                new BigDecimal("150000.00")).getPrecioBase())
                .hasValueSatisfying(precio -> assertThat(precio).isEqualByComparingTo("150000"));
    }

    @Test
    void rechazaUnPrecioDeAlojamientoQueContradiceASusUnidades() {
        assertThatThrownBy(() -> servicio("Hotel", Categoria.Alojamiento, ALOJAMIENTO, EstadoPublicacion.Publicado,
                true, new BigDecimal("260000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("menor precioNoche");
    }

    @ParameterizedTest
    @CsvSource({
            "Publicado, true, true",
            "Publicado, false, false",
            "Pendiente, true, false",
            "Rechazado, true, false"
    })
    void soloEsVisibleSiEstaPublicadoYElPrestadorEstaActivo(EstadoPublicacion estado, boolean prestadorActivo,
                                                            boolean visible) {
        ServicioComercial hotel = servicio("Hotel", Categoria.Alojamiento, ALOJAMIENTO, estado, prestadorActivo, null);

        assertThat(hotel.esVisibleEnBusquedaPublica()).isEqualTo(visible);
    }

    private static ServicioComercial servicio(String nombre, Categoria categoria, DetalleCategoria detalle,
                                              EstadoPublicacion estado, boolean prestadorActivo, BigDecimal precio) {
        return new ServicioComercial(null, PRESTADOR, nombre, categoria, detalle, "Descripción", CENTRO,
                new Contacto("+57 300 000 0000", "hola@hotel.test"), estado, prestadorActivo,
                Set.of(CondicionAccesibilidad.MovilidadReducida), "24 horas", precio, List.of("/img/hotel.svg"));
    }
}
