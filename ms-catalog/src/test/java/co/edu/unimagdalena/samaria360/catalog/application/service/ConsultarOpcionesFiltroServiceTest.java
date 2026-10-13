package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.OpcionesFiltroRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.CriteriosBusquedaInvalidosException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Categoria;
import co.edu.unimagdalena.samaria360.catalog.domain.model.OpcionesFiltro;
import co.edu.unimagdalena.samaria360.catalog.domain.model.OpcionesFiltro.Rango;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioIncluido;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultarOpcionesFiltroServiceTest {

    /** Doble del puerto: devuelve opciones fijas y registra las categorías consultadas. */
    private static final class OpcionesFijas implements OpcionesFiltroRepositoryPort {

        private final List<Categoria> consultadas = new ArrayList<>();

        @Override
        public OpcionesFiltro opciones(Categoria categoria) {
            consultadas.add(categoria);
            return switch (categoria) {
                case Alojamiento -> OpcionesFiltro.alojamiento(Set.of(ServicioIncluido.Piscina, ServicioIncluido.WiFi),
                        List.of("Suite", "Cabaña", "Doble", "Ecohabitación"), new Rango<>(2, 6));
                case Gastronomia -> OpcionesFiltro.gastronomia(true, List.of("Típica", "Mariscos"));
                case Tour -> OpcionesFiltro.tour(List.of("Media", "Fácil"),
                        new Rango<>(new BigDecimal("3.5"), new BigDecimal("8")));
                case AlquilerEquipo -> OpcionesFiltro.alquilerEquipo(List.of("Bicicleta urbana"));
            };
        }
    }

    private final OpcionesFijas repositorio = new OpcionesFijas();
    private final ConsultarOpcionesFiltroService servicio = new ConsultarOpcionesFiltroService(repositorio);

    @Test
    void sinCategoriaEsUnCriterioInvalidoYNoConsultaElRepositorio() {
        assertThatThrownBy(() -> servicio.consultar(null))
                .isInstanceOf(CriteriosBusquedaInvalidosException.class)
                .hasMessageContaining("categoria");
        assertThat(repositorio.consultadas).isEmpty();
    }

    @Test
    void consultaLaCategoriaPedida() {
        OpcionesFiltro opciones = servicio.consultar(Categoria.Tour);

        assertThat(repositorio.consultadas).containsExactly(Categoria.Tour);
        assertThat(opciones.categoria()).isEqualTo(Categoria.Tour);
    }

    @Test
    void lasListasVanOrdenadasEnEspanolYSoloConLosCamposDeLaCategoria() {
        OpcionesFiltro alojamiento = servicio.consultar(Categoria.Alojamiento);

        assertThat(alojamiento.tiposHabitacion()).containsExactly("Cabaña", "Doble", "Ecohabitación", "Suite");
        assertThat(alojamiento.serviciosIncluidos()).containsExactly(ServicioIncluido.WiFi, ServicioIncluido.Piscina);
        assertThat(alojamiento.huespedes()).isEqualTo(new Rango<>(2, 6));
        assertThat(alojamiento.tiposCocina()).isNull();
        assertThat(alojamiento.duracionHoras()).isNull();

        assertThat(servicio.consultar(Categoria.Tour).nivelesDificultad()).containsExactly("Fácil", "Media");
        assertThat(servicio.consultar(Categoria.Gastronomia).serviciosIncluidos()).isNull();
    }

    @Test
    void unRangoNoPuedeEstarInvertido() {
        assertThatThrownBy(() -> new Rango<>(6, 2)).isInstanceOf(IllegalArgumentException.class);
    }
}
