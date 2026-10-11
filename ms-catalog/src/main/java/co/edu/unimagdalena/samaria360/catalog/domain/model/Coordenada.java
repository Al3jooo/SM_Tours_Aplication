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

    /** Radio medio de la Tierra (km) para la fórmula del haversine. */
    private static final double RADIO_TIERRA_KM = 6371.0;

    /**
     * Distancia aproximada en línea recta (RF38), con la fórmula del haversine. MongoDB usa su propia esfera en
     * $geoNear, así que ambos valores pueden diferir en una fracción de punto porcentual.
     */
    public double distanciaKm(Coordenada otra) {
        double dLat = Math.toRadians(otra.latitud - latitud);
        double dLon = Math.toRadians(otra.longitud - longitud);
        double a = Math.pow(Math.sin(dLat / 2), 2)
                + Math.cos(Math.toRadians(latitud)) * Math.cos(Math.toRadians(otra.latitud)) * Math.pow(Math.sin(dLon / 2), 2);
        return 2 * RADIO_TIERRA_KM * Math.asin(Math.min(1, Math.sqrt(a)));
    }
}
