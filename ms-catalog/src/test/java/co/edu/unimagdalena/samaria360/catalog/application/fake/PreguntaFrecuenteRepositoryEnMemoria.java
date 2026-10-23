package co.edu.unimagdalena.samaria360.catalog.application.fake;

import co.edu.unimagdalena.samaria360.catalog.application.port.out.PreguntaFrecuenteRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PreguntaFrecuente;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Colección {@code faq} en memoria, con el orden de la colación {@code es} nivel 1 (sin mayúsculas ni tildes). */
public class PreguntaFrecuenteRepositoryEnMemoria implements PreguntaFrecuenteRepositoryPort {

    private final Map<String, PreguntaFrecuente> preguntas = new LinkedHashMap<>();
    private long secuencia;

    public void agregar(PreguntaFrecuente pregunta) {
        preguntas.put(pregunta.id(), pregunta);
    }

    public int insertadas() {
        return preguntas.size();
    }

    @Override
    public String nuevoId() {
        return String.format("%024x", ++secuencia);
    }

    @Override
    public List<PreguntaFrecuente> listar() {
        Collator espanol = Collator.getInstance(Locale.forLanguageTag("es"));
        espanol.setStrength(Collator.PRIMARY);
        List<PreguntaFrecuente> lista = new ArrayList<>(preguntas.values());
        lista.sort(Comparator.comparing(PreguntaFrecuente::titulo, espanol).thenComparing(PreguntaFrecuente::id));
        return lista;
    }

    @Override
    public long contar() {
        return preguntas.size();
    }

    @Override
    public void insertar(PreguntaFrecuente pregunta) {
        preguntas.put(pregunta.id(), pregunta);
    }

    @Override
    public boolean reemplazar(PreguntaFrecuente pregunta) {
        return preguntas.replace(pregunta.id(), pregunta) != null;
    }

    @Override
    public boolean eliminar(String id) {
        return preguntas.remove(id) != null;
    }
}
