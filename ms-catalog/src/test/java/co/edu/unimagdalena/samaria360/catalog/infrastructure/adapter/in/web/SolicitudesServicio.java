package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Cuerpos JSON de {@code POST /prestador/servicios}, válidos por categoría, para las pruebas de integración. */
final class SolicitudesServicio {

    static final List<String> TODOS_LOS_DIAS =
            List.of("Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo");
    private static final ObjectMapper JSON = new ObjectMapper();

    private SolicitudesServicio() {
    }

    static Map<String, Object> base(String nombre, String categoria) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("nombreComercial", nombre);
        cuerpo.put("categoria", categoria);
        cuerpo.put("descripcion", "Servicio registrado en la prueba de integración");
        cuerpo.put("direccion", "Carrera 1 # 2-3, Taganga");
        cuerpo.put("coordenadas", Map.of("longitud", -74.1906, "latitud", 11.2672));
        cuerpo.put("contacto", Map.of("telefono", "+57 300 000 0000", "correo", "prueba@servicio.test"));
        cuerpo.put("condicionesAccesibilidad", List.of("Visual"));
        cuerpo.put("horarioAtencion", "Lunes a domingo, 8:00 a 18:00");
        return cuerpo;
    }

    static Map<String, Object> alojamiento(String nombre, List<Map<String, Object>> unidades) {
        Map<String, Object> cuerpo = base(nombre, "Alojamiento");
        cuerpo.put("detalleCategoria", Map.of("unidades", unidades, "serviciosIncluidos", List.of("WiFi")));
        cuerpo.put("oferta", Map.of("diasOperacion", TODOS_LOS_DIAS));
        return cuerpo;
    }

    static Map<String, Object> unidad(String nombre, int habitaciones, int capacidad, String precio) {
        Map<String, Object> unidad = new LinkedHashMap<>();
        unidad.put("nombre", nombre);
        unidad.put("tipoHabitacion", "Doble");
        unidad.put("numHabitaciones", habitaciones);
        unidad.put("capacidadMaxHuespedes", capacidad);
        unidad.put("precioNoche", new java.math.BigDecimal(precio));
        return unidad;
    }

    static Map<String, Object> tour(String nombre) {
        Map<String, Object> cuerpo = base(nombre, "Tour");
        cuerpo.put("precioBase", 95000);
        cuerpo.put("detalleCategoria", Map.of("puntoEncuentro", "Muelle de Taganga", "duracionHoras", 3.5,
                "nivelDificultad", "Baja", "aforoMaxGrupo", 12));
        cuerpo.put("oferta", Map.of("diasOperacion", List.of("Sabado", "Domingo"), "salidas", List.of("08:00")));
        return cuerpo;
    }

    static Map<String, Object> gastronomia(String nombre) {
        Map<String, Object> cuerpo = base(nombre, "Gastronomia");
        cuerpo.put("precioBase", 40000);
        cuerpo.put("detalleCategoria", Map.of("tipoCocina", "Caribeña", "aforoMaxTurno", 60,
                "menuVegetariano", true, "rangoPrecioPlato", "$30.000 - $60.000"));
        return cuerpo;
    }

    static Map<String, Object> alquiler(String nombre) {
        Map<String, Object> cuerpo = base(nombre, "AlquilerEquipo");
        cuerpo.put("precioBase", 30000);
        cuerpo.put("detalleCategoria", Map.of("tipoVehiculoEquipo", "Kayak doble", "unidadesDisponibles", 8));
        return cuerpo;
    }

    static Map<String, Object> con(Map<String, Object> cuerpo, Consumer<Map<String, Object>> cambio) {
        Map<String, Object> copia = new LinkedHashMap<>(cuerpo);
        cambio.accept(copia);
        return copia;
    }

    static String json(Object cuerpo) {
        try {
            return JSON.writeValueAsString(cuerpo);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
