package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.RegistrarAtractivoUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.AlmacenImagenesPort;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.GestionAtractivosPort;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.RelojPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.AtractivoPublico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorAtractivo;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ImagenSubida;
import co.edu.unimagdalena.samaria360.catalog.domain.model.RegistroAtractivo;
import co.edu.unimagdalena.samaria360.catalog.domain.model.RegistroAtractivo.ImagenPorGuardar;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * CU «Registrar atractivo» (RF06; DEC-209, ADR-009). Orden: primero se validan los datos y las imágenes (400 sin
 * escribir nada), después se guardan las imágenes en GridFS y al final se inserta el atractivo.
 * <p>
 * Compensación: GridFS y la colección no comparten una transacción (el MongoDB de Compose no es un replica set). Si una
 * imagen o el insert fallan, se borran las imágenes ya guardadas y se relanza el error original; un borrado que también
 * falla se agrega como suprimido y deja un archivo huérfano (riesgo registrado en ADR-009).
 */
public class RegistrarAtractivoService implements RegistrarAtractivoUseCase {

    private final GestionAtractivosPort atractivos;
    private final AlmacenImagenesPort imagenes;
    private final RelojPort reloj;

    public RegistrarAtractivoService(GestionAtractivosPort atractivos, AlmacenImagenesPort imagenes, RelojPort reloj) {
        this.atractivos = Objects.requireNonNull(atractivos);
        this.imagenes = Objects.requireNonNull(imagenes);
        this.reloj = Objects.requireNonNull(reloj);
    }

    /** La fecha va en milisegundos, como la guarda MongoDB. */
    @Override
    public AtractivoPublico registrar(UUID gestorId, BorradorAtractivo borrador, List<ImagenSubida> subidas) {
        RegistroAtractivo.Registro registro = RegistroAtractivo.registrar(borrador, subidas, atractivos.nuevoId(),
                imagenes::nuevoId, gestorId, reloj.ahora().truncatedTo(ChronoUnit.MILLIS));
        List<String> guardadas = new ArrayList<>();
        try {
            for (ImagenPorGuardar imagen : registro.imagenes()) {
                imagenes.guardar(imagen.id(), imagen.formato(), imagen.nombre(), imagen.contenido());
                guardadas.add(imagen.id());
            }
            atractivos.insertar(registro.atractivo());
        } catch (RuntimeException e) {
            compensar(guardadas, e);
            throw e;
        }
        return registro.atractivo();
    }

    private void compensar(List<String> guardadas, RuntimeException causa) {
        for (String id : guardadas) {
            try {
                imagenes.borrar(id);
            } catch (RuntimeException e) {
                causa.addSuppressed(e);
            }
        }
    }
}
