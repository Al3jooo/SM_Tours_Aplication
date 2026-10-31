package co.edu.unimagdalena.samaria360.catalog.domain.service;

import co.edu.unimagdalena.samaria360.catalog.domain.model.FormatoImagen;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ImagenSubida;
import co.edu.unimagdalena.samaria360.catalog.domain.model.MotivoInvalido;

import java.util.Optional;

/**
 * Reglas de las imágenes de un atractivo (RF06; DEC-55, DEC-209, ADR-009): de 1 a 5 por recurso, JPEG o PNG
 * reconocidos por su firma y 5 MB (5 × 1024 × 1024 bytes) como máximo cada una. El tope de la petición multipart lo
 * pone la infraestructura un poco por encima, para que una imagen de más llegue aquí y reciba su motivo por campo.
 */
public final class PoliticaImagen {

    public static final int MINIMO = 1;
    public static final int MAXIMO = 5;
    public static final long TAMANO_MAXIMO = 5L * 1024 * 1024;

    private PoliticaImagen() {
    }

    /** Motivo del campo {@code imagenes} por la cantidad; vacío si está entre 1 y 5. */
    public static Optional<MotivoInvalido> motivoCantidad(int cantidad) {
        if (cantidad < MINIMO) {
            return Optional.of(cantidad == 0 ? MotivoInvalido.REQUERIDO : MotivoInvalido.CANTIDAD_MINIMA);
        }
        return cantidad > MAXIMO ? Optional.of(MotivoInvalido.CANTIDAD_MAXIMA) : Optional.empty();
    }

    /** Motivo de una imagen ({@code imagenes[i]}): vacía, más grande que el tope o sin firma de JPEG ni PNG. */
    public static Optional<MotivoInvalido> motivo(ImagenSubida imagen) {
        byte[] contenido = imagen == null ? null : imagen.contenido();
        if (contenido == null || contenido.length == 0) {
            return Optional.of(MotivoInvalido.REQUERIDO);
        }
        if (contenido.length > TAMANO_MAXIMO) {
            return Optional.of(MotivoInvalido.TAMANO_MAXIMO);
        }
        return FormatoImagen.detectar(contenido).isPresent() ? Optional.empty() : Optional.of(MotivoInvalido.FORMATO);
    }
}
