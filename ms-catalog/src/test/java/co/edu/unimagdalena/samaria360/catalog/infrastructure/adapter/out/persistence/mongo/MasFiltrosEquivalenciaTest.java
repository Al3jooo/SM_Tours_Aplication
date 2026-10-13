package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.out.persistence.mongo;

import co.edu.unimagdalena.samaria360.catalog.domain.model.Categoria;
import co.edu.unimagdalena.samaria360.catalog.domain.model.CondicionAccesibilidad;
import co.edu.unimagdalena.samaria360.catalog.domain.model.Coordenada;
import co.edu.unimagdalena.samaria360.catalog.domain.model.CriteriosBusqueda;
import co.edu.unimagdalena.samaria360.catalog.domain.model.DetalleAlojamiento;
import co.edu.unimagdalena.samaria360.catalog.domain.model.DetalleAlquilerEquipo;
import co.edu.unimagdalena.samaria360.catalog.domain.model.DetalleCategoria;
import co.edu.unimagdalena.samaria360.catalog.domain.model.DetalleGastronomia;
import co.edu.unimagdalena.samaria360.catalog.domain.model.DetalleTour;
import co.edu.unimagdalena.samaria360.catalog.domain.model.EstadoPublicacion;
import co.edu.unimagdalena.samaria360.catalog.domain.model.FiltrosAlojamiento;
import co.edu.unimagdalena.samaria360.catalog.domain.model.FiltrosAlquilerEquipo;
import co.edu.unimagdalena.samaria360.catalog.domain.model.FiltrosCategoria;
import co.edu.unimagdalena.samaria360.catalog.domain.model.FiltrosGastronomia;
import co.edu.unimagdalena.samaria360.catalog.domain.model.FiltrosTour;
import co.edu.unimagdalena.samaria360.catalog.domain.model.OpcionesFiltro;
import co.edu.unimagdalena.samaria360.catalog.domain.model.OpcionesFiltro.Rango;
import co.edu.unimagdalena.samaria360.catalog.domain.model.OrdenBusqueda;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ResultadoOferta;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioComercial;
import co.edu.unimagdalena.samaria360.catalog.domain.model.ServicioIncluido;
import co.edu.unimagdalena.samaria360.catalog.domain.service.PoliticaBusqueda;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * «Más filtros»: la agregación de {@link OfertaMongoAdapter} devuelve lo mismo que {@link PoliticaBusqueda} en
 * memoria, con datos propios (sin la semilla) que incluyen tildes: «Cabaña», «Ecohabitación», «Típica» y «Fácil»,
 * junto a «Tipica» y «Facil» sin tilde. Regla común: mayúsculas no, tildes sí.
 */
@Testcontainers
class MasFiltrosEquivalenciaTest {

    private static final UUID PRESTADOR = UUID.fromString("00000000-0000-4000-8000-000000000002");
    private static final Coordenada CENTRO = new Coordenada(-74.2125, 11.2425);
    private static final Set<CondicionAccesibilidad> ACCESIBLE = Set.of(CondicionAccesibilidad.MovilidadReducida);

    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0")
            .withCopyFileToContainer(MountableFile.forHostPath(Path.of("../infra/mongo/init")), "/scripts");

    private static final List<ServicioComercial> SERVICIOS = new ArrayList<>();
    private static MongoClient cliente;
    private static OfertaMongoAdapter ofertas;
    private static OpcionesFiltroMongoAdapter opciones;
    private final PoliticaBusqueda politica = new PoliticaBusqueda();

    @BeforeAll
    static void cargarDatos() throws Exception {
        // Solo validadores e índices: los documentos de prueba deben pasar el mismo $jsonSchema que la semilla.
        for (String script : List.of("01-validadores.js", "02-indices.js")) {
            ExecResult resultado = MONGO.execInContainer("mongosh", "--quiet", "/scripts/" + script);
            assertThat(resultado.getExitCode()).as(resultado.getStderr()).isZero();
        }
        cliente = MongoClients.create(MONGO.getConnectionString());
        MongoTemplate mongo = new MongoTemplate(cliente, "catalog_db");
        ofertas = new OfertaMongoAdapter(mongo);
        opciones = new OpcionesFiltroMongoAdapter(mongo);

        agregar("Cabañas del río", new DetalleAlojamiento("Cabaña", 3, 4, List.of("WiFi", "Desayuno")), "180000",
                EstadoPublicacion.Publicado, true, ACCESIBLE);
        agregar("Ecolodge", new DetalleAlojamiento("Ecohabitación", 5, 3, List.of("Desayuno", "Piscina")), "420000",
                EstadoPublicacion.Publicado, true, Set.of());
        agregar("Hotel pendiente", new DetalleAlojamiento("Doble", 8, 2, List.of("Parqueadero")), "150000",
                EstadoPublicacion.Pendiente, true, Set.of());
        agregar("Fonda típica", new DetalleGastronomia("Típica", 30, true, "$20.000"), "20000",
                EstadoPublicacion.Publicado, true, ACCESIBLE);
        agregar("Fonda sin tilde", new DetalleGastronomia("Tipica", 30, null, "$20.000"), "25000",
                EstadoPublicacion.Publicado, true, Set.of());
        agregar("Marisquería", new DetalleGastronomia("Mariscos", 30, false, "$40.000"), "40000",
                EstadoPublicacion.Publicado, true, Set.of());
        agregar("Sendero fácil", new DetalleTour("Muelle", new BigDecimal("3.5"), "Fácil", 10), "60000",
                EstadoPublicacion.Publicado, true, Set.of());
        agregar("Sendero sin tilde", new DetalleTour("Muelle", new BigDecimal("2"), "Facil", 10), "50000",
                EstadoPublicacion.Publicado, true, Set.of());
        agregar("Tour sin nivel", new DetalleTour("Muelle", new BigDecimal("5"), null, 10), "70000",
                EstadoPublicacion.Publicado, true, Set.of());
        agregar("Tour de prestador inactivo", new DetalleTour("Muelle", new BigDecimal("1"), "Baja", 10), "10000",
                EstadoPublicacion.Publicado, false, Set.of());
        agregar("Kayaks", new DetalleAlquilerEquipo("Kayak doble", 4, null), "40000",
                EstadoPublicacion.Publicado, true, Set.of());
        agregar("Bicicletas", new DetalleAlquilerEquipo("Bicicleta urbana", 10, null), "30000",
                EstadoPublicacion.Publicado, true, Set.of());
    }

    @AfterAll
    static void cerrar() {
        if (cliente != null) {
            cliente.close();
        }
    }

    static Stream<Arguments> casos() {
        return Stream.of(
                caso(alojamiento(null, null, "Cabaña"), "Cabañas del río"),
                caso(alojamiento(null, null, "cabaña"), "Cabañas del río"),
                caso(alojamiento(null, null, "CABAÑA"), "Cabañas del río"),
                caso(alojamiento(null, null, "Cabana")),
                caso(alojamiento(null, null, "Caba")),
                caso(alojamiento(null, null, "ecohabitación"), "Ecolodge"),
                caso(alojamiento(null, null, "Ecohabitacion")),
                caso(alojamiento(Set.of(ServicioIncluido.Desayuno), null, null), "Cabañas del río", "Ecolodge"),
                caso(alojamiento(Set.of(ServicioIncluido.WiFi, ServicioIncluido.Desayuno), null, null),
                        "Cabañas del río"),
                caso(alojamiento(Set.of(ServicioIncluido.Parqueadero), null, null)),
                caso(alojamiento(null, 3, null), "Cabañas del río", "Ecolodge"),
                caso(alojamiento(null, 4, null), "Cabañas del río"),
                caso(alojamiento(null, 5, null)),
                caso(new FiltrosGastronomia(true, null), "Fonda típica"),
                caso(new FiltrosGastronomia(false, "Típica"), "Fonda típica"),
                caso(new FiltrosGastronomia(false, "típica"), "Fonda típica"),
                caso(new FiltrosGastronomia(false, "TÍPICA"), "Fonda típica"),
                caso(new FiltrosGastronomia(false, "Tipica"), "Fonda sin tilde"),
                caso(new FiltrosGastronomia(true, "Tipica")),
                caso(new FiltrosTour("Fácil", null), "Sendero fácil"),
                caso(new FiltrosTour("fácil", null), "Sendero fácil"),
                caso(new FiltrosTour("Facil", null), "Sendero sin tilde"),
                caso(new FiltrosTour("Baja", null)),
                caso(new FiltrosTour(null, new BigDecimal("3.5")), "Sendero fácil", "Sendero sin tilde"),
                caso(new FiltrosTour(null, new BigDecimal("3.49")), "Sendero sin tilde"),
                caso(new FiltrosTour("Fácil", new BigDecimal("3"))),
                caso(new FiltrosAlquilerEquipo("kayak doble"), "Kayaks"),
                caso(new FiltrosAlquilerEquipo("Kayak")),
                caso(new FiltrosAlquilerEquipo("Kayak.doble")));
    }

    @ParameterizedTest(name = "{0} → {1}")
    @MethodSource("casos")
    void laAgregacionYLaPoliticaEnMemoriaCoinciden(FiltrosCategoria filtros, List<String> esperados) {
        for (OrdenBusqueda orden : List.of(OrdenBusqueda.Nombre, OrdenBusqueda.PrecioAscendente)) {
            CriteriosBusqueda criterios = new CriteriosBusqueda(null, filtros.categoria(), filtros, null, null, null,
                    Set.of(), orden, 0, CriteriosBusqueda.TAMANO_PAGINA_MAXIMO);

            List<String> enMongo = nombres(ofertas.buscar(criterios).elementos());
            List<String> enMemoria = nombres(politica.buscar(SERVICIOS, List.of(), criterios).elementos());

            assertThat(enMongo).as("MongoDB, orden %s", orden).isEqualTo(enMemoria);
            assertThat(enMongo).as("orden %s", orden).containsExactlyInAnyOrderElementsOf(esperados);
        }
    }

    @Test
    void combinaMasFiltrosConAccesibilidadYPrecio() {
        CriteriosBusqueda criterios = new CriteriosBusqueda(null, Categoria.Gastronomia,
                new FiltrosGastronomia(false, null), new BigDecimal("30000"), null, null, ACCESIBLE,
                OrdenBusqueda.Nombre, 0, 10);
        CriteriosBusqueda vegetarianoBarato = new CriteriosBusqueda(null, Categoria.Gastronomia,
                new FiltrosGastronomia(true, null), new BigDecimal("30000"), null, null, ACCESIBLE,
                OrdenBusqueda.Nombre, 0, 10);

        assertThat(nombres(ofertas.buscar(criterios).elementos())).containsExactly("Fonda típica");
        assertThat(nombres(ofertas.buscar(vegetarianoBarato).elementos()))
                .isEqualTo(nombres(politica.buscar(SERVICIOS, List.of(), vegetarianoBarato).elementos()))
                .containsExactly("Fonda típica");
    }

    // ------------------------------------------------------------------ opciones

    @Test
    void lasOpcionesSalenSoloDeLosServiciosVisibles() {
        OpcionesFiltro alojamiento = opciones.opciones(Categoria.Alojamiento);
        OpcionesFiltro gastronomia = opciones.opciones(Categoria.Gastronomia);
        OpcionesFiltro tour = opciones.opciones(Categoria.Tour);
        OpcionesFiltro alquiler = opciones.opciones(Categoria.AlquilerEquipo);

        // «Hotel pendiente» (Doble, Parqueadero, 2 huéspedes) no está publicado.
        assertThat(alojamiento.serviciosIncluidos())
                .containsExactly(ServicioIncluido.WiFi, ServicioIncluido.Desayuno, ServicioIncluido.Piscina);
        assertThat(alojamiento.tiposHabitacion()).containsExactly("Cabaña", "Ecohabitación");
        assertThat(alojamiento.huespedes()).isEqualTo(new Rango<>(3, 4));
        assertThat(alojamiento.tiposCocina()).isNull();

        assertThat(gastronomia.opcionVegetariana()).isTrue();
        assertThat(gastronomia.tiposCocina()).containsExactly("Mariscos", "Tipica", "Típica");

        // «Baja» y 1 h son del prestador inactivo; el tour sin nivel no aporta nivel, pero sí duración.
        assertThat(tour.nivelesDificultad()).containsExactly("Facil", "Fácil");
        assertThat(tour.duracionHoras().minimo()).isEqualByComparingTo("2");
        assertThat(tour.duracionHoras().maximo()).isEqualByComparingTo("5");

        assertThat(alquiler.tiposVehiculoEquipo()).containsExactly("Bicicleta urbana", "Kayak doble");
    }

    @Test
    void cadaOpcionOfrecidaDevuelveAlMenosUnResultado() {
        OpcionesFiltro alojamiento = opciones.opciones(Categoria.Alojamiento);
        alojamiento.serviciosIncluidos().forEach(servicio ->
                assertConResultados(alojamiento(Set.of(servicio), null, null)));
        alojamiento.tiposHabitacion().forEach(tipo -> assertConResultados(alojamiento(null, null, tipo)));
        assertConResultados(alojamiento(null, alojamiento.huespedes().maximo(), null));

        opciones.opciones(Categoria.Gastronomia).tiposCocina()
                .forEach(tipo -> assertConResultados(new FiltrosGastronomia(false, tipo)));
        OpcionesFiltro tour = opciones.opciones(Categoria.Tour);
        tour.nivelesDificultad().forEach(nivel -> assertConResultados(new FiltrosTour(nivel, null)));
        assertConResultados(new FiltrosTour(null, tour.duracionHoras().minimo()));
        opciones.opciones(Categoria.AlquilerEquipo).tiposVehiculoEquipo()
                .forEach(tipo -> assertConResultados(new FiltrosAlquilerEquipo(tipo)));
    }

    // ------------------------------------------------------------------ auxiliares

    private void assertConResultados(FiltrosCategoria filtros) {
        CriteriosBusqueda criterios = new CriteriosBusqueda(null, filtros.categoria(), filtros, null, null, null,
                Set.of(), OrdenBusqueda.Nombre, 0, 10);
        assertThat(ofertas.buscar(criterios).totalElementos()).as("%s", filtros).isPositive();
    }

    private static Arguments caso(FiltrosCategoria filtros, String... esperados) {
        return Arguments.of(filtros, List.of(esperados));
    }

    private static FiltrosAlojamiento alojamiento(Set<ServicioIncluido> servicios, Integer huespedes, String tipo) {
        return new FiltrosAlojamiento(servicios, huespedes, tipo);
    }

    private static List<String> nombres(List<ResultadoOferta> resultados) {
        return resultados.stream().map(ResultadoOferta::nombre).toList();
    }

    /** Inserta el documento en MongoDB y el mismo servicio, con su _id, en la lista de la política. */
    private static void agregar(String nombre, DetalleCategoria detalle, String precio, EstadoPublicacion estado,
                                boolean prestadorActivo, Set<CondicionAccesibilidad> accesibilidad) {
        ObjectId id = new ObjectId();
        Document documento = new Document("_id", id)
                .append("prestadorId", PRESTADOR.toString())
                .append("nombreComercial", nombre)
                .append("categoria", detalle.categoria().name())
                .append("coordenadas", new Document("type", "Point")
                        .append("coordinates", List.of(CENTRO.longitud(), CENTRO.latitud())))
                .append("estadoPublicacion", estado.name())
                .append("prestadorActivo", prestadorActivo)
                .append("condicionesAccesibilidad", accesibilidad.stream().map(Enum::name).sorted().toList())
                .append("precioBase", new Decimal128(new BigDecimal(precio)))
                .append("imagenes", List.of())
                .append("detalleCategoria", detalle(detalle));
        cliente.getDatabase("catalog_db").getCollection("servicios").insertOne(documento);
        SERVICIOS.add(new ServicioComercial(id.toHexString(), PRESTADOR, nombre, detalle.categoria(), detalle, null,
                CENTRO, null, estado, prestadorActivo, accesibilidad, null, new BigDecimal(precio), List.of()));
    }

    private static Document detalle(DetalleCategoria detalle) {
        Document documento = new Document();
        if (detalle instanceof DetalleAlojamiento alojamiento) {
            documento.append("tipoHabitacion", alojamiento.tipoHabitacion())
                    .append("numHabitaciones", alojamiento.numHabitaciones())
                    .append("capacidadMaxHuespedes", alojamiento.capacidadMaxHuespedes())
                    .append("serviciosIncluidos", alojamiento.serviciosIncluidos());
        } else if (detalle instanceof DetalleGastronomia gastronomia) {
            documento.append("tipoCocina", gastronomia.tipoCocina())
                    .append("aforoMaxTurno", gastronomia.aforoMaxTurno())
                    .append("rangoPrecioPlato", gastronomia.rangoPrecioPlato());
            if (gastronomia.menuVegetariano() != null) {
                documento.append("menuVegetariano", gastronomia.menuVegetariano());
            }
        } else if (detalle instanceof DetalleTour tour) {
            documento.append("puntoEncuentro", tour.puntoEncuentro())
                    .append("duracionHoras", new Decimal128(tour.duracionHoras()))
                    .append("aforoMaxGrupo", tour.aforoMaxGrupo());
            if (tour.nivelDificultad() != null) {
                documento.append("nivelDificultad", tour.nivelDificultad());
            }
        } else if (detalle instanceof DetalleAlquilerEquipo alquiler) {
            documento.append("tipoVehiculoEquipo", alquiler.tipoVehiculoEquipo())
                    .append("unidadesDisponibles", alquiler.unidadesDisponibles());
        }
        return documento;
    }
}
