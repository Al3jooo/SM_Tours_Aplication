package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.fake.AlmacenImagenesEnMemoria;
import co.edu.unimagdalena.samaria360.catalog.application.fake.GestionAtractivosEnMemoria;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.AtractivoNoEncontradoException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.ImagenNoEncontradaException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.AtractivoPublico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Coordenada;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EstadoOperativo;
import co.edu.unimagdalena.samaria360.catalog.domain.model.FormatoImagen;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ImagenAlmacenada;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Detalle público de un atractivo (DEC-217) y sus imágenes (DEC-209). */
class ConsultarAtractivoServiceTest {

    private static final String ID = "0000000000000000000000aa";

    private final GestionAtractivosEnMemoria atractivos = new GestionAtractivosEnMemoria();
    private final AlmacenImagenesEnMemoria imagenes = new AlmacenImagenesEnMemoria();

    @Test
    void unAtractivoEsVisibleMientrasExistaEnCualquierEstado() {
        atractivos.agregar(new AtractivoPublico(ID, UUID.randomUUID(), "Mirador", new Coordenada(-74.196, 11.264),
                null, "Naturaleza", List.of(), 100, EstadoOperativo.En_mantenimiento, null, Set.of()));

        assertThat(new ConsultarAtractivoService(atractivos).detalle(ID).getNombre()).isEqualTo("Mirador");
    }

    @Test
    void unIdInvalidoOInexistenteEs404() {
        ConsultarAtractivoService servicio = new ConsultarAtractivoService(atractivos);

        assertThatThrownBy(() -> servicio.detalle("abc")).isInstanceOf(AtractivoNoEncontradoException.class);
        assertThatThrownBy(() -> servicio.detalle(ID)).isInstanceOf(AtractivoNoEncontradoException.class);
    }

    @Test
    void sirveUnaImagenGuardadaYUnaInexistenteEs404() {
        imagenes.agregar(ID, new ImagenAlmacenada(FormatoImagen.PNG, new byte[]{1, 2}));
        ConsultarImagenService servicio = new ConsultarImagenService(imagenes);

        assertThat(servicio.buscar(ID).formato()).isEqualTo(FormatoImagen.PNG);
        assertThatThrownBy(() -> servicio.buscar("0000000000000000000000bb"))
                .isInstanceOf(ImagenNoEncontradaException.class);
        assertThatThrownBy(() -> servicio.buscar("../etc")).isInstanceOf(ImagenNoEncontradaException.class);
    }
}
