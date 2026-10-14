package co.edu.unimagdalena.samaria360.auth.domain.model;

import co.edu.unimagdalena.samaria360.auth.domain.exception.DatosRegistroInvalidosException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.ValorInvalidoException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Valida todos los campos de un registro y acumula un motivo por campo, para responder todos los errores a la vez.
 * Los nombres de campo son los del JSON de la API (primerNombre, aceptoPoliticas…): el frontend los asocia a cada control.
 */
final class ValidadorCampos {

    private final Map<String, MotivoInvalido> errores = new LinkedHashMap<>();

    /** Crea un objeto de valor; si lo rechaza, anota el motivo en el campo y devuelve null. */
    <T> T valor(String campo, Supplier<T> crear) {
        try {
            return crear.get();
        } catch (ValorInvalidoException e) {
            errores.putIfAbsent(campo, e.getMotivo());
            return null;
        }
    }

    /** Texto obligatorio, con trim y una longitud máxima igual a la de su columna. */
    String texto(String campo, String valor, int maximo) {
        if (valor == null || valor.isBlank()) {
            errores.putIfAbsent(campo, MotivoInvalido.REQUERIDO);
            return null;
        }
        return acotado(campo, valor.trim(), maximo);
    }

    /** Texto opcional: vacío equivale a null. */
    String textoOpcional(String campo, String valor, int maximo) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return acotado(campo, valor.trim(), maximo);
    }

    /** Al menos una, del vocabulario cerrado y sin repetir (decisión D1). Repetidas no se unifican: es un error. */
    Set<CategoriaInteres> categorias(String campo, List<String> valores) {
        if (valores == null || valores.isEmpty()) {
            errores.putIfAbsent(campo, MotivoInvalido.REQUERIDO);
            return null;
        }
        List<CategoriaInteres> leidas = new ArrayList<>();
        for (String valor : valores) {
            var categoria = CategoriaInteres.desdeTexto(valor);
            if (categoria.isEmpty()) {
                errores.putIfAbsent(campo, MotivoInvalido.NO_PERMITIDO);
                return null;
            }
            leidas.add(categoria.get());
        }
        Set<CategoriaInteres> unicas = EnumSet.copyOf(leidas);
        if (unicas.size() != leidas.size()) {
            errores.putIfAbsent(campo, MotivoInvalido.DUPLICADO);
            return null;
        }
        return unicas;
    }

    void lanzarSiHayErrores() {
        if (!errores.isEmpty()) {
            throw new DatosRegistroInvalidosException(errores);
        }
    }

    private String acotado(String campo, String valor, int maximo) {
        if (valor.codePointCount(0, valor.length()) > maximo) {
            errores.putIfAbsent(campo, MotivoInvalido.LONGITUD_MAXIMA);
            return null;
        }
        return valor;
    }
}
