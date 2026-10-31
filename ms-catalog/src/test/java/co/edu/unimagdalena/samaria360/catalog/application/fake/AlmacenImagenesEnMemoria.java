package co.edu.unimagdalena.samaria360.catalog.application.fake;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.AlmacenImagenesPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.FormatoImagen;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ImagenAlmacenada;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * GridFS en memoria (ADR-009). {@link #fallarAlGuardarLaNumero(int, RuntimeException)} y
 * {@link #fallarAlBorrar(RuntimeException)} simulan los fallos que obligan a compensar.
 */
public class AlmacenImagenesEnMemoria implements AlmacenImagenesPort {

    private final Map<String, ImagenAlmacenada> imagenes = new LinkedHashMap<>();
    private int siguienteId = 1;
    private int guardadas;
    private int fallarEn = -1;
    private RuntimeException errorAlGuardar;
    private RuntimeException errorAlBorrar;

    /** @param numero 1 para la primera imagen que se intente guardar */
    public void fallarAlGuardarLaNumero(int numero, RuntimeException error) {
        this.fallarEn = numero;
        this.errorAlGuardar = error;
    }

    public void fallarAlBorrar(RuntimeException error) {
        this.errorAlBorrar = error;
    }

    public Set<String> ids() {
        return Set.copyOf(imagenes.keySet());
    }

    public void agregar(String id, ImagenAlmacenada imagen) {
        imagenes.put(id, imagen);
    }

    @Override
    public String nuevoId() {
        return String.format("%024x", 0xA000 + siguienteId++);
    }

    @Override
    public void guardar(String id, FormatoImagen formato, String nombre, byte[] contenido) {
        guardadas++;
        if (guardadas == fallarEn) {
            throw errorAlGuardar;
        }
        imagenes.put(id, new ImagenAlmacenada(formato, contenido));
    }

    @Override
    public void borrar(String id) {
        if (errorAlBorrar != null) {
            throw errorAlBorrar;
        }
        imagenes.remove(id);
    }

    @Override
    public Optional<ImagenAlmacenada> buscar(String id) {
        return Optional.ofNullable(imagenes.get(id));
    }
}
