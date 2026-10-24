package co.edu.unimagdalena.samaria360.auth.domain.model;

import co.edu.unimagdalena.samaria360.auth.domain.exception.DatosRegistroInvalidosException;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Datos personales e intereses que el turista puede editar en su perfil (RF53, DEC-79). El correo y la contraseña no
 * forman parte: el correo es la identidad del login y no hay un RF de cambio de contraseña. Las reglas son las del
 * registro (RF01): mismas longitudes, mismo país y mismas categorías (decisión D1, DEC-85).
 *
 * @param segundoNombre     opcional (null si no se envió)
 * @param segundoApellido   opcional (null si no se envió)
 * @param categoriasInteres entre 1 y 3, sin repetir, al validar; leído de la base puede venir vacío (semilla V14)
 */
public record PerfilTurista(String primerNombre, String segundoNombre, String primerApellido, String segundoApellido,
                            CodigoPais paisOrigen, Set<CategoriaInteres> categoriasInteres) {

    public PerfilTurista {
        Objects.requireNonNull(primerNombre, "primerNombre");
        Objects.requireNonNull(primerApellido, "primerApellido");
        Objects.requireNonNull(paisOrigen, "paisOrigen");
        categoriasInteres = categoriasInteres == null || categoriasInteres.isEmpty()
                ? Set.of()
                : Set.copyOf(EnumSet.copyOf(categoriasInteres));
    }

    /** Las categorías en el orden del vocabulario (Naturaleza, Cultura, Aventura), para responder siempre igual. */
    public List<CategoriaInteres> categoriasOrdenadas() {
        return categoriasInteres.stream().sorted().toList();
    }

    /**
     * Valida el perfil tal como llega: todos los errores a la vez, con los nombres JSON de la API.
     *
     * @throws DatosRegistroInvalidosException con un motivo por campo
     */
    public static PerfilTurista validar(String primerNombre, String segundoNombre, String primerApellido,
                                        String segundoApellido, String paisOrigen, List<String> categoriasInteres) {
        ValidadorCampos v = new ValidadorCampos();
        int maximo = RegistroTurista.LONGITUD_MAXIMA_NOMBRE;
        String primerNombreValido = v.texto("primerNombre", primerNombre, maximo);
        String segundoNombreValido = v.textoOpcional("segundoNombre", segundoNombre, maximo);
        String primerApellidoValido = v.texto("primerApellido", primerApellido, maximo);
        String segundoApellidoValido = v.textoOpcional("segundoApellido", segundoApellido, maximo);
        CodigoPais paisValido = v.valor("paisOrigen", () -> new CodigoPais(paisOrigen));
        Set<CategoriaInteres> categorias = v.categorias("categoriasInteres", categoriasInteres);
        v.lanzarSiHayErrores();
        return new PerfilTurista(primerNombreValido, segundoNombreValido, primerApellidoValido, segundoApellidoValido,
                paisValido, categorias);
    }
}
