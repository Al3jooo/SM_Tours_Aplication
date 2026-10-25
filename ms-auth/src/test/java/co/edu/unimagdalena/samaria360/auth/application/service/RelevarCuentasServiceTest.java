package co.edu.unimagdalena.samaria360.auth.application.service;

import co.edu.unimagdalena.samaria360.auth.application.fake.DestinoCuentasFalso;
import co.edu.unimagdalena.samaria360.auth.application.fake.OutboxEnMemoria;
import co.edu.unimagdalena.samaria360.auth.application.port.out.RelojPort;
import co.edu.unimagdalena.samaria360.auth.domain.model.Destino;
import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoAdministrativo;
import co.edu.unimagdalena.samaria360.auth.domain.model.EventoCuentaActualizada;
import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;
import co.edu.unimagdalena.samaria360.auth.domain.service.PoliticaReintento;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Relevo del outbox (ADR-008) con dobles del outbox y de los destinos. */
class RelevarCuentasServiceTest {

    private static final Instant T0 = Instant.parse("2030-01-10T15:00:00Z");
    private static final UUID USUARIO = UUID.fromString("00000000-0000-4000-8000-0000000000a2");
    private static final UUID OTRO = UUID.fromString("00000000-0000-4000-8000-0000000000a3");

    private final OutboxEnMemoria outbox = new OutboxEnMemoria();
    private final DestinoCuentasFalso destinos = new DestinoCuentasFalso();
    private final Reloj reloj = new Reloj(T0);
    private final RelevarCuentasService relevo =
            new RelevarCuentasService(outbox, destinos, reloj, new PoliticaReintento());

    private static final class Reloj implements RelojPort {
        private Instant ahora;

        Reloj(Instant ahora) {
            this.ahora = ahora;
        }

        void avanzar(Duration duracion) {
            ahora = ahora.plus(duracion);
        }

        @Override
        public Instant ahora() {
            return ahora;
        }
    }

    private void evento(UUID usuario, EstadoAdministrativo estado, long version) {
        outbox.registrar(new EventoCuentaActualizada(usuario, Rol.Turista, estado, version, null, reloj.ahora()),
                List.of(Destino.values()));
    }

    @Test
    void entregaElEventoACadaDestinoPorSeparadoYLoMarcaEntregado() {
        evento(USUARIO, EstadoAdministrativo.Desactivado, 1);

        assertThat(relevo.relevar(Destino.MS_CATALOG)).isEqualTo(1);
        assertThat(outbox.entrega(Destino.MS_BOOKING, 1).entregadoEn).isNull();
        assertThat(relevo.relevar(Destino.MS_BOOKING)).isEqualTo(1);

        assertThat(destinos.enviosA(Destino.MS_CATALOG)).singleElement().satisfies(envio -> {
            assertThat(envio.entrega().idUsuario()).isEqualTo(USUARIO);
            assertThat(envio.entrega().estadoCuenta()).isEqualTo(EstadoAdministrativo.Desactivado);
            assertThat(envio.entrega().version()).isEqualTo(1);
        });
        assertThat(destinos.enviosA(Destino.MS_BOOKING)).hasSize(1);
        assertThat(outbox.entregas()).allSatisfy(e -> assertThat(e.entregadoEn).isNotNull());
    }

    @Test
    void sinPendientesNoHaceNada() {
        assertThat(relevo.relevar(Destino.MS_CATALOG)).isZero();
        assertThat(destinos.envios()).isEmpty();
    }

    @Test
    void unaEntregaFallidaSeReintentaConEsperaCrecienteYNoSeVuelveAIntentarAntes() {
        evento(USUARIO, EstadoAdministrativo.Desactivado, 1);
        destinos.caido(Destino.MS_BOOKING);

        assertThat(relevo.relevar(Destino.MS_BOOKING)).isZero();
        OutboxEnMemoria.Entrega entrega = outbox.entrega(Destino.MS_BOOKING, 1);
        assertThat(entrega.intentos).isEqualTo(1);
        assertThat(entrega.ultimoError).contains("ms-booking");
        assertThat(entrega.proximoIntento).isEqualTo(T0.plusSeconds(2));

        // Antes de que venza la espera no se reintenta.
        reloj.avanzar(Duration.ofSeconds(1));
        assertThat(relevo.relevar(Destino.MS_BOOKING)).isZero();
        assertThat(entrega.intentos).isEqualTo(1);

        // Vencida la espera, falla de nuevo y la espera crece.
        reloj.avanzar(Duration.ofSeconds(1));
        assertThat(relevo.relevar(Destino.MS_BOOKING)).isZero();
        assertThat(entrega.intentos).isEqualTo(2);
        assertThat(entrega.proximoIntento).isEqualTo(reloj.ahora().plusSeconds(4));

        // Cuando el destino vuelve, la entrega sale y queda marcada.
        destinos.levantar(Destino.MS_BOOKING);
        reloj.avanzar(Duration.ofSeconds(4));
        assertThat(relevo.relevar(Destino.MS_BOOKING)).isEqualTo(1);
        assertThat(entrega.entregadoEn).isNotNull();
    }

    @Test
    void unDestinoCaidoNoBloqueaAlOtro() {
        evento(USUARIO, EstadoAdministrativo.Desactivado, 1);
        destinos.caido(Destino.MS_BOOKING);

        assertThat(relevo.relevar(Destino.MS_BOOKING)).isZero();
        assertThat(relevo.relevar(Destino.MS_CATALOG)).isEqualTo(1);

        assertThat(outbox.entrega(Destino.MS_CATALOG, 1).entregadoEn).isNotNull();
        assertThat(outbox.entrega(Destino.MS_BOOKING, 1).entregadoEn).isNull();
    }

    @Test
    void soloSeEntregaLaUltimaVersionPendienteDeCadaUsuarioYLasAnterioresQuedanEntregadas() {
        evento(USUARIO, EstadoAdministrativo.Desactivado, 1);
        evento(USUARIO, EstadoAdministrativo.Activo, 2);

        assertThat(relevo.relevar(Destino.MS_CATALOG)).isEqualTo(1);

        assertThat(destinos.enviosA(Destino.MS_CATALOG)).singleElement()
                .satisfies(envio -> assertThat(envio.entrega().version()).isEqualTo(2));
        assertThat(outbox.entrega(Destino.MS_CATALOG, 1).entregadoEn).isNotNull();
        assertThat(outbox.entrega(Destino.MS_CATALOG, 2).entregadoEn).isNotNull();
    }

    @Test
    void anteElPrimerFalloLasQueNoSeIntentaronVuelvenALaColaSinContarIntento() {
        evento(USUARIO, EstadoAdministrativo.Desactivado, 1);
        evento(OTRO, EstadoAdministrativo.Desactivado, 1);
        destinos.caido(Destino.MS_CATALOG);

        assertThat(relevo.relevar(Destino.MS_CATALOG)).isZero();

        OutboxEnMemoria.Entrega primera = outbox.entregas().stream()
                .filter(e -> e.destino == Destino.MS_CATALOG && e.evento.idUsuario().equals(USUARIO)).findFirst().orElseThrow();
        OutboxEnMemoria.Entrega segunda = outbox.entregas().stream()
                .filter(e -> e.destino == Destino.MS_CATALOG && e.evento.idUsuario().equals(OTRO)).findFirst().orElseThrow();
        assertThat(primera.intentos).isEqualTo(1);
        assertThat(segunda.intentos).isZero();
        assertThat(segunda.proximoIntento).isEqualTo(reloj.ahora());
    }

    @Test
    void mientrasUnaEntregaTieneArriendoNoSeReclamaDeNuevo() {
        evento(USUARIO, EstadoAdministrativo.Desactivado, 1);

        var reclamadas = outbox.reclamar(Destino.MS_CATALOG, T0, Duration.ofSeconds(30), 50);
        assertThat(reclamadas).hasSize(1);

        assertThat(relevo.relevar(Destino.MS_CATALOG)).isZero();
        assertThat(destinos.envios()).isEmpty();

        reloj.avanzar(Duration.ofSeconds(31));
        assertThat(relevo.relevar(Destino.MS_CATALOG)).isEqualTo(1);
    }
}
