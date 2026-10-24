package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.fake.HistorialBusquedaRepositoryEnMemoria;
import co.edu.unimagdalena.samaria360.catalog.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BusquedaReciente;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Categoria;
import co.edu.unimagdalena.samaria360.catalog.domain.model.CriteriosGuardados;
import co.edu.unimagdalena.samaria360.catalog.domain.model.OrdenBusqueda;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/** CU «Consultar historial de búsquedas» (RF60; DEC-56, DEC-88) con dobles de los puertos. */
class HistorialBusquedasServiceTest {

    private static final UUID TURISTA = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID OTRO = UUID.fromString("00000000-0000-4000-8000-000000000009");

    private final HistorialBusquedaRepositoryEnMemoria historial = new HistorialBusquedaRepositoryEnMemoria();
    private final RelojFijo reloj = new RelojFijo(Instant.parse("2026-10-23T10:00:00.987654Z"));
    private final HistorialBusquedasService servicio = new HistorialBusquedasService(historial, reloj);

    @Test
    void registrarGuardaLaBusquedaConLaFechaEnMilisegundos() {
        servicio.registrar(TURISTA, texto("Playa"));

        assertThat(servicio.listar(TURISTA)).singleElement().satisfies(b -> {
            assertThat(b.criterios().texto()).isEqualTo("Playa");
            assertThat(b.fecha()).isEqualTo(Instant.parse("2026-10-23T10:00:00.987Z"));
        });
    }

    @Test
    void losMismosCriteriosNoSeDuplicanYSubenAlPrimerLugar() {
        servicio.registrar(TURISTA, texto("Playa"));
        reloj.avanzar(Duration.ofMinutes(1));
        servicio.registrar(TURISTA, texto("Tayrona"));
        reloj.avanzar(Duration.ofMinutes(1));

        servicio.registrar(TURISTA, texto("  PLAYA "));

        List<BusquedaReciente> lista = servicio.listar(TURISTA);
        assertThat(lista).extracting(b -> b.criterios().texto()).containsExactly("PLAYA", "Tayrona");
    }

    @Test
    void laBusquedaOnceBorraLaMasAntiguaYSeDevuelvenComoMaximoDiezEnOrden() {
        for (int i = 1; i <= 11; i++) {
            servicio.registrar(TURISTA, texto("busqueda " + i));
            reloj.avanzar(Duration.ofSeconds(1));
        }

        List<BusquedaReciente> lista = servicio.listar(TURISTA);
        assertThat(historial.total()).isEqualTo(BusquedaReciente.MAXIMO_POR_TURISTA);
        assertThat(lista).hasSize(10);
        assertThat(lista.get(0).criterios().texto()).isEqualTo("busqueda 11");
        assertThat(lista.get(9).criterios().texto()).isEqualTo("busqueda 2");
    }

    @Test
    void unaBusquedaSinCriteriosNoSeGuarda() {
        servicio.registrar(TURISTA, new CriteriosGuardados(null, null, null, false, null, null, OrdenBusqueda.Nombre,
                null));

        assertThat(historial.total()).isZero();
    }

    @Test
    void unUpsertQueChocaConElIndiceUnicoSeReintentaComoActualizacion() {
        servicio.registrar(TURISTA, texto("Playa"));
        reloj.avanzar(Duration.ofMinutes(1));
        historial.simularCarrera(false);
        int antes = historial.actualizaciones();

        servicio.registrar(TURISTA, new CriteriosGuardados("Playa", Categoria.Tour, null, false, null, null, null,
                null));
        historial.simularCarrera(false);
        servicio.registrar(TURISTA, texto("playa"));

        assertThat(historial.actualizaciones() - antes).isEqualTo(2);
        assertThat(servicio.listar(TURISTA)).singleElement().satisfies(b -> {
            assertThat(b.criterios().texto()).isEqualTo("playa");
            assertThat(b.fecha()).isEqualTo(Instant.parse("2026-10-23T10:01:00.987Z"));
        });
    }

    @Test
    void siTambienFallaLaActualizacionNoEsUnError() {
        historial.simularCarrera(true);

        assertThatCode(() -> servicio.registrar(TURISTA, texto("Playa"))).doesNotThrowAnyException();
        assertThat(historial.total()).isZero();
    }

    @Test
    void unTuristaNoVeElHistorialDeOtro() {
        servicio.registrar(OTRO, texto("Playa"));

        assertThat(servicio.listar(TURISTA)).isEmpty();
        assertThat(servicio.listar(OTRO)).hasSize(1);
    }

    private static CriteriosGuardados texto(String texto) {
        return new CriteriosGuardados(texto, null, null, false, null, null, null, null);
    }
}
