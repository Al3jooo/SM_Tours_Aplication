package co.edu.unimagdalena.samaria360.auth.application.fake;

import co.edu.unimagdalena.samaria360.auth.application.port.out.DestinoCuentasPort;
import co.edu.unimagdalena.samaria360.auth.application.port.out.EntregaFallidaException;
import co.edu.unimagdalena.samaria360.auth.application.port.out.EntregaPendiente;
import co.edu.unimagdalena.samaria360.auth.domain.model.Destino;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Destinos que reciben las entregas, o fallan si están «caídos». */
public class DestinoCuentasFalso implements DestinoCuentasPort {

    public record Envio(Destino destino, EntregaPendiente entrega) {
    }

    private final Set<Destino> caidos = EnumSet.noneOf(Destino.class);
    private final List<Envio> envios = new ArrayList<>();

    public void caido(Destino destino) {
        caidos.add(destino);
    }

    public void levantar(Destino destino) {
        caidos.remove(destino);
    }

    public List<Envio> envios() {
        return envios;
    }

    public List<Envio> enviosA(Destino destino) {
        return envios.stream().filter(e -> e.destino() == destino).toList();
    }

    @Override
    public void enviar(Destino destino, EntregaPendiente entrega) {
        if (caidos.contains(destino)) {
            throw new EntregaFallidaException(destino.texto() + " no respondió");
        }
        envios.add(new Envio(destino, entrega));
    }
}
