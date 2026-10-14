package co.edu.unimagdalena.samaria360.auth.domain.model;

import co.edu.unimagdalena.samaria360.auth.domain.exception.ValorInvalidoException;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * País de origen del turista (RF01), como código ISO 3166-1 alfa-2 (decisión D5). La lista sale del JDK, que es la única
 * fuente: el backend la publica en GET /api/v1/auth/paises y el frontend solo traduce los nombres.
 *
 * @param valor en mayúsculas, p. ej. «CO»
 */
public record CodigoPais(String valor) {

    private static final Pattern DOS_LETRAS = Pattern.compile("^[A-Z]{2}$");
    private static final Set<String> CODIGOS = Set.copyOf(Locale.getISOCountries(Locale.IsoCountryCode.PART1_ALPHA2));

    public CodigoPais {
        if (valor == null || valor.isBlank()) {
            throw new ValorInvalidoException(MotivoInvalido.REQUERIDO);
        }
        valor = valor.trim().toUpperCase(Locale.ROOT);
        if (!DOS_LETRAS.matcher(valor).matches()) {
            throw new ValorInvalidoException(MotivoInvalido.FORMATO);
        }
        if (!CODIGOS.contains(valor)) {
            throw new ValorInvalidoException(MotivoInvalido.NO_PERMITIDO);
        }
    }

    /** Todos los códigos, en orden alfabético de código (el frontend ordena por el nombre traducido). */
    public static List<String> todos() {
        return CODIGOS.stream().sorted().toList();
    }
}
