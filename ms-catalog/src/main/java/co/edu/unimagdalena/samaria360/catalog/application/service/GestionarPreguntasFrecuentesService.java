package co.edu.unimagdalena.samaria360.catalog.application.service;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.GestionarPreguntasFrecuentesUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.PreguntaFrecuenteRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.RelojPort;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.LimitePreguntasException;
import co.edu.unimagdalena.samaria360.catalog.domain.exception.PreguntaNoEncontradaException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.IdentificadorObjeto;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PreguntaFrecuente;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

/**
 * CU «Gestionar FAQs» (RF26, RF47). Un id que no es un ObjectId tampoco existe: 404, igual que uno inexistente.
 * <p>
 * El tope de {@value PreguntaFrecuente#MAXIMO_PREGUNTAS} (DEC-62) se comprueba contando antes de insertar, sin
 * atomicidad: dos creaciones simultáneas pueden dejar una pregunta de más. Se acepta porque el único actor es el
 * Administrador y el listado público sigue completo.
 */
public class GestionarPreguntasFrecuentesService implements GestionarPreguntasFrecuentesUseCase {

    private final PreguntaFrecuenteRepositoryPort preguntas;
    private final RelojPort reloj;

    public GestionarPreguntasFrecuentesService(PreguntaFrecuenteRepositoryPort preguntas, RelojPort reloj) {
        this.preguntas = Objects.requireNonNull(preguntas);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public List<PreguntaFrecuente> listar() {
        return preguntas.listar();
    }

    @Override
    public PreguntaFrecuente crear(String titulo, String cuerpo) {
        PreguntaFrecuente.validar(titulo, cuerpo);
        if (preguntas.contar() >= PreguntaFrecuente.MAXIMO_PREGUNTAS) {
            throw new LimitePreguntasException();
        }
        PreguntaFrecuente pregunta = PreguntaFrecuente.de(preguntas.nuevoId(), titulo, cuerpo, ahora());
        preguntas.insertar(pregunta);
        return pregunta;
    }

    @Override
    public PreguntaFrecuente editar(String id, String titulo, String cuerpo) {
        PreguntaFrecuente.validar(titulo, cuerpo);
        if (!IdentificadorObjeto.esValido(id)) {
            throw new PreguntaNoEncontradaException(id);
        }
        PreguntaFrecuente pregunta = PreguntaFrecuente.de(id, titulo, cuerpo, ahora());
        if (!preguntas.reemplazar(pregunta)) {
            throw new PreguntaNoEncontradaException(id);
        }
        return pregunta;
    }

    @Override
    public void eliminar(String id) {
        if (!IdentificadorObjeto.esValido(id) || !preguntas.eliminar(id)) {
            throw new PreguntaNoEncontradaException(id);
        }
    }

    /** MongoDB guarda milisegundos: la respuesta lleva la misma fecha que devolverá el listado. */
    private Instant ahora() {
        return reloj.ahora().truncatedTo(ChronoUnit.MILLIS);
    }
}
