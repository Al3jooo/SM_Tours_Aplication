package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorServicio.DatosContacto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Reglas por campo de un servicio, comunes al registro ({@link RegistroServicio}) y a la edición del Prestador
 * ({@link ActualizacionPrecio}, {@link ActualizacionDatosComerciales}): reúne un motivo por campo, con el nombre JSON, y
 * lanza {@link ContenidoInvalidoException} al final. Los textos se recortan con {@code strip()} y se cuentan en puntos
 * de código (DEC-138).
 */
final class CamposServicio {

    /** La forma de ms-auth ({@code Correo}): sin espacios, con @ y un punto en el dominio. */
    private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final Map<String, MotivoInvalido> campos = new LinkedHashMap<>();

    void agregar(String campo, MotivoInvalido motivo) {
        campos.put(campo, motivo);
    }

    boolean tiene(String campo) {
        return campos.containsKey(campo);
    }

    int cantidad() {
        return campos.size();
    }

    /** @throws ContenidoInvalidoException con un motivo por cada campo inválido, si hay alguno */
    void lanzarSiHayErrores() {
        if (!campos.isEmpty()) {
            throw new ContenidoInvalidoException(campos);
        }
    }

    String texto(String campo, String valor, int maximo) {
        TextoAcotado.validar(valor, maximo).ifPresent(motivo -> campos.put(campo, motivo));
        String limpio = TextoAcotado.limpiar(valor);
        return limpio.isEmpty() ? null : limpio;
    }

    /** Opcional: vacío o en blanco es «sin valor». */
    String textoOpcional(String campo, String valor, int maximo) {
        return TextoAcotado.limpiar(valor).isEmpty() ? null : texto(campo, valor, maximo);
    }

    Integer entero(String campo, Integer valor, int minimo, int maximo) {
        if (valor == null) {
            campos.put(campo, MotivoInvalido.REQUERIDO);
        } else if (valor < minimo) {
            campos.put(campo, MotivoInvalido.VALOR_MINIMO);
        } else if (valor > maximo) {
            campos.put(campo, MotivoInvalido.VALOR_MAXIMO);
        }
        return valor;
    }

    /** Mayor que 0, hasta el máximo y con 2 decimales como mucho (un exponente positivo se lleva a escala 0). */
    BigDecimal decimal(String campo, BigDecimal valor, BigDecimal maximo) {
        if (valor == null) {
            campos.put(campo, MotivoInvalido.REQUERIDO);
            return null;
        }
        if (valor.signum() <= 0) {
            campos.put(campo, MotivoInvalido.VALOR_MINIMO);
        } else if (valor.compareTo(maximo) > 0) {
            campos.put(campo, MotivoInvalido.VALOR_MAXIMO);
        } else if (valor.stripTrailingZeros().scale() > 2) {
            campos.put(campo, MotivoInvalido.FORMATO);
        }
        return valor.scale() < 0 ? valor.setScale(0) : valor;
    }

    /** Teléfono y correo de atención al público (RF09, RF43), ambos obligatorios. */
    Contacto contacto(DatosContacto datos) {
        if (datos == null) {
            campos.put("contacto", MotivoInvalido.REQUERIDO);
            return null;
        }
        String telefono = texto("contacto.telefono", datos.telefono(), RegistroServicio.TELEFONO_MAXIMO);
        String correo = texto("contacto.correo", datos.correo(), RegistroServicio.CORREO_MAXIMO);
        if (correo != null && !campos.containsKey("contacto.correo") && !CORREO.matcher(correo).matches()) {
            campos.put("contacto.correo", MotivoInvalido.FORMATO);
        }
        return new Contacto(telefono, correo);
    }
}
