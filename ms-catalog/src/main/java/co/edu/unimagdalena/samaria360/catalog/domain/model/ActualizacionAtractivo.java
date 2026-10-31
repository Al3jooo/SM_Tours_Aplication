package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.RegistroAtractivo.Datos;

import java.time.Instant;
import java.util.Objects;

/**
 * CU «Actualizar atractivo» (RF07, RF24, RF27, RF58; DEC-206) y CU «Cambiar estado operativo» (RF33; DEC-210). El
 * resultado es el atractivo con los cambios y la versión que el Gestor leyó: la escritura es condicional sobre esa
 * versión y la sube en uno (DEC-207, como DEC-186). La edición no toca el registro (Gestor y fecha), las imágenes (su
 * edición se difiere, DEC-209) ni el estado, que tiene su propio flujo.
 */
public final class ActualizacionAtractivo {

    private final CamposServicio campos = new CamposServicio();

    private ActualizacionAtractivo() {
    }

    /**
     * @param version la versión del atractivo que el Gestor leyó
     * @throws ContenidoInvalidoException con un motivo por cada campo inválido
     */
    public static AtractivoPublico aplicar(AtractivoPublico atractivo, BorradorAtractivo borrador, Integer version,
                                           Instant fecha) {
        return new ActualizacionAtractivo().editar(Objects.requireNonNull(atractivo),
                borrador == null ? RegistroAtractivo.vacio() : borrador, version, fecha);
    }

    /**
     * RF33: cualquiera de los tres estados, salvo el que ya tiene ({@code campos.estadoNuevo: NO_PERMITIDO}).
     *
     * @throws ContenidoInvalidoException si el estado o la versión no sirven
     */
    public static AtractivoPublico cambiarEstado(AtractivoPublico atractivo, String estadoNuevo, Integer version,
                                                 Instant fecha) {
        return new ActualizacionAtractivo().estado(Objects.requireNonNull(atractivo), estadoNuevo, version, fecha);
    }

    private AtractivoPublico editar(AtractivoPublico atractivo, BorradorAtractivo b, Integer version, Instant fecha) {
        Integer esperada = campos.entero("version", version, 0, Integer.MAX_VALUE);
        Datos datos = RegistroAtractivo.datos(campos, b);
        campos.lanzarSiHayErrores();
        return atractivo.conDatos(datos.nombre(), datos.coordenada(), datos.descripcion(), datos.categoria(),
                datos.limiteCapacidadCarga(), datos.normasAmbientales(), datos.accesibilidad(),
                datos.horarioAtencion(), fecha, esperada);
    }

    private AtractivoPublico estado(AtractivoPublico atractivo, String valor, Integer version, Instant fecha) {
        Integer esperada = campos.entero("version", version, 0, Integer.MAX_VALUE);
        EstadoOperativo estado = RegistroAtractivo.estado(campos, "estadoNuevo", valor);
        if (estado != null && atractivo.getEstadoOperativo().filter(estado::equals).isPresent()) {
            campos.agregar("estadoNuevo", MotivoInvalido.NO_PERMITIDO);
        }
        campos.lanzarSiHayErrores();
        return atractivo.conEstado(estado, fecha, esperada);
    }
}
