package co.edu.unimagdalena.samaria360.booking.application.service;

import co.edu.unimagdalena.samaria360.booking.application.port.in.ConsultarDisponibilidadUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ResumirDisponibilidadUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.out.InventarioPort;
import co.edu.unimagdalena.samaria360.booking.application.port.out.RelojPort;
import co.edu.unimagdalena.samaria360.booking.domain.model.DisponibilidadServicio;
import co.edu.unimagdalena.samaria360.booking.domain.model.EstadoDisponibilidad;
import co.edu.unimagdalena.samaria360.booking.domain.model.HorarioReservas;
import co.edu.unimagdalena.samaria360.booking.domain.model.IdentificadorObjeto;
import co.edu.unimagdalena.samaria360.booking.domain.model.MotivoInvalido;
import co.edu.unimagdalena.samaria360.booking.domain.model.RangoFechas;
import co.edu.unimagdalena.samaria360.booking.domain.model.Reserva;
import co.edu.unimagdalena.samaria360.booking.domain.model.ValidadorCampos;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Disponibilidad pública (RF11): lee inventario_cupo. Antes asegura que la oferta del catálogo esté materializada en el
 * rango (DEC-152); solo entonces puede llamar a ms-catalog (enmienda A2 de ADR-007). Los datos inválidos se rechazan
 * antes, sin llamar al catálogo.
 */
public class DisponibilidadService implements ConsultarDisponibilidadUseCase, ResumirDisponibilidadUseCase {

    static final int DIAS_POR_DEFECTO_CONSULTA = 30;
    /** Hoy y los 7 días siguientes. */
    static final int DIAS_POR_DEFECTO_RESUMEN = 8;

    /** Un resumen ya validado: los servicios sin repetidos, en el orden pedido. */
    public record ConsultaResumen(List<String> servicios, RangoFechas rango) {
    }

    private final InventarioPort inventario;
    private final MaterializarInventarioService materializacion;
    private final RelojPort reloj;
    private final HorarioReservas horario;

    public DisponibilidadService(InventarioPort inventario, MaterializarInventarioService materializacion,
                                 RelojPort reloj, HorarioReservas horario) {
        this.inventario = Objects.requireNonNull(inventario);
        this.materializacion = Objects.requireNonNull(materializacion);
        this.reloj = Objects.requireNonNull(reloj);
        this.horario = Objects.requireNonNull(horario);
    }

    @Override
    public DisponibilidadServicio consultar(String idServicio, String idUnidad, String desde, String hasta) {
        ValidadorCampos validador = new ValidadorCampos();
        String servicio = validador.objectId("servicio", idServicio, true);
        String unidad = validador.objectId("unidad", idUnidad, false);
        RangoFechas rango = RangoFechas.validar(desde, hasta, HorarioReservas.hoy(reloj.ahora()),
                DIAS_POR_DEFECTO_CONSULTA, validador);
        validador.lanzarSiHayErrores();
        materializacion.asegurar(List.of(servicio), rango.hasta());
        return new DisponibilidadServicio(servicio, rango, inventario.consultar(servicio, unidad, rango), horario,
                Reserva.PLAZO_CANCELACION);
    }

    /**
     * Encadena las tres fases sin plazo. El adaptador de entrada recibe el decorador de infraestructura, que limita el
     * tiempo de {@link #materializarResumen} (DEC-155).
     */
    @Override
    public Map<String, EstadoDisponibilidad> resumir(List<String> idsServicios, String desde, String hasta) {
        ConsultaResumen consulta = validarResumen(idsServicios, desde, hasta);
        materializarResumen(consulta);
        return leerResumen(consulta);
    }

    public ConsultaResumen validarResumen(List<String> idsServicios, String desde, String hasta) {
        ValidadorCampos validador = new ValidadorCampos();
        Set<String> servicios = new LinkedHashSet<>();
        if (idsServicios != null) {
            idsServicios.stream().map(ValidadorCampos::texto).filter(Objects::nonNull).forEach(servicios::add);
        }
        if (servicios.isEmpty()) {
            validador.error("servicios", MotivoInvalido.REQUERIDO);
        } else if (servicios.size() > MAXIMO_SERVICIOS) {
            validador.error("servicios", MotivoInvalido.MAXIMO_ELEMENTOS);
        } else if (!servicios.stream().allMatch(IdentificadorObjeto::esValido)) {
            validador.error("servicios", MotivoInvalido.FORMATO);
        }
        RangoFechas rango = RangoFechas.validar(desde, hasta, HorarioReservas.hoy(reloj.ahora()),
                DIAS_POR_DEFECTO_RESUMEN, validador);
        validador.lanzarSiHayErrores();
        return new ConsultaResumen(List.copyOf(servicios), rango);
    }

    public void materializarResumen(ConsultaResumen consulta) {
        materializacion.asegurar(consulta.servicios(), consulta.rango().hasta());
    }

    public Map<String, EstadoDisponibilidad> leerResumen(ConsultaResumen consulta) {
        return inventario.resumir(consulta.servicios(), consulta.rango());
    }
}
