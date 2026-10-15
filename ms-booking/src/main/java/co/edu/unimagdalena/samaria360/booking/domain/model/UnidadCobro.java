package co.edu.unimagdalena.samaria360.booking.domain.model;

/**
 * Cómo se cobra (§5 del plan; el esquema del catálogo no lo define, se toma de WF-05):
 * <ul>
 *   <li>{@link #Noche}: alojamiento, por noche y por habitación;</li>
 *   <li>{@link #Persona}: tour, por persona.</li>
 * </ul>
 */
public enum UnidadCobro {
    Noche,
    Persona
}
