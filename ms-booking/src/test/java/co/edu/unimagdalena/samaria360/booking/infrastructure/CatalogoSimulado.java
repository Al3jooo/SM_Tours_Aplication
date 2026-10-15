package co.edu.unimagdalena.samaria360.booking.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ms-catalog (y el JWKS de ms-auth) simulado con {@code com.sun.net.httpserver.HttpServer} del JDK: sin dependencias
 * nuevas. Responde el detalle público de los servicios publicados (404 si no), la búsqueda paginada de los que se
 * marcan para ella, y permite fallar, demorar y contar llamadas por servicio.
 */
public final class CatalogoSimulado {

    public static final UUID PRESTADOR = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final AtomicLong SECUENCIA = new AtomicLong(System.nanoTime() & 0xFFFFFFFFL);
    private static final int TAMANO_PAGINA = 10;

    /** Unidad de un alojamiento en el detalle del catálogo (D1). */
    public record Unidad(String idUnidad, String nombre, int numHabitaciones, int capacidadMaxHuespedes,
                         String precioNoche) {
    }

    private final HttpServer servidor;
    private final Map<String, String> detalles = new ConcurrentHashMap<>();
    private final Map<String, List<String>> busqueda = new ConcurrentHashMap<>();
    private final Map<String, Integer> fallos = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> fallosRestantes = new ConcurrentHashMap<>();
    private final Map<String, Duration> demoras = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> llamadas = new ConcurrentHashMap<>();

    private CatalogoSimulado() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.setExecutor(Executors.newCachedThreadPool());
        servidor.createContext("/", this::responder);
        servidor.start();
    }

    public static CatalogoSimulado iniciar() {
        try {
            return new CatalogoSimulado();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public String url() {
        return "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    /** Un id con la forma de un ObjectId, distinto en cada llamada (las pruebas comparten la base). */
    public static String nuevoId() {
        return String.format("%024x", SECUENCIA.incrementAndGet());
    }

    // ------------------------------------------------------------------ publicar

    public String publicarHotel(String nombre, Unidad... unidades) {
        String id = nuevoId();
        publicarHotel(id, nombre, unidades);
        return id;
    }

    public void publicarHotel(String id, String nombre, Unidad... unidades) {
        List<Map<String, Object>> lista = new ArrayList<>();
        BigDecimal desde = null;
        for (Unidad unidad : unidades) {
            Map<String, Object> datos = new LinkedHashMap<>();
            datos.put("idUnidad", unidad.idUnidad());
            datos.put("nombre", unidad.nombre());
            datos.put("tipoHabitacion", unidad.nombre());
            datos.put("numHabitaciones", unidad.numHabitaciones());
            datos.put("capacidadMaxHuespedes", unidad.capacidadMaxHuespedes());
            datos.put("precioNoche", new BigDecimal(unidad.precioNoche()));
            lista.add(datos);
            BigDecimal precio = new BigDecimal(unidad.precioNoche());
            desde = desde == null || precio.compareTo(desde) < 0 ? precio : desde;
        }
        publicar(id, nombre, "Alojamiento", desde, Map.of("serviciosIncluidos", List.of("WiFi"), "unidades", lista));
    }

    public String publicarTour(String nombre, int aforo, String precio) {
        String id = nuevoId();
        publicar(id, nombre, "Tour", new BigDecimal(precio), Map.of("puntoEncuentro", "Muelle",
                "duracionHoras", new BigDecimal("4"), "nivelDificultad", "Media", "aforoMaxGrupo", aforo));
        return id;
    }

    public String publicarRestaurante(String nombre) {
        String id = nuevoId();
        publicar(id, nombre, "Gastronomia", new BigDecimal("50000"), Map.of("tipoCocina", "Típica",
                "aforoMaxTurno", 40, "rangoPrecioPlato", "$20.000"));
        return id;
    }

    private void publicar(String id, String nombre, String categoria, BigDecimal precio,
                          Map<String, Object> detalle) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("id", id);
        cuerpo.put("idPrestador", PRESTADOR.toString());
        cuerpo.put("nombreComercial", nombre);
        cuerpo.put("categoria", categoria);
        cuerpo.put("precioBase", precio);
        cuerpo.put("moneda", "COP");
        cuerpo.put("detalleCategoria", detalle);
        try {
            detalles.put(id, JSON.writeValueAsString(cuerpo));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** El servicio deja de ser visible: su detalle responde 404. */
    public void retirar(String id) {
        detalles.remove(id);
    }

    /** Aparece en la búsqueda pública de su categoría (para el generador de inventario). */
    public void enBusqueda(String categoria, String id) {
        busqueda.computeIfAbsent(categoria, c -> new CopyOnWriteArrayList<>()).add(id);
    }

    public void limpiarBusqueda() {
        busqueda.clear();
    }

    // ------------------------------------------------------------------ fallos

    /** Responde {@code estado} en las próximas {@code veces} llamadas al detalle (−1 = siempre). */
    public void fallar(String id, int estado, int veces) {
        fallos.put(id, estado);
        fallosRestantes.put(id, new AtomicInteger(veces));
    }

    public void demorar(String id, Duration demora) {
        demoras.put(id, demora);
    }

    public void normalizar(String id) {
        fallos.remove(id);
        fallosRestantes.remove(id);
        demoras.remove(id);
    }

    public int llamadas(String id) {
        AtomicInteger contador = llamadas.get(id);
        return contador == null ? 0 : contador.get();
    }

    // ------------------------------------------------------------------ HTTP

    private void responder(HttpExchange intercambio) throws IOException {
        try (intercambio) {
            String ruta = intercambio.getRequestURI().getPath();
            if (ruta.equals("/api/v1/auth/.well-known/jwks.json")) {
                escribir(intercambio, 200, TokensDePrueba.jwks());
            } else if (ruta.startsWith("/api/v1/catalogo/servicios/")) {
                detalle(intercambio, ruta.substring("/api/v1/catalogo/servicios/".length()));
            } else if (ruta.equals("/api/v1/catalogo/oferta")) {
                pagina(intercambio);
            } else {
                escribir(intercambio, 404, "{}");
            }
        }
    }

    private void detalle(HttpExchange intercambio, String id) throws IOException {
        llamadas.computeIfAbsent(id, i -> new AtomicInteger()).incrementAndGet();
        Duration demora = demoras.get(id);
        if (demora != null) {
            try {
                Thread.sleep(demora.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        Integer estado = fallos.get(id);
        AtomicInteger restantes = fallosRestantes.get(id);
        if (estado != null && restantes != null && (restantes.get() < 0 || restantes.getAndDecrement() > 0)) {
            escribir(intercambio, estado, "{\"codigo\":\"SIMULADO\"}");
            return;
        }
        String cuerpo = detalles.get(id);
        if (cuerpo == null) {
            escribir(intercambio, 404, "{\"codigo\":\"OFERTA_NO_DISPONIBLE\"}");
        } else {
            escribir(intercambio, 200, cuerpo);
        }
    }

    private void pagina(HttpExchange intercambio) throws IOException {
        Map<String, String> parametros = new LinkedHashMap<>();
        String consulta = intercambio.getRequestURI().getQuery();
        if (consulta != null) {
            for (String parte : consulta.split("&")) {
                String[] par = parte.split("=", 2);
                parametros.put(par[0], par.length > 1 ? par[1] : "");
            }
        }
        List<String> ids = busqueda.getOrDefault(parametros.get("categoria"), List.of());
        int pagina = Integer.parseInt(parametros.getOrDefault("pagina", "0"));
        int desde = Math.min(pagina * TAMANO_PAGINA, ids.size());
        List<Map<String, Object>> elementos = ids.subList(desde, Math.min(desde + TAMANO_PAGINA, ids.size())).stream()
                .<Map<String, Object>>map(id -> Map.of("tipo", "Servicio", "id", id)).toList();
        int totalPaginas = (ids.size() + TAMANO_PAGINA - 1) / TAMANO_PAGINA;
        escribir(intercambio, 200, JSON.writeValueAsString(Map.of("elementos", elementos, "pagina", pagina,
                "tamano", TAMANO_PAGINA, "totalElementos", ids.size(), "totalPaginas", totalPaginas)));
    }

    private static void escribir(HttpExchange intercambio, int estado, String cuerpo) throws IOException {
        byte[] bytes = cuerpo.getBytes(StandardCharsets.UTF_8);
        intercambio.getResponseHeaders().add("Content-Type", estado >= 400
                ? "application/problem+json" : "application/json");
        intercambio.sendResponseHeaders(estado, bytes.length);
        try (OutputStream salida = intercambio.getResponseBody()) {
            salida.write(bytes);
        }
    }
}
