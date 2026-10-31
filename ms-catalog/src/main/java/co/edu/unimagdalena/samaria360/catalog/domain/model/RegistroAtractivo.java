package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorAtractivo.DatosCoordenadas;
import co.edu.unimagdalena.samaria360.catalog.domain.service.PoliticaImagen;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * CU «Registrar atractivo» (RF06, RF07, RF24, RF27, RF33, RF58; DEC-207 a DEC-212): valida un {@link BorradorAtractivo}
 * y sus imágenes y crea el atractivo, ya publicado (DEC-87). Reúne un motivo por campo, con el nombre JSON (las
 * imágenes como {@code imagenes[i]}), y lanza {@link ContenidoInvalidoException} si alguno no sirve. Las reglas por
 * campo son las de los servicios ({@link CamposServicio}: {@code strip()} y puntos de código, DEC-138). Los máximos de
 * longitud son los del validador v2 (M012); el de capacidad es técnico, no una regla del SRS.
 */
public final class RegistroAtractivo {

    public static final int NOMBRE_MAXIMO = 120;
    public static final int DESCRIPCION_MAXIMA = 1000;
    /** WF-09: «Máximo 1.000 caracteres» (RF24). */
    public static final int NORMAS_MAXIMAS = 1000;
    /** Como el horario de los servicios (RF58, DEC-208). */
    public static final int HORARIO_MAXIMO = RegistroServicio.TEXTO_LARGO_MAXIMO;
    public static final int CAPACIDAD_MAXIMA = 100_000;
    /** Ruta pública con la que {@code imagenes[]} guarda una imagen de GridFS (DEC-209). */
    public static final String RUTA_IMAGENES = "/api/v1/catalogo/imagenes/";

    /** Una imagen ya validada, con el id (ObjectId en texto) con el que se guardará. */
    public record ImagenPorGuardar(String id, FormatoImagen formato, String nombre, byte[] contenido) {
    }

    /** El atractivo nuevo y las imágenes que hay que guardar antes de insertarlo, en el orden recibido. */
    public record Registro(AtractivoPublico atractivo, List<ImagenPorGuardar> imagenes) {
    }

    /** Los datos editables ya validados: los comparten el registro y la edición. */
    record Datos(String nombre, String categoria, String descripcion, Coordenada coordenada, int limiteCapacidadCarga,
                 String normasAmbientales, String horarioAtencion, Set<CondicionAccesibilidad> accesibilidad) {
    }

    private final CamposServicio campos = new CamposServicio();

    private RegistroAtractivo() {
    }

    /**
     * @param id        ObjectId del atractivo nuevo
     * @param idsImagen genera el id de cada imagen; solo se llama si todo es válido
     * @throws ContenidoInvalidoException con un motivo por cada campo inválido
     */
    public static Registro registrar(BorradorAtractivo borrador, List<ImagenSubida> imagenes, String id,
                                     Supplier<String> idsImagen, UUID gestorId, Instant fechaRegistro) {
        return new RegistroAtractivo().validar(borrador, imagenes == null ? List.of() : imagenes, id, idsImagen,
                Objects.requireNonNull(gestorId), fechaRegistro);
    }

    private Registro validar(BorradorAtractivo b, List<ImagenSubida> imagenes, String id, Supplier<String> idsImagen,
                             UUID gestorId, Instant fechaRegistro) {
        BorradorAtractivo borrador = b == null ? vacio() : b;
        Datos datos = datos(campos, borrador);
        EstadoOperativo estado = estado(campos, "estadoOperativo", borrador.estadoOperativo());
        validarImagenes(imagenes);
        campos.lanzarSiHayErrores();
        List<ImagenPorGuardar> porGuardar = new ArrayList<>();
        List<String> rutas = new ArrayList<>();
        for (ImagenSubida imagen : imagenes) {
            ImagenPorGuardar guardar = new ImagenPorGuardar(idsImagen.get(),
                    FormatoImagen.detectar(imagen.contenido()).orElseThrow(), imagen.nombre(), imagen.contenido());
            porGuardar.add(guardar);
            rutas.add(RUTA_IMAGENES + guardar.id());
        }
        AtractivoPublico atractivo = new AtractivoPublico(id, gestorId, datos.nombre(), datos.coordenada(),
                datos.descripcion(), datos.categoria(), rutas, datos.limiteCapacidadCarga(), estado,
                datos.normasAmbientales(), datos.accesibilidad(), datos.horarioAtencion(), fechaRegistro, null, 0);
        return new Registro(atractivo, porGuardar);
    }

    private void validarImagenes(List<ImagenSubida> imagenes) {
        PoliticaImagen.motivoCantidad(imagenes.size()).ifPresent(motivo -> campos.agregar("imagenes", motivo));
        if (imagenes.size() > PoliticaImagen.MAXIMO) {
            return;
        }
        for (int i = 0; i < imagenes.size(); i++) {
            String campo = "imagenes[" + i + "]";
            PoliticaImagen.motivo(imagenes.get(i)).ifPresent(motivo -> campos.agregar(campo, motivo));
        }
    }

    // ------------------------------------------------------------------ reglas comunes con la edición

    /** RF06, RF07, RF24, RF27 y RF58: todo lo editable. Un dato inválido deja su motivo y un nulo en su lugar. */
    static Datos datos(CamposServicio campos, BorradorAtractivo b) {
        String nombre = campos.texto("nombre", b.nombre(), NOMBRE_MAXIMO);
        CategoriaAtractivo categoria = valor(campos, "categoria", CategoriaAtractivo.class, b.categoria());
        String descripcion = campos.texto("descripcion", b.descripcion(), DESCRIPCION_MAXIMA);
        Coordenada coordenada = coordenada(campos, b.coordenadas());
        Integer limite = campos.entero("limiteCapacidadCarga", b.limiteCapacidadCarga(), 1, CAPACIDAD_MAXIMA);
        String normas = campos.textoOpcional("normasAmbientales", b.normasAmbientales(), NORMAS_MAXIMAS);
        String horario = campos.textoOpcional("horarioAtencion", b.horarioAtencion(), HORARIO_MAXIMO);
        Set<CondicionAccesibilidad> accesibilidad = accesibilidad(campos, b.condicionesAccesibilidad());
        return new Datos(nombre, categoria == null ? null : categoria.name(), descripcion, coordenada,
                limite == null ? 0 : limite, normas, horario, accesibilidad);
    }

    /** RF33: uno de los tres estados exactos. */
    static EstadoOperativo estado(CamposServicio campos, String campo, String valor) {
        return valor(campos, campo, EstadoOperativo.class, valor);
    }

    /** Como en el registro de servicios (DEC-137): solo el rango global; el SRS no fija un límite geográfico. */
    private static Coordenada coordenada(CamposServicio campos, DatosCoordenadas datos) {
        if (datos == null) {
            campos.agregar("coordenadas", MotivoInvalido.REQUERIDO);
            return null;
        }
        boolean longitud = enRango(campos, "coordenadas.longitud", datos.longitud(), 180);
        boolean latitud = enRango(campos, "coordenadas.latitud", datos.latitud(), 90);
        return longitud && latitud ? new Coordenada(datos.longitud(), datos.latitud()) : null;
    }

    private static boolean enRango(CamposServicio campos, String campo, Double valor, double limite) {
        if (valor == null || !Double.isFinite(valor)) {
            campos.agregar(campo, valor == null ? MotivoInvalido.REQUERIDO : MotivoInvalido.FORMATO);
        } else if (valor < -limite) {
            campos.agregar(campo, MotivoInvalido.VALOR_MINIMO);
        } else if (valor > limite) {
            campos.agregar(campo, MotivoInvalido.VALOR_MAXIMO);
        } else {
            return true;
        }
        return false;
    }

    /** RF27: el vocabulario cerrado del SRS, sin repetir (DEC-221). */
    private static Set<CondicionAccesibilidad> accesibilidad(CamposServicio campos, List<String> valores) {
        Set<CondicionAccesibilidad> condiciones = EnumSet.noneOf(CondicionAccesibilidad.class);
        if (valores == null) {
            return condiciones;
        }
        for (int i = 0; i < valores.size(); i++) {
            String campo = "condicionesAccesibilidad[" + i + "]";
            CondicionAccesibilidad condicion = valorDe(CondicionAccesibilidad.class, valores.get(i));
            if (condicion == null) {
                campos.agregar(campo, MotivoInvalido.NO_PERMITIDO);
            } else if (!condiciones.add(condicion)) {
                campos.agregar(campo, MotivoInvalido.DUPLICADO);
            }
        }
        return condiciones;
    }

    private static <E extends Enum<E>> E valor(CamposServicio campos, String campo, Class<E> tipo, String valor) {
        if (valor == null || valor.isBlank()) {
            campos.agregar(campo, MotivoInvalido.REQUERIDO);
            return null;
        }
        E encontrado = valorDe(tipo, valor);
        if (encontrado == null) {
            campos.agregar(campo, MotivoInvalido.NO_PERMITIDO);
        }
        return encontrado;
    }

    private static <E extends Enum<E>> E valorDe(Class<E> tipo, String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return Enum.valueOf(tipo, valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static BorradorAtractivo vacio() {
        return new BorradorAtractivo(null, null, null, null, null, null, null, null, null);
    }
}
