package co.edu.unimagdalena.samaria360.booking.application.fake;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Registro común del orden en que los dobles reciben las llamadas (p. ej. catálogo antes que la base). */
public final class Eventos {

    private final List<String> lista = Collections.synchronizedList(new ArrayList<>());

    public void registrar(String evento) {
        lista.add(evento);
    }

    public List<String> todos() {
        return List.copyOf(lista);
    }
}
