package co.edu.unimagdalena.samaria360.catalog.application.fake;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.VisibilidadPrestadorPort;

import java.util.ArrayList;
import java.util.List;

/** Registra las llamadas a {@code servicios.prestadorActivo}. */
public class VisibilidadPrestadorFalsa implements VisibilidadPrestadorPort {

    public record Llamada(String idPrestador, boolean activo) {
    }

    private final List<Llamada> llamadas = new ArrayList<>();
    private RuntimeException fallar;

    public List<Llamada> llamadas() {
        return llamadas;
    }

    public void fallar(RuntimeException error) {
        this.fallar = error;
    }

    @Override
    public void fijarPrestadorActivo(String idPrestador, boolean activo) {
        if (fallar != null) {
            throw fallar;
        }
        llamadas.add(new Llamada(idPrestador, activo));
    }
}
