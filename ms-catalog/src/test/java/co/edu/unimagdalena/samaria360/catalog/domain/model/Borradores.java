package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorServicio.DatosContacto;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorServicio.DatosCoordenadas;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorServicio.DatosDetalle;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorServicio.DatosOferta;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorServicio.DatosUnidad;

import java.math.BigDecimal;
import java.util.List;

/** Borradores válidos de cada categoría para las pruebas del registro (RF09, RF10; DEC-136). */
public final class Borradores {

    public static final List<String> TODOS_LOS_DIAS =
            List.of("Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo");

    private Borradores() {
    }

    public static DatosUnidad unidad(String nombre, int habitaciones, int capacidad, String precio) {
        return new DatosUnidad(nombre, "Doble", habitaciones, capacidad, new BigDecimal(precio));
    }

    public static BorradorServicio alojamiento(List<DatosUnidad> unidades) {
        return base("Alojamiento", null,
                new DatosDetalle(unidades, List.of("WiFi", "Desayuno"), null, null, null, null, null, null, null, null,
                        null, null, null),
                new DatosOferta(TODOS_LOS_DIAS, null));
    }

    public static BorradorServicio alojamiento() {
        return alojamiento(List.of(unidad("Doble estándar", 10, 2, "180000")));
    }

    public static BorradorServicio tour() {
        return base("Tour", new BigDecimal("95000"),
                new DatosDetalle(null, null, null, null, null, null, "Muelle de Taganga", new BigDecimal("3.5"),
                        "Baja", 12, null, null, null),
                new DatosOferta(List.of("Sabado", "Domingo"), List.of("14:00", "08:00")));
    }

    public static BorradorServicio gastronomia() {
        return base("Gastronomia", new BigDecimal("40000"),
                new DatosDetalle(null, null, "Caribeña", 60, true, "$30.000 - $60.000", null, null, null, null, null,
                        null, null),
                null);
    }

    public static BorradorServicio alquiler() {
        return base("AlquilerEquipo", new BigDecimal("30000"),
                new DatosDetalle(null, null, null, null, null, null, null, null, null, null, "Kayak doble", 8,
                        "Depósito de $50.000"),
                null);
    }

    private static BorradorServicio base(String categoria, BigDecimal precio, DatosDetalle detalle,
                                         DatosOferta oferta) {
        return new BorradorServicio("  Servicio de prueba  ", categoria, "Descripción del servicio", "Calle 1 # 2-3",
                new DatosCoordenadas(-74.2125, 11.2425), new DatosContacto("+57 300 000 0000", "hola@servicio.test"),
                List.of("Visual", "MovilidadReducida"), "Lunes a domingo, 8:00 a 18:00", precio, detalle, oferta);
    }
}
