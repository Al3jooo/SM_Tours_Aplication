package co.edu.unimagdalena.samaria360.catalog.domain.model;

/**
 * Punto geográfico. En MongoDB se guarda como GeoJSON con el orden [longitud, latitud]: por eso la longitud
 * va primero también aquí, para que el orden no se invierta al mapear.
 */
public record Coordenada(double longitud, double latitud) {

    public Coordenada {
        if (!Double.isFinite(longitud) || longitud < -180 || longitud > 180) {
            throw new IllegalArgumentException("La longitud debe estar entre -180 y 180: " + longitud);
        }
        if (!Double.isFinite(latitud) || latitud < -90 || latitud > 90) {
            throw new IllegalArgumentException("La latitud debe estar entre -90 y 90: " + latitud);
        }
    }
}
