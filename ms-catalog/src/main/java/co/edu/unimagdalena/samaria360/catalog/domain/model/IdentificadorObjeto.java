package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.util.regex.Pattern;

/**
 * Forma textual de un ObjectId de MongoDB: 24 dígitos hexadecimales en minúscula, como los entrega
 * {@code ObjectId.toHexString()}. Es la forma de los identificadores de servicios y de unidades en la API y en
 * reservas_db ({@code varchar(24)}). El dominio no depende del driver de MongoDB.
 */
public final class IdentificadorObjeto {

    private static final Pattern FORMA = Pattern.compile("[0-9a-f]{24}");

    private IdentificadorObjeto() {
    }

    public static boolean esValido(String valor) {
        return valor != null && FORMA.matcher(valor).matches();
    }
}
