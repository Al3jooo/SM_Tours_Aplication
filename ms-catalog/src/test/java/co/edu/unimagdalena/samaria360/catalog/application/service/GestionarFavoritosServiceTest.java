package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.fake.FavoritoRepositoryEnMemoria;
import co.edu.unimagdalena.samaria360.catalog.application.fake.OfertaReferenciadaEnMemoria;
import co.edu.unimagdalena.samaria360.catalog.application.fake.RelojFijo;
import co.edu.unimagdalena.samaria360.catalog.application.port.in.GestionarFavoritosUseCase.Guardado;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.OfertaReferenciadaPort.Referencia;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.LimiteFavoritosException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.OfertaNoDisponibleException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Favorito;
import co.edu.unimagdalena.samaria360.catalog.domain.model.FavoritoResuelto;
import co.edu.unimagdalena.samaria360.catalog.domain.model.TipoOferta;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** CU «Guardar favoritos» (RF54; DEC-80, DEC-86) con dobles de los puertos. */
class GestionarFavoritosServiceTest {

    private static final UUID TURISTA = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final UUID OTRO = UUID.fromString("00000000-0000-4000-8000-000000000009");
    private static final String SERVICIO = "6a0000000000000000000001";
    private static final String ATRACTIVO = "6a0000000000000000000002";
    private static final Instant AHORA = Instant.parse("2026-10-23T10:00:00.123456Z");

    private final FavoritoRepositoryEnMemoria favoritos = new FavoritoRepositoryEnMemoria();
    private final OfertaReferenciadaEnMemoria referencias = new OfertaReferenciadaEnMemoria();
    private final RelojFijo reloj = new RelojFijo(AHORA);
    private final GestionarFavoritosService servicio = new GestionarFavoritosService(favoritos, referencias, reloj);

    GestionarFavoritosServiceTest() {
        referencias.agregar(TipoOferta.Servicio, SERVICIO, new Referencia("Hotel", "Alojamiento", "/a.webp", true));
        referencias.agregar(TipoOferta.Atractivo, ATRACTIVO, new Referencia("Cascada", "Naturaleza", null, true));
    }

    @Test
    void guardarCreaElFavoritoConLaFechaEnMilisegundos() {
        Guardado guardado = servicio.guardar(TURISTA, TipoOferta.Servicio, SERVICIO);

        assertThat(guardado.creado()).isTrue();
        assertThat(guardado.favorito().nombre()).isEqualTo("Hotel");
        assertThat(guardado.favorito().disponible()).isTrue();
        assertThat(favoritos.todos()).singleElement().satisfies(f ->
                assertThat(f.fechaCreacion()).isEqualTo(Instant.parse("2026-10-23T10:00:00.123Z")));
    }

    @Test
    void guardarDosVecesNoDuplicaYLaSegundaNoEsCreacion() {
        servicio.guardar(TURISTA, TipoOferta.Atractivo, ATRACTIVO);
        reloj.avanzar(Duration.ofMinutes(5));

        Guardado repetido = servicio.guardar(TURISTA, TipoOferta.Atractivo, ATRACTIVO);

        assertThat(repetido.creado()).isFalse();
        assertThat(repetido.favorito().favorito().fechaCreacion()).isEqualTo(Instant.parse("2026-10-23T10:00:00.123Z"));
        assertThat(favoritos.todos()).hasSize(1);
    }

    @Test
    void unPutSimultaneoQueChocaConElIndiceUnicoDevuelveElExistente() {
        Favorito ganador = new Favorito(TURISTA, TipoOferta.Servicio, SERVICIO, Instant.parse("2026-10-23T09:59:59Z"));
        favoritos.simularCarrera(ganador);

        Guardado guardado = servicio.guardar(TURISTA, TipoOferta.Servicio, SERVICIO);

        assertThat(guardado.creado()).isFalse();
        assertThat(guardado.favorito().favorito()).isEqualTo(ganador);
        assertThat(favoritos.todos()).containsExactly(ganador);
    }

    @Test
    void unaReferenciaInvalidaInexistenteONoVisibleEs404() {
        referencias.agregar(TipoOferta.Servicio, "6a0000000000000000000003", new Referencia("Oculto", "Tour", null,
                false));

        for (String id : List.of("no-es-un-id", "6a00000000000000000000ff", "6a0000000000000000000003")) {
            assertThatThrownBy(() -> servicio.guardar(TURISTA, TipoOferta.Servicio, id))
                    .isInstanceOf(OfertaNoDisponibleException.class);
        }
        assertThatThrownBy(() -> servicio.guardar(TURISTA, TipoOferta.Atractivo, SERVICIO))
                .as("el id de un servicio no es un atractivo").isInstanceOf(OfertaNoDisponibleException.class);
        assertThat(favoritos.todos()).isEmpty();
    }

    @Test
    void conCienFavoritosElSiguienteEs409PeroUnoRepetidoSigueSiendo200() {
        for (int i = 0; i < Favorito.MAXIMO_POR_TURISTA; i++) {
            favoritos.agregar(new Favorito(TURISTA, TipoOferta.Atractivo, String.format("%024x", i + 100),
                    AHORA.minusSeconds(i)));
        }
        favoritos.agregar(new Favorito(OTRO, TipoOferta.Servicio, SERVICIO, AHORA));

        assertThatThrownBy(() -> servicio.guardar(TURISTA, TipoOferta.Servicio, SERVICIO))
                .isInstanceOf(LimiteFavoritosException.class);
        assertThat(servicio.guardar(TURISTA, TipoOferta.Atractivo, String.format("%024x", 100)).creado()).isFalse();
        assertThat(servicio.guardar(OTRO, TipoOferta.Atractivo, ATRACTIVO).creado()).as("el tope es por turista")
                .isTrue();
    }

    @Test
    void listarResuelveDelMasRecienteAlMasAntiguoYMarcaLosNoDisponibles() {
        servicio.guardar(TURISTA, TipoOferta.Servicio, SERVICIO);
        reloj.avanzar(Duration.ofMinutes(1));
        servicio.guardar(TURISTA, TipoOferta.Atractivo, ATRACTIVO);
        referencias.agregar(TipoOferta.Servicio, SERVICIO, new Referencia("Hotel", "Alojamiento", "/a.webp", false));
        int antes = referencias.consultas();

        List<FavoritoResuelto> lista = servicio.listar(TURISTA);

        assertThat(lista).extracting(r -> r.favorito().entidadId()).containsExactly(ATRACTIVO, SERVICIO);
        assertThat(lista.get(1).disponible()).as("despublicado o de un prestador inactivo").isFalse();
        assertThat(lista.get(1).nombre()).isEqualTo("Hotel");
        assertThat(referencias.consultas() - antes).as("una consulta por tipo").isEqualTo(2);
    }

    @Test
    void unFavoritoCuyaReferenciaYaNoExisteSeListaSinDatos() {
        servicio.guardar(TURISTA, TipoOferta.Servicio, SERVICIO);
        referencias.quitar(TipoOferta.Servicio, SERVICIO);

        assertThat(servicio.listar(TURISTA)).singleElement().satisfies(r -> {
            assertThat(r.disponible()).isFalse();
            assertThat(r.nombre()).isNull();
        });
        assertThat(servicio.guardar(TURISTA, TipoOferta.Servicio, SERVICIO).creado())
                .as("ya guardado: se acepta aunque ya no esté").isFalse();
    }

    @Test
    void unTuristaNoVeNiQuitaLosFavoritosDeOtro() {
        servicio.guardar(OTRO, TipoOferta.Servicio, SERVICIO);

        servicio.quitar(TURISTA, TipoOferta.Servicio, SERVICIO);

        assertThat(servicio.listar(TURISTA)).isEmpty();
        assertThat(servicio.listar(OTRO)).hasSize(1);
    }

    @Test
    void quitarEsIdempotente() {
        servicio.guardar(TURISTA, TipoOferta.Servicio, SERVICIO);

        servicio.quitar(TURISTA, TipoOferta.Servicio, SERVICIO);
        servicio.quitar(TURISTA, TipoOferta.Servicio, SERVICIO);
        servicio.quitar(TURISTA, TipoOferta.Servicio, "no-es-un-id");

        assertThat(favoritos.todos()).isEmpty();
    }
}
