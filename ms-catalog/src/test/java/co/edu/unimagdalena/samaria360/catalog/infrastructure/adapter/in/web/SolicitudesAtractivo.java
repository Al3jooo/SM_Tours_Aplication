package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web;

import co.edu.unimagdalena.samaria360.catalog.infrastructure.TokensDePrueba;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;

/**
 * Cuerpos y ayudantes de las pruebas de atractivos (RF06; DEC-216): la parte {@code datos} en JSON, las imágenes con su
 * firma, el token del Gestor con su fila en {@code cuentas} (DEC-110) y la limpieza de lo que cada prueba agrega.
 */
final class SolicitudesAtractivo {

    static final String RUTA = "/api/v1/catalogo/gestor/atractivos";
    static final byte[] FIRMA_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0};
    static final byte[] FIRMA_PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};

    private SolicitudesAtractivo() {
    }

    static Map<String, Object> datos(String nombre) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("nombre", nombre);
        datos.put("categoria", "Naturaleza");
        datos.put("descripcion", "Sendero entre bosque húmedo hasta una caída de agua.");
        datos.put("coordenadas", Map.of("longitud", -74.11, "latitud", 11.15));
        datos.put("limiteCapacidadCarga", 80);
        datos.put("estadoOperativo", "Abierto");
        datos.put("normasAmbientales", "Prohibido hacer fogatas.");
        datos.put("horarioAtencion", "Martes a domingo, 8:00 a 16:00");
        datos.put("condicionesAccesibilidad", List.of("Visual", "MovilidadReducida"));
        return datos;
    }

    static Map<String, Object> con(Map<String, Object> datos, Consumer<Map<String, Object>> cambio) {
        Map<String, Object> copia = new LinkedHashMap<>(datos);
        cambio.accept(copia);
        return copia;
    }

    static byte[] imagen(byte[] firma, int tamano) {
        return Arrays.copyOf(firma, tamano);
    }

    static MockMultipartFile parteImagen(String nombre, byte[] contenido) {
        return new MockMultipartFile("imagenes", nombre, MediaType.IMAGE_JPEG_VALUE, contenido);
    }

    static MockMultipartFile parteDatos(Map<String, Object> datos) {
        return new MockMultipartFile("datos", "", MediaType.APPLICATION_JSON_VALUE,
                SolicitudesServicio.json(datos).getBytes(StandardCharsets.UTF_8));
    }

    /** POST multipart con los datos (si no son nulos) y las imágenes dadas. */
    static MockMultipartHttpServletRequestBuilder registro(String token, Map<String, Object> datos,
                                                           MockMultipartFile... imagenes) {
        MockMultipartHttpServletRequestBuilder solicitud = multipart(RUTA);
        if (datos != null) {
            solicitud.file(parteDatos(datos));
        }
        for (MockMultipartFile imagen : imagenes) {
            solicitud.file(imagen);
        }
        solicitud.header(HttpHeaders.AUTHORIZATION, token);
        return solicitud;
    }

    static String tokenGestor(UUID gestor, long ver) {
        return TokensDePrueba.bearer(TokensDePrueba.usuario(gestor, "Gestor", ver));
    }

    /** La fila de {@code cuentas} sin la cual un Gestor recibe 401 SESION_INVALIDADA (DEC-110). */
    static void filaDeCuenta(MongoDatabase base, UUID id, String rol, long version) {
        base.getCollection("cuentas").insertOne(new Document("_id", id.toString()).append("rol", rol)
                .append("estadoCuenta", "Activo").append("version", version).append("actualizadoEn", new Date()));
    }

    /** Borra la fila del Gestor, sus atractivos y las imágenes de GridFS a las que apuntan. */
    static void limpiar(MongoDatabase base, UUID gestor) {
        for (Document atractivo : base.getCollection("atractivos").find(Filters.eq("gestorId", gestor.toString()))) {
            for (String ruta : atractivo.getList("imagenes", String.class, List.of())) {
                if (ruta.startsWith("/api/v1/catalogo/imagenes/")) {
                    try {
                        GridFSBuckets.create(base, "imagenes")
                                .delete(new ObjectId(ruta.substring(ruta.lastIndexOf('/') + 1)));
                    } catch (RuntimeException e) {
                        // Ya no existe.
                    }
                }
            }
        }
        base.getCollection("atractivos").deleteMany(Filters.eq("gestorId", gestor.toString()));
        base.getCollection("cuentas").deleteOne(Filters.eq("_id", gestor.toString()));
    }
}
