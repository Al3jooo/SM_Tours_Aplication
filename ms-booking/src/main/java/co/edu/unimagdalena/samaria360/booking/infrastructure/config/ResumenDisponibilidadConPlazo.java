package co.edu.unimagdalena.samaria360.booking.infrastructure.config;

import co.edu.unimagdalena.samaria360.booking.application.port.in.ResumirDisponibilidadUseCase;
import co.edu.unimagdalena.samaria360.booking.application.service.DisponibilidadService;
import co.edu.unimagdalena.samaria360.booking.application.service.DisponibilidadService.ConsultaResumen;
import co.edu.unimagdalena.samaria360.booking.domain.exception.CatalogoNoDisponibleException;
import co.edu.unimagdalena.samaria360.booking.domain.model.EstadoDisponibilidad;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Resumen de las tarjetas con un plazo total para materializar (DEC-155). Hasta 10 servicios sin materializar son hasta
 * 10 llamadas al catálogo, y una sola puede tardar ≈ 4 s (ADR-007): sin plazo, el gateway cortaría a los 5 s.
 * <ol>
 *   <li>valida en el hilo de la petición: los datos inválidos no llegan al catálogo;</li>
 *   <li>materializa en un ejecutor acotado y espera como mucho el plazo. Si se agota, o el ejecutor está lleno,
 *   responde 503 {@code SERVICIO_NO_DISPONIBLE}: nunca un {@code SIN_INVENTARIO} falso. La tarea que quedó corriendo
 *   termina sola (la acotan los timeouts HTTP) y lo que inserta es idempotente: adelanta la siguiente petición;</li>
 *   <li>lee el resumen.</li>
 * </ol>
 */
public class ResumenDisponibilidadConPlazo implements ResumirDisponibilidadUseCase, AutoCloseable {

    private static final int HILOS = 4;
    private static final int EN_ESPERA = 16;

    private final DisponibilidadService servicio;
    private final Duration plazo;
    private final ThreadPoolExecutor ejecutor;

    public ResumenDisponibilidadConPlazo(DisponibilidadService servicio, Duration plazo) {
        this.servicio = servicio;
        this.plazo = plazo;
        AtomicInteger numero = new AtomicInteger();
        this.ejecutor = new ThreadPoolExecutor(HILOS, HILOS, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(EN_ESPERA), tarea -> {
                    Thread hilo = new Thread(tarea, "resumen-materializacion-" + numero.incrementAndGet());
                    hilo.setDaemon(true);
                    return hilo;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    @Override
    public Map<String, EstadoDisponibilidad> resumir(List<String> idsServicios, String desde, String hasta) {
        ConsultaResumen consulta = servicio.validarResumen(idsServicios, desde, hasta);
        Future<?> materializacion;
        try {
            materializacion = ejecutor.submit(() -> servicio.materializarResumen(consulta));
        } catch (RejectedExecutionException lleno) {
            throw new CatalogoNoDisponibleException("Demasiadas materializaciones en curso", lleno);
        }
        try {
            materializacion.get(plazo.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException agotado) {
            throw new CatalogoNoDisponibleException("La materialización del resumen superó " + plazo, agotado);
        } catch (InterruptedException interrumpido) {
            Thread.currentThread().interrupt();
            throw new CatalogoNoDisponibleException("Se interrumpió la materialización del resumen", interrumpido);
        } catch (ExecutionException fallo) {
            if (fallo.getCause() instanceof RuntimeException error) {
                throw error;
            }
            throw new IllegalStateException(fallo.getCause());
        }
        return servicio.leerResumen(consulta);
    }

    /** Spring lo llama al cerrar el contexto (método de destrucción inferido). */
    @Override
    public void close() {
        ejecutor.shutdownNow();
    }
}
