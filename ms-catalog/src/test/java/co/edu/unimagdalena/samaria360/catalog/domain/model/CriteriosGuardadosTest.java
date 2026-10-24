package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.CriteriosBusquedaInvalidosException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Criterios del historial (RF60, DEC-88): normalización, clave de deduplicación y las reglas de la búsqueda. */
class CriteriosGuardadosTest {

    @Test
    void seNormalizanSinPerderLoQueEscribioElTurista() {
        CriteriosGuardados criterios = new CriteriosGuardados("  Playa Cristal ", Categoria.Alojamiento,
                new BigDecimal("200000.00"), false, null,
                List.of(CondicionAccesibilidad.Visual, CondicionAccesibilidad.MovilidadReducida,
                        CondicionAccesibilidad.Visual), null,
                new ParametrosFiltrosCategoria(List.of("piscina", "WiFi", "Piscina"), 2, "  ", false, null, null,
                        null, null));

        assertThat(criterios.texto()).isEqualTo("Playa Cristal");
        assertThat(criterios.precioMax()).isEqualByComparingTo("200000").hasToString("200000");
        assertThat(criterios.accesibilidad())
                .containsExactly(CondicionAccesibilidad.MovilidadReducida, CondicionAccesibilidad.Visual);
        assertThat(criterios.masFiltros().serviciosIncluidos()).containsExactly("WiFi", "Piscina");
        assertThat(criterios.masFiltros().tipoHabitacion()).isNull();
        assertThat(criterios.masFiltros().menuVegetariano()).isNull();
        assertThat(criterios.masFiltros().huespedes()).isEqualTo(2);
    }

    @Test
    void laClaveNoDistingueMayusculasNiEspaciosNiElOrdenDeLasListas() {
        CriteriosGuardados una = texto(" playa   CRISTAL ", List.of(CondicionAccesibilidad.Visual,
                CondicionAccesibilidad.Auditiva));
        CriteriosGuardados otra = texto("Playa Cristal", List.of(CondicionAccesibilidad.Auditiva,
                CondicionAccesibilidad.Visual));

        assertThat(una.clave()).isEqualTo(otra.clave());
        assertThat(una.texto()).isEqualTo("playa   CRISTAL");
    }

    @Test
    void combinacionesDistintasTienenClavesDistintas() {
        CriteriosGuardados base = texto("Playa", List.of());

        assertThat(base.clave()).isNotEqualTo(texto("Playas", List.of()).clave());
        assertThat(base.clave()).isNotEqualTo(new CriteriosGuardados("Playa", null, null, false, null, List.of(),
                OrdenBusqueda.Nombre, null).clave());
        assertThat(base.clave()).isNotEqualTo(new CriteriosGuardados("Playa", null, null, true, null, List.of(),
                null, null).clave());
        assertThat(texto("a|precioMax=1", List.of()).clave())
                .isNotEqualTo(new CriteriosGuardados("a", null, BigDecimal.ONE, false, null, List.of(), null, null)
                        .clave());
    }

    @Test
    void elOrdenSoloNoEsUnCriterioYCualquierFiltroSi() {
        assertThat(new CriteriosGuardados(" ", null, null, false, null, null, OrdenBusqueda.Nombre, null).esVacio())
                .isTrue();
        assertThat(new CriteriosGuardados(null, Categoria.Tour, null, false, null, null, null, null).esVacio())
                .isFalse();
        assertThat(new CriteriosGuardados(null, null, BigDecimal.TEN, false, null, null, null, null).esVacio())
                .isFalse();
        assertThat(new CriteriosGuardados(null, null, null, true, null, null, null, null).esVacio()).isFalse();
        assertThat(texto(null, List.of(CondicionAccesibilidad.Visual)).esVacio()).isFalse();
    }

    @Test
    void laUbicacionNoSeGuardaPeroLaDistanciaYElOrdenPorDistanciaLaExigen() {
        CriteriosGuardados cerca = new CriteriosGuardados(null, null, null, true, 5.0, null, OrdenBusqueda.Distancia,
                null);

        assertThat(cerca.usarUbicacion()).isTrue();
        assertThat(cerca.distanciaKm()).isEqualTo(5.0);
        assertThatThrownBy(() -> new CriteriosGuardados(null, null, null, false, 5.0, null, null, null))
                .isInstanceOf(CriteriosBusquedaInvalidosException.class).hasMessageContaining("RF38");
        assertThatThrownBy(() -> new CriteriosGuardados("x", null, null, false, null, null, OrdenBusqueda.Distancia,
                null)).isInstanceOf(CriteriosBusquedaInvalidosException.class).hasMessageContaining("RF61");
    }

    @Test
    void lasReglasSonLasDeLaBusqueda() {
        assertThatThrownBy(() -> texto("x".repeat(101), List.of()))
                .isInstanceOf(CriteriosBusquedaInvalidosException.class);
        assertThatThrownBy(() -> new CriteriosGuardados(null, null, new BigDecimal("-1"), false, null, null, null,
                null)).isInstanceOf(CriteriosBusquedaInvalidosException.class);
        assertThatThrownBy(() -> new CriteriosGuardados(null, Categoria.Tour, null, false, null, null, null,
                new ParametrosFiltrosCategoria(null, null, null, null, "Caribeña", null, null, null)))
                .isInstanceOf(CriteriosBusquedaInvalidosException.class)
                .hasMessageContaining("tipoCocina requiere categoria=Gastronomia");
        assertThatThrownBy(() -> new CriteriosGuardados(null, Categoria.Alojamiento, null, false, null, null, null,
                new ParametrosFiltrosCategoria(List.of("Jacuzzi"), null, null, null, null, null, null, null)))
                .isInstanceOf(CriteriosBusquedaInvalidosException.class).hasMessageContaining("serviciosIncluidos");
    }

    private static CriteriosGuardados texto(String texto, List<CondicionAccesibilidad> accesibilidad) {
        return new CriteriosGuardados(texto, null, null, false, null, accesibilidad, null, null);
    }
}
