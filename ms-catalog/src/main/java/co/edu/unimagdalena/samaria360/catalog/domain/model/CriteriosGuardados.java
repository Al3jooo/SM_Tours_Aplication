package co.edu.unimagdalena.samaria360.catalog.domain.model;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.CriteriosBusquedaInvalidosException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Criterios de una búsqueda del historial (RF60, DEC-56, DEC-88): los mismos de {@code GET /oferta}, normalizados, sin
 * página ni coordenadas. Por RF38 la ubicación del turista no se guarda: solo si la búsqueda la usó
 * ({@code usarUbicacion}); al repetirla, el frontend la vuelve a pedir.
 * <p>
 * Se validan con las reglas de {@link CriteriosBusqueda} (con una ubicación de referencia cuando
 * {@code usarUbicacion} es true), así que un criterio inválido es el mismo 400 CRITERIOS_INVALIDOS de la búsqueda.
 *
 * @param texto        con trim; vacío equivale a null
 * @param accesibilidad en el orden del vocabulario, sin repetir
 * @param masFiltros   «Más filtros» (RF10), con los textos recortados y {@code serviciosIncluidos} canónicos y ordenados
 */
public record CriteriosGuardados(String texto, Categoria categoria, BigDecimal precioMax, boolean usarUbicacion,
                                 Double distanciaKm, List<CondicionAccesibilidad> accesibilidad, OrdenBusqueda orden,
                                 ParametrosFiltrosCategoria masFiltros) {

    /** Solo para validar con las reglas de la búsqueda: no se guarda ni se usa para calcular distancias. */
    private static final Coordenada UBICACION_DE_REFERENCIA = new Coordenada(0, 0);
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");

    public CriteriosGuardados {
        texto = recortado(texto);
        precioMax = decimal(precioMax);
        accesibilidad = accesibilidad == null ? List.of()
                : accesibilidad.stream().filter(Objects::nonNull).distinct().sorted().toList();
        masFiltros = normalizados(masFiltros == null ? ParametrosFiltrosCategoria.ninguno() : masFiltros);
        new CriteriosBusqueda(texto, categoria, masFiltros.paraCategoria(categoria), precioMax,
                usarUbicacion ? UBICACION_DE_REFERENCIA : null, distanciaKm, Set.copyOf(accesibilidad), orden, 0,
                CriteriosBusqueda.TAMANO_PAGINA_MAXIMO);
    }

    /**
     * Sin ningún filtro: no se guarda (DEC-88). El orden por sí solo no es un criterio; «Más filtros» exige categoría,
     * así que sin categoría no hay ninguno.
     */
    public boolean esVacio() {
        return texto == null && categoria == null && precioMax == null && !usarUbicacion && distanciaKm == null
                && accesibilidad.isEmpty();
    }

    /**
     * Forma canónica para la deduplicación (índice único con el turista): el texto sin distinguir mayúsculas y con los
     * espacios internos colapsados, como lo compara la búsqueda, y el resto de los campos en un orden fijo.
     */
    public String clave() {
        ParametrosFiltrosCategoria f = masFiltros;
        return String.join("|",
                parte("texto", comparable(texto)),
                parte("categoria", categoria),
                parte("precioMax", precioMax == null ? null : precioMax.toPlainString()),
                parte("usarUbicacion", usarUbicacion),
                parte("distanciaKm", distanciaKm == null ? null
                        : BigDecimal.valueOf(distanciaKm).stripTrailingZeros().toPlainString()),
                parte("accesibilidad", accesibilidad.stream().map(Enum::name).collect(Collectors.joining(","))),
                parte("orden", orden),
                parte("serviciosIncluidos", String.join(",", f.serviciosIncluidos())),
                parte("huespedes", f.huespedes()),
                parte("tipoHabitacion", comparable(f.tipoHabitacion())),
                parte("menuVegetariano", f.menuVegetariano()),
                parte("tipoCocina", comparable(f.tipoCocina())),
                parte("nivelDificultad", comparable(f.nivelDificultad())),
                parte("duracionMaxHoras", f.duracionMaxHoras() == null ? null
                        : f.duracionMaxHoras().toPlainString()),
                parte("tipoVehiculoEquipo", comparable(f.tipoVehiculoEquipo())));
    }

    private static ParametrosFiltrosCategoria normalizados(ParametrosFiltrosCategoria p) {
        List<String> servicios;
        try {
            servicios = p.serviciosIncluidos().stream().map(ServicioIncluido::desdeTexto).distinct().sorted()
                    .map(Enum::name).toList();
        } catch (IllegalArgumentException e) {
            throw new CriteriosBusquedaInvalidosException(e.getMessage());
        }
        return new ParametrosFiltrosCategoria(servicios, p.huespedes(), recortado(p.tipoHabitacion()),
                Boolean.TRUE.equals(p.menuVegetariano()) ? Boolean.TRUE : null, recortado(p.tipoCocina()),
                recortado(p.nivelDificultad()),
                decimal(p.duracionMaxHoras()),
                recortado(p.tipoVehiculoEquipo()));
    }

    /** Sin ceros a la derecha y nunca en notación científica (200000, no 2E+5). */
    private static BigDecimal decimal(BigDecimal valor) {
        if (valor == null) {
            return null;
        }
        BigDecimal sinCeros = valor.stripTrailingZeros();
        return sinCeros.scale() < 0 ? sinCeros.setScale(0) : sinCeros;
    }

    private static String recortado(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    private static String comparable(String valor) {
        return valor == null ? null : ESPACIOS.matcher(valor.toLowerCase(Locale.ROOT)).replaceAll(" ");
    }

    /** {@code nombre=valor}, con {@code \} y {@code |} escapados para que dos combinaciones distintas no coincidan. */
    private static String parte(String nombre, Object valor) {
        String texto = valor == null ? "" : valor.toString().replace("\\", "\\\\").replace("|", "\\|");
        return nombre + "=" + texto;
    }
}
