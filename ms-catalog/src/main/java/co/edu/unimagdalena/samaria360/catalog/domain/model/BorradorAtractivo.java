package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.util.List;

/**
 * Datos de un atractivo tal como los envía el Gestor, sin validar (RF06, RF07, RF24, RF27, RF33, RF58). Los valida
 * {@link RegistroAtractivo} al registrar y {@link ActualizacionAtractivo} al editar. No trae lo que fija el servidor:
 * el Gestor, las fechas, la versión ni las rutas de las imágenes.
 *
 * @param estadoOperativo solo se lee al registrar; después cambia con su propio flujo (DEC-210)
 */
public record BorradorAtractivo(String nombre, String categoria, String descripcion, DatosCoordenadas coordenadas,
                                Integer limiteCapacidadCarga, String estadoOperativo, String normasAmbientales,
                                String horarioAtencion, List<String> condicionesAccesibilidad) {

    /** RF06: «dirección o coordenadas geográficas según corresponda»; WF-09 pide latitud y longitud. */
    public record DatosCoordenadas(Double longitud, Double latitud) {
    }
}
