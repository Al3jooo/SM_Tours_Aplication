package co.edu.unimagdalena.samaria360.auth.domain.model;

import co.edu.unimagdalena.samaria360.auth.domain.exception.DatosRegistroInvalidosException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.PoliticasNoAceptadasException;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Datos válidos del registro de un turista (RF01, WF-02). Las longitudes máximas son las de las columnas de V1.
 *
 * @param segundoNombre       opcional (null si no se envió)
 * @param segundoApellido     opcional (null si no se envió)
 * @param categoriasInteres   entre 1 y 3, sin repetir (decisión D1)
 */
public record RegistroTurista(Correo correo, ContrasenaNueva contrasena, String primerNombre, String segundoNombre,
                              String primerApellido, String segundoApellido, CodigoPais paisOrigen,
                              Set<CategoriaInteres> categoriasInteres) {

    /** {@code turista.primer_nombre}, {@code segundo_nombre}, {@code primer_apellido} y {@code segundo_apellido}. */
    public static final int LONGITUD_MAXIMA_NOMBRE = 50;

    public RegistroTurista {
        Objects.requireNonNull(correo, "correo");
        Objects.requireNonNull(contrasena, "contrasena");
        Objects.requireNonNull(primerNombre, "primerNombre");
        Objects.requireNonNull(primerApellido, "primerApellido");
        Objects.requireNonNull(paisOrigen, "paisOrigen");
        if (categoriasInteres == null || categoriasInteres.isEmpty()) {
            throw new IllegalArgumentException("categoriasInteres es obligatorio");
        }
        categoriasInteres = Set.copyOf(EnumSet.copyOf(categoriasInteres));
    }

    /**
     * Valida los datos tal como llegan: primero el formato de todos los campos (todos los errores a la vez) y después la
     * aceptación de las políticas.
     *
     * @throws DatosRegistroInvalidosException con un motivo por campo (nombres JSON de la API)
     * @throws PoliticasNoAceptadasException   si los datos son válidos pero {@code aceptoPoliticas} no es true
     */
    public static RegistroTurista validar(String primerNombre, String segundoNombre, String primerApellido,
                                          String segundoApellido, String correo, String contrasena, String paisOrigen,
                                          List<String> categoriasInteres, Boolean aceptoPoliticas) {
        ValidadorCampos v = new ValidadorCampos();
        String primerNombreValido = v.texto("primerNombre", primerNombre, LONGITUD_MAXIMA_NOMBRE);
        String segundoNombreValido = v.textoOpcional("segundoNombre", segundoNombre, LONGITUD_MAXIMA_NOMBRE);
        String primerApellidoValido = v.texto("primerApellido", primerApellido, LONGITUD_MAXIMA_NOMBRE);
        String segundoApellidoValido = v.textoOpcional("segundoApellido", segundoApellido, LONGITUD_MAXIMA_NOMBRE);
        Correo correoValido = v.valor("correo", () -> new Correo(correo));
        ContrasenaNueva contrasenaValida = v.valor("contrasena", () -> new ContrasenaNueva(contrasena));
        CodigoPais paisValido = v.valor("paisOrigen", () -> new CodigoPais(paisOrigen));
        Set<CategoriaInteres> categorias = v.categorias("categoriasInteres", categoriasInteres);
        v.lanzarSiHayErrores();
        if (!Boolean.TRUE.equals(aceptoPoliticas)) {
            throw new PoliticasNoAceptadasException();
        }
        return new RegistroTurista(correoValido, contrasenaValida, primerNombreValido, segundoNombreValido,
                primerApellidoValido, segundoApellidoValido, paisValido, categorias);
    }
}
