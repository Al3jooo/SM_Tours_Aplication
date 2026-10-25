package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.ContenidoInvalidoException;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorServicio.DatosContacto;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorServicio.DatosCoordenadas;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorServicio.DatosDetalle;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorServicio.DatosOferta;
import co.edu.unimagdalena.samaria360.catalog.domain.model.BorradorServicio.DatosUnidad;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * CU «Registrar servicio» (RF09, RF10, RF37; parte de RF27 y RF58): valida un {@link BorradorServicio} y crea el
 * servicio Pendiente (DEC-124). Reúne un motivo por campo, con el nombre JSON (los anidados con punto e índice, p. ej.
 * {@code detalleCategoria.unidades[0].precioNoche}, DEC-151), y lanza {@link ContenidoInvalidoException} si alguno no
 * sirve. Los textos se recortan con {@code strip()} y se cuentan en puntos de código (DEC-138). Los máximos numéricos
 * son técnicos, no reglas de negocio: el precio cabe en {@code reserva.precio_unitario numeric(10,2)} de ms-booking.
 */
public final class RegistroServicio {

    public static final int DESCRIPCION_MAXIMA = 2000;
    public static final int TELEFONO_MAXIMO = 20;
    public static final int CORREO_MAXIMO = 254;
    public static final int TEXTO_LARGO_MAXIMO = 500;
    public static final int UNIDADES_MAXIMAS = 20;
    public static final BigDecimal PRECIO_MAXIMO = new BigDecimal("99999999.99");
    public static final BigDecimal DURACION_MAXIMA = new BigDecimal("720");
    public static final int AFORO_MAXIMO = 10_000;

    /** La forma de ms-auth ({@code Correo}): sin espacios, con @ y un punto en el dominio. */
    private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern HORA = Pattern.compile("^([01][0-9]|2[0-3]):[0-5][0-9]$");

    private final Map<String, MotivoInvalido> campos = new LinkedHashMap<>();

    private RegistroServicio() {
    }

    /**
     * @param id              ObjectId del servicio nuevo
     * @param idsUnidad       genera el {@code idUnidad} de cada unidad (DEC-141)
     * @param rnt             copia del RNT de la proyección {@code cuentas} (DEC-126)
     * @param prestadorActivo la copia del estado del prestador, también de la proyección (DEC-125)
     * @throws ContenidoInvalidoException con un motivo por cada campo inválido
     */
    public static ServicioComercial registrar(BorradorServicio borrador, String id, Supplier<String> idsUnidad,
                                              UUID prestadorId, String rnt, boolean prestadorActivo,
                                              Instant fechaRegistro) {
        return new RegistroServicio().validar(borrador, id, idsUnidad, prestadorId, rnt, prestadorActivo,
                fechaRegistro);
    }

    private ServicioComercial validar(BorradorServicio b, String id, Supplier<String> idsUnidad, UUID prestadorId,
                                      String rnt, boolean prestadorActivo, Instant fechaRegistro) {
        String nombre = texto("nombreComercial", b.nombreComercial(), ServicioComercial.LONGITUD_MAXIMA_NOMBRE);
        Categoria categoria = categoria(b.categoria());
        String descripcion = texto("descripcion", b.descripcion(), DESCRIPCION_MAXIMA);
        String direccion = texto("direccion", b.direccion(), ServicioComercial.LONGITUD_MAXIMA_DIRECCION);
        Coordenada coordenada = coordenada(b.coordenadas());
        Contacto contacto = contacto(b.contacto());
        Set<CondicionAccesibilidad> accesibilidad = accesibilidad(b.condicionesAccesibilidad());
        String horario = textoOpcional("horarioAtencion", b.horarioAtencion(), TEXTO_LARGO_MAXIMO);
        DetalleCategoria detalle = null;
        BigDecimal precio = null;
        Oferta oferta = null;
        if (categoria != null) {
            detalle = detalle(categoria, b.detalleCategoria(), idsUnidad);
            // En Alojamiento el precio «desde» se deriva de las unidades (DEC-127): el recibido se ignora.
            precio = categoria == Categoria.Alojamiento ? null : decimal("precioBase", b.precioBase(), PRECIO_MAXIMO);
            oferta = oferta(categoria, b.oferta());
        }
        if (!campos.isEmpty()) {
            throw new ContenidoInvalidoException(campos);
        }
        return new ServicioComercial(id, prestadorId, nombre, categoria, detalle, descripcion, coordenada, contacto,
                EstadoPublicacion.Pendiente, prestadorActivo, accesibilidad, horario, precio, List.of(), direccion,
                oferta, rnt, fechaRegistro, null);
    }

    // ------------------------------------------------------------------ datos generales (RF09)

    private Categoria categoria(String valor) {
        if (valor == null || valor.isBlank()) {
            campos.put("categoria", MotivoInvalido.REQUERIDO);
            return null;
        }
        Categoria categoria = valorDe(Categoria.class, valor);
        if (categoria == null) {
            campos.put("categoria", MotivoInvalido.NO_PERMITIDO);
        }
        return categoria;
    }

    private Coordenada coordenada(DatosCoordenadas datos) {
        if (datos == null) {
            campos.put("coordenadas", MotivoInvalido.REQUERIDO);
            return null;
        }
        boolean longitud = enRango("coordenadas.longitud", datos.longitud(), 180);
        boolean latitud = enRango("coordenadas.latitud", datos.latitud(), 90);
        return longitud && latitud ? new Coordenada(datos.longitud(), datos.latitud()) : null;
    }

    /** DEC-137: solo el rango global; el SRS no fija un límite geográfico. */
    private boolean enRango(String campo, Double valor, double limite) {
        if (valor == null) {
            campos.put(campo, MotivoInvalido.REQUERIDO);
        } else if (valor < -limite) {
            campos.put(campo, MotivoInvalido.VALOR_MINIMO);
        } else if (valor > limite) {
            campos.put(campo, MotivoInvalido.VALOR_MAXIMO);
        } else {
            return true;
        }
        return false;
    }

    private Contacto contacto(DatosContacto datos) {
        if (datos == null) {
            campos.put("contacto", MotivoInvalido.REQUERIDO);
            return null;
        }
        String telefono = texto("contacto.telefono", datos.telefono(), TELEFONO_MAXIMO);
        String correo = texto("contacto.correo", datos.correo(), CORREO_MAXIMO);
        if (correo != null && !campos.containsKey("contacto.correo") && !CORREO.matcher(correo).matches()) {
            campos.put("contacto.correo", MotivoInvalido.FORMATO);
        }
        return new Contacto(telefono, correo);
    }

    /** RF27: el vocabulario cerrado del SRS, sin repetir. */
    private Set<CondicionAccesibilidad> accesibilidad(List<String> valores) {
        Set<CondicionAccesibilidad> condiciones = EnumSet.noneOf(CondicionAccesibilidad.class);
        if (valores == null) {
            return condiciones;
        }
        for (int i = 0; i < valores.size(); i++) {
            String campo = "condicionesAccesibilidad[" + i + "]";
            CondicionAccesibilidad condicion = valorDe(CondicionAccesibilidad.class, valores.get(i));
            if (condicion == null) {
                campos.put(campo, MotivoInvalido.NO_PERMITIDO);
            } else if (!condiciones.add(condicion)) {
                campos.put(campo, MotivoInvalido.DUPLICADO);
            }
        }
        return condiciones;
    }

    // ------------------------------------------------------------------ campos por categoría (RF10)

    private DetalleCategoria detalle(Categoria categoria, DatosDetalle d, Supplier<String> idsUnidad) {
        if (d == null) {
            campos.put("detalleCategoria", MotivoInvalido.REQUERIDO);
            return null;
        }
        int antes = campos.size();
        return switch (categoria) {
            case Alojamiento -> alojamiento(d, idsUnidad);
            case Gastronomia -> {
                String tipoCocina = texto("detalleCategoria.tipoCocina", d.tipoCocina(), 100);
                Integer aforo = entero("detalleCategoria.aforoMaxTurno", d.aforoMaxTurno(), 1, AFORO_MAXIMO);
                String rango = texto("detalleCategoria.rangoPrecioPlato", d.rangoPrecioPlato(), 50);
                yield campos.size() > antes ? null
                        : new DetalleGastronomia(tipoCocina, aforo, d.menuVegetariano(), rango);
            }
            case Tour -> {
                String punto = texto("detalleCategoria.puntoEncuentro", d.puntoEncuentro(), 200);
                BigDecimal duracion = decimal("detalleCategoria.duracionHoras", d.duracionHoras(), DURACION_MAXIMA);
                String dificultad = textoOpcional("detalleCategoria.nivelDificultad", d.nivelDificultad(), 50);
                Integer aforo = entero("detalleCategoria.aforoMaxGrupo", d.aforoMaxGrupo(), 1, AFORO_MAXIMO);
                yield campos.size() > antes ? null : new DetalleTour(punto, duracion, dificultad, aforo);
            }
            case AlquilerEquipo -> {
                String tipo = texto("detalleCategoria.tipoVehiculoEquipo", d.tipoVehiculoEquipo(), 100);
                Integer unidades = entero("detalleCategoria.unidadesDisponibles", d.unidadesDisponibles(), 1,
                        AFORO_MAXIMO);
                String condiciones = textoOpcional("detalleCategoria.condicionesAlquiler", d.condicionesAlquiler(),
                        TEXTO_LARGO_MAXIMO);
                yield campos.size() > antes ? null : new DetalleAlquilerEquipo(tipo, unidades, condiciones);
            }
        };
    }

    /** DEC-133: de 1 a 20 unidades; el {@code idUnidad} lo genera el servidor. */
    private DetalleAlojamiento alojamiento(DatosDetalle d, Supplier<String> idsUnidad) {
        int antes = campos.size();
        List<DatosUnidad> datos = d.unidades();
        List<UnidadAlojamiento> unidades = new ArrayList<>();
        if (datos == null) {
            campos.put("detalleCategoria.unidades", MotivoInvalido.REQUERIDO);
        } else if (datos.isEmpty()) {
            campos.put("detalleCategoria.unidades", MotivoInvalido.CANTIDAD_MINIMA);
        } else if (datos.size() > UNIDADES_MAXIMAS) {
            campos.put("detalleCategoria.unidades", MotivoInvalido.CANTIDAD_MAXIMA);
        } else {
            for (int i = 0; i < datos.size(); i++) {
                UnidadAlojamiento unidad = unidad("detalleCategoria.unidades[" + i + "]", datos.get(i), idsUnidad);
                if (unidad != null) {
                    unidades.add(unidad);
                }
            }
        }
        List<String> incluidos = serviciosIncluidos(d.serviciosIncluidos());
        return campos.size() > antes ? null : new DetalleAlojamiento(unidades, incluidos);
    }

    private UnidadAlojamiento unidad(String prefijo, DatosUnidad u, Supplier<String> idsUnidad) {
        if (u == null) {
            campos.put(prefijo, MotivoInvalido.REQUERIDO);
            return null;
        }
        int antes = campos.size();
        String nombre = texto(prefijo + ".nombre", u.nombre(), UnidadAlojamiento.LONGITUD_MAXIMA_NOMBRE);
        String tipo = texto(prefijo + ".tipoHabitacion", u.tipoHabitacion(), 50);
        Integer habitaciones = entero(prefijo + ".numHabitaciones", u.numHabitaciones(), 1, 1000);
        Integer capacidad = entero(prefijo + ".capacidadMaxHuespedes", u.capacidadMaxHuespedes(), 1, 50);
        BigDecimal precio = decimal(prefijo + ".precioNoche", u.precioNoche(), PRECIO_MAXIMO);
        return campos.size() > antes ? null
                : new UnidadAlojamiento(idsUnidad.get(), nombre, tipo, habitaciones, capacidad, precio);
    }

    /** RF10: el vocabulario cerrado de WF-08, sin distinguir mayúsculas y sin repetir. */
    private List<String> serviciosIncluidos(List<String> valores) {
        List<String> incluidos = new ArrayList<>();
        if (valores == null) {
            return incluidos;
        }
        for (int i = 0; i < valores.size(); i++) {
            String campo = "detalleCategoria.serviciosIncluidos[" + i + "]";
            try {
                String nombre = ServicioIncluido.desdeTexto(valores.get(i)).name();
                if (incluidos.contains(nombre)) {
                    campos.put(campo, MotivoInvalido.DUPLICADO);
                } else {
                    incluidos.add(nombre);
                }
            } catch (IllegalArgumentException e) {
                campos.put(campo, MotivoInvalido.NO_PERMITIDO);
            }
        }
        return incluidos;
    }

    // ------------------------------------------------------------------ oferta inicial (DEC-130, DEC-136)

    private Oferta oferta(Categoria categoria, DatosOferta datos) {
        if (categoria == Categoria.Gastronomia || categoria == Categoria.AlquilerEquipo) {
            if (datos != null) {
                campos.put("oferta", MotivoInvalido.NO_PERMITIDO);
            }
            return null;
        }
        if (datos == null) {
            campos.put("oferta", MotivoInvalido.REQUERIDO);
            return null;
        }
        int antes = campos.size();
        List<DiaSemana> dias = dias(datos.diasOperacion());
        List<LocalTime> salidas = List.of();
        if (categoria == Categoria.Tour) {
            salidas = salidas(datos.salidas());
        } else if (datos.salidas() != null && !datos.salidas().isEmpty()) {
            campos.put("oferta.salidas", MotivoInvalido.NO_PERMITIDO);
        }
        return campos.size() > antes ? null : new Oferta(dias, salidas);
    }

    private List<DiaSemana> dias(List<String> valores) {
        List<DiaSemana> dias = new ArrayList<>();
        if (valores == null || valores.isEmpty()) {
            campos.put("oferta.diasOperacion", MotivoInvalido.REQUERIDO);
            return dias;
        }
        for (int i = 0; i < valores.size(); i++) {
            String campo = "oferta.diasOperacion[" + i + "]";
            DiaSemana dia = valorDe(DiaSemana.class, valores.get(i));
            if (dia == null) {
                campos.put(campo, MotivoInvalido.NO_PERMITIDO);
            } else if (dias.contains(dia)) {
                campos.put(campo, MotivoInvalido.DUPLICADO);
            } else {
                dias.add(dia);
            }
        }
        return dias;
    }

    private List<LocalTime> salidas(List<String> valores) {
        List<LocalTime> salidas = new ArrayList<>();
        if (valores == null || valores.isEmpty()) {
            campos.put("oferta.salidas", MotivoInvalido.REQUERIDO);
            return salidas;
        }
        if (valores.size() > Oferta.MAXIMO_SALIDAS) {
            campos.put("oferta.salidas", MotivoInvalido.CANTIDAD_MAXIMA);
            return salidas;
        }
        Set<LocalTime> vistas = new HashSet<>();
        for (int i = 0; i < valores.size(); i++) {
            String campo = "oferta.salidas[" + i + "]";
            String valor = valores.get(i);
            if (valor == null || !HORA.matcher(valor).matches()) {
                campos.put(campo, MotivoInvalido.FORMATO);
            } else if (!vistas.add(LocalTime.parse(valor))) {
                campos.put(campo, MotivoInvalido.DUPLICADO);
            } else {
                salidas.add(LocalTime.parse(valor));
            }
        }
        return salidas;
    }

    // ------------------------------------------------------------------ auxiliares

    private String texto(String campo, String valor, int maximo) {
        TextoAcotado.validar(valor, maximo).ifPresent(motivo -> campos.put(campo, motivo));
        String limpio = TextoAcotado.limpiar(valor);
        return limpio.isEmpty() ? null : limpio;
    }

    /** Opcional: vacío o en blanco es «sin valor». */
    private String textoOpcional(String campo, String valor, int maximo) {
        return TextoAcotado.limpiar(valor).isEmpty() ? null : texto(campo, valor, maximo);
    }

    private Integer entero(String campo, Integer valor, int minimo, int maximo) {
        if (valor == null) {
            campos.put(campo, MotivoInvalido.REQUERIDO);
        } else if (valor < minimo) {
            campos.put(campo, MotivoInvalido.VALOR_MINIMO);
        } else if (valor > maximo) {
            campos.put(campo, MotivoInvalido.VALOR_MAXIMO);
        }
        return valor;
    }

    /** Mayor que 0, hasta el máximo y con 2 decimales como mucho (un exponente positivo se lleva a escala 0). */
    private BigDecimal decimal(String campo, BigDecimal valor, BigDecimal maximo) {
        if (valor == null) {
            campos.put(campo, MotivoInvalido.REQUERIDO);
            return null;
        }
        if (valor.signum() <= 0) {
            campos.put(campo, MotivoInvalido.VALOR_MINIMO);
        } else if (valor.compareTo(maximo) > 0) {
            campos.put(campo, MotivoInvalido.VALOR_MAXIMO);
        } else if (valor.stripTrailingZeros().scale() > 2) {
            campos.put(campo, MotivoInvalido.FORMATO);
        }
        return valor.scale() < 0 ? valor.setScale(0) : valor;
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
}
