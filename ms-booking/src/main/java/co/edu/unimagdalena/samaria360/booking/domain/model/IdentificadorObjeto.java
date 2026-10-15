package co.edu.unimagdalena.samaria360.booking.domain.model;

import java.util.regex.Pattern;

/**
 * Forma textual de un ObjectId de MongoDB (24 hexadecimales en minúscula): los ids de servicios y de unidades de
 * catalog_db, que reservas_db guarda como {@code varchar(24)} (referencias lógicas, sin FK).
 */
public final class IdentificadorObjeto {

    private static final Pattern FORMA = Pattern.compile("[0-9a-f]{24}");

    private IdentificadorObjeto() {
    }

    public static boolean esValido(String valor) {
        return valor != null && FORMA.matcher(valor).matches();
    }
}
