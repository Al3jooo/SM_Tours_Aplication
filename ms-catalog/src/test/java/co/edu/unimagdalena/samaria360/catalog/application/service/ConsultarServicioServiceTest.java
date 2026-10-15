package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.fake.ServicioRepositoryEnMemoria;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.OfertaNoDisponibleException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Categoria;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Coordenada;
import co.edu.unimagdalena.samaria360.catalog.domain.model.DetalleAlojamiento;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EstadoPublicacion;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Unidades;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Detalle de un servicio (WF-06, WF-10): solo los visibles, con sus unidades (D1). */
class ConsultarServicioServiceTest {

    private static final UUID PRESTADOR = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final String VISIBLE = Unidades.nuevoId();
    private static final String PENDIENTE = Unidades.nuevoId();
    private static final String DE_INACTIVO = Unidades.nuevoId();

    private final ServicioRepositoryEnMemoria repositorio = new ServicioRepositoryEnMemoria().con(
            hotel(VISIBLE, EstadoPublicacion.Publicado, true),
            hotel(PENDIENTE, EstadoPublicacion.Pendiente, true),
            hotel(DE_INACTIVO, EstadoPublicacion.Publicado, false));
    private final ConsultarServicioService servicio = new ConsultarServicioService(repositorio);

    @Test
    void devuelveElServicioVisibleConSusUnidades() {
        ServicioComercial hotel = servicio.consultar(VISIBLE);

        assertThat(hotel.getId()).contains(VISIBLE);
        assertThat(((DetalleAlojamiento) hotel.getDetalle()).unidades())
                .extracting(unidad -> unidad.nombre()).containsExactly("Doble", "Suite familiar");
        assertThat(hotel.getPrecioBase()).hasValueSatisfying(precio -> assertThat(precio).isEqualByComparingTo("150000"));
    }

    @Test
    void unServicioPendienteODeUnPrestadorInactivoNoExisteParaElPublico() {
        for (String id : List.of(PENDIENTE, DE_INACTIVO, Unidades.nuevoId())) {
            assertThatThrownBy(() -> servicio.consultar(id)).isInstanceOf(OfertaNoDisponibleException.class);
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc", "6ACFF19A69BFCC83461B196D", "6acff19a69bfcc83461b196", "' or 1=1"})
    void unIdQueNoEsUnObjectIdNoLlegaAlRepositorio(String id) {
        assertThatThrownBy(() -> servicio.consultar(id)).isInstanceOf(OfertaNoDisponibleException.class);
        assertThat(repositorio.consultados()).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"Publicado,true,true", "Publicado,false,false", "Rechazado,true,false"})
    void aunSiElRepositorioDevolvieraUnServicioNoVisibleNoSeEntrega(EstadoPublicacion estado, boolean activo,
                                                                    boolean entregado) {
        String id = Unidades.nuevoId();
        ConsultarServicioService conRepositorioPermisivo = new ConsultarServicioService(
                idBuscado -> Optional.of(hotel(id, estado, activo)));

        if (entregado) {
            assertThat(conRepositorioPermisivo.consultar(id).getId()).contains(id);
        } else {
            assertThatThrownBy(() -> conRepositorioPermisivo.consultar(id))
                    .isInstanceOf(OfertaNoDisponibleException.class);
        }
    }

    private static ServicioComercial hotel(String id, EstadoPublicacion estado, boolean prestadorActivo) {
        DetalleAlojamiento detalle = new DetalleAlojamiento(List.of(
                Unidades.unidad("Doble", 12, 2, "150000"),
                Unidades.unidad("Suite familiar", "Suite", 3, 4, "260000")), List.of("WiFi"));
        return new ServicioComercial(id, PRESTADOR, "Hotel Casa Tayrona", Categoria.Alojamiento, detalle, null,
                new Coordenada(-74.2125, 11.2425), null, estado, prestadorActivo, Set.of(), null, null, List.of());
    }
}
