package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorAtractivo.DatosCoordenadas;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

/** CU «Actualizar atractivo» (RF07, RF24, RF27, RF58) y «Cambiar estado operativo» (RF33; DEC-206, DEC-207, DEC-210). */
class ActualizacionAtractivoTest {

    private static final UUID GESTOR = UUID.fromString("00000000-0000-4000-8000-000000000003");
    private static final Instant REGISTRO = Instant.parse("2026-10-01T10:00:00Z");
    private static final Instant AHORA = Instant.parse("2026-10-30T15:00:00Z");

    @Test
    void reemplazaLosDatosEditablesYConservaRegistroImagenesYEstado() {
        BorradorAtractivo borrador = new BorradorAtractivo("Cascada El Salto", "Cultura", "Nueva descripción",
                new DatosCoordenadas(-74.2, 11.2), 120, "En_mantenimiento", null, "Todos los días",
                List.of("Auditiva"));

        AtractivoPublico editado = ActualizacionAtractivo.aplicar(actual(), borrador, 4, AHORA);

        assertThat(editado.getNombre()).isEqualTo("Cascada El Salto");
        assertThat(editado.getCategoria()).contains("Cultura");
        assertThat(editado.getLimiteCapacidadCarga()).isEqualTo(120);
        assertThat(editado.getNormasAmbientales()).isEmpty();
        assertThat(editado.getHorarioAtencion()).contains("Todos los días");
        assertThat(editado.getCondicionesAccesibilidad()).containsExactly(CondicionAccesibilidad.Auditiva);
        assertThat(editado.getFechaActualizacion()).contains(AHORA);
        assertThat(editado.getVersion()).as("la versión leída").isEqualTo(4);
        // Lo que la edición no toca (DEC-206, DEC-209, DEC-210):
        assertThat(editado.getEstadoOperativo()).contains(EstadoOperativo.Abierto);
        assertThat(editado.getImagenes()).containsExactly("/imagenes/catalogo/cascada-el-salto.webp");
        assertThat(editado.getGestorId()).isEqualTo(GESTOR);
        assertThat(editado.getFechaRegistro()).contains(REGISTRO);
        assertThat(editado.getId()).contains("0000000000000000000000aa");
    }

    @Test
    void laEdicionExigeLaVersionYLasReglasDelRegistro() {
        BorradorAtractivo borrador = new BorradorAtractivo(" ", null, "ok", null, 0, null, null, null, null);

        Map<String, MotivoInvalido> campos = catchThrowableOfType(ContenidoInvalidoException.class,
                () -> ActualizacionAtractivo.aplicar(actual(), borrador, null, AHORA)).getCampos();

        assertThat(campos).containsOnly(
                Map.entry("version", MotivoInvalido.REQUERIDO),
                Map.entry("nombre", MotivoInvalido.REQUERIDO),
                Map.entry("categoria", MotivoInvalido.REQUERIDO),
                Map.entry("coordenadas", MotivoInvalido.REQUERIDO),
                Map.entry("limiteCapacidadCarga", MotivoInvalido.VALOR_MINIMO));
    }

    @Test
    void cambiaElEstadoConLaVersionLeida() {
        AtractivoPublico cerrado = ActualizacionAtractivo.cambiarEstado(actual(), "Cerrado_temporalmente", 4, AHORA);

        assertThat(cerrado.getEstadoOperativo()).contains(EstadoOperativo.Cerrado_temporalmente);
        assertThat(cerrado.getFechaActualizacion()).contains(AHORA);
        assertThat(cerrado.getVersion()).isEqualTo(4);
        assertThat(cerrado.getNombre()).isEqualTo(actual().getNombre());
    }

    @Test
    void elEstadoNuevoDebeSerUnoDeLosTresYDistintoDelActual() {
        assertThat(motivos("Cerrado", 4)).containsOnly(Map.entry("estadoNuevo", MotivoInvalido.NO_PERMITIDO));
        assertThat(motivos("Abierto", 4)).containsOnly(Map.entry("estadoNuevo", MotivoInvalido.NO_PERMITIDO));
        assertThat(motivos(null, -1)).containsOnly(
                Map.entry("estadoNuevo", MotivoInvalido.REQUERIDO), Map.entry("version", MotivoInvalido.VALOR_MINIMO));
    }

    private static Map<String, MotivoInvalido> motivos(String estado, Integer version) {
        return catchThrowableOfType(ContenidoInvalidoException.class,
                () -> ActualizacionAtractivo.cambiarEstado(actual(), estado, version, AHORA)).getCampos();
    }

    private static AtractivoPublico actual() {
        return new AtractivoPublico("0000000000000000000000aa", GESTOR, "Cascada", new Coordenada(-74.11, 11.15),
                "Sendero", "Naturaleza", List.of("/imagenes/catalogo/cascada-el-salto.webp"), 80,
                EstadoOperativo.Abierto, "No fogatas", Set.of(CondicionAccesibilidad.Visual), null, REGISTRO, null, 4);
    }
}
