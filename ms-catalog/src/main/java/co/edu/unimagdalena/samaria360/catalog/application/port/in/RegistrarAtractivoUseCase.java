package co.edu.unimagdalena.samaria360.catalog.application.port.in;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.AtractivoPublico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorAtractivo;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ImagenSubida;

import java.util.List;
import java.util.UUID;

/**
 * CU «Registrar atractivo» (RF06, RF07, RF24, RF27, RF33, RF58), con «Cargar imágenes» (DEC-209). El atractivo queda
 * publicado al registrarse (DEC-87). El rol Gestor lo exige la infraestructura.
 */
public interface RegistrarAtractivoUseCase {

    /** @throws ContenidoInvalidoException con un motivo por cada campo o imagen inválidos */
    AtractivoPublico registrar(UUID gestorId, BorradorAtractivo borrador, List<ImagenSubida> imagenes);
}
