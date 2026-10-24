package co.edu.unimagdalena.samaria360.catalog.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FavoritoTest {

    private static final UUID TURISTA = UUID.fromString("00000000-0000-4000-8000-000000000001");
    private static final Instant AHORA = Instant.parse("2026-10-23T10:00:00Z");

    @Test
    void unFavoritoReferenciaUnObjectIdEnTexto() {
        Favorito favorito = new Favorito(TURISTA, TipoOferta.Atractivo, "6a1e00000000000000000001", AHORA);

        assertThat(favorito.entidadId()).isEqualTo("6a1e00000000000000000001");
        assertThat(Favorito.MAXIMO_POR_TURISTA).isEqualTo(100);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"no-es-un-id", "6A1E00000000000000000001", "6a1e0000000000000000000"})
    void unaReferenciaQueNoEsUnObjectIdSeRechaza(String id) {
        assertThatThrownBy(() -> new Favorito(TURISTA, TipoOferta.Servicio, id, AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sinReferenciaNoTieneDatosYNoEstaDisponible() {
        FavoritoResuelto resuelto = FavoritoResuelto.sinReferencia(
                new Favorito(TURISTA, TipoOferta.Servicio, "6a1e00000000000000000001", AHORA));

        assertThat(resuelto.nombre()).isNull();
        assertThat(resuelto.categoria()).isNull();
        assertThat(resuelto.imagen()).isNull();
        assertThat(resuelto.disponible()).isFalse();
    }
}
