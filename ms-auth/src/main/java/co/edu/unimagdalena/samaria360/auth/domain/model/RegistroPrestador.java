package co.edu.unimagdalena.samaria360.auth.domain.model;

import co.edu.unimagdalena.samaria360.auth.domain.exception.DatosRegistroInvalidosException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.PoliticasNoAceptadasException;

import java.util.Objects;

/**
 * Datos válidos del registro de un prestador (RF02, WF-03). Las longitudes máximas son las de las columnas de V1.
 * El SRS no define el formato del RNT ni del teléfono, así que solo se valida que estén y su longitud.
 *
 * @param correoCorporativo credencial de acceso y, al registrarse, también el correo de contacto (decisión D2)
 * @param razonSocial       opcional (decisión D2, V5)
 */
public record RegistroPrestador(String nombreRepresentanteLegal, String razonSocial, String rnt,
                                Correo correoCorporativo, String telefono, ContrasenaNueva contrasena) {

    public static final int LONGITUD_MAXIMA_REPRESENTANTE = 150;
    public static final int LONGITUD_MAXIMA_RAZON_SOCIAL = 150;
    public static final int LONGITUD_MAXIMA_RNT = 50;
    public static final int LONGITUD_MAXIMA_TELEFONO = 20;

    public RegistroPrestador {
        Objects.requireNonNull(nombreRepresentanteLegal, "nombreRepresentanteLegal");
        Objects.requireNonNull(rnt, "rnt");
        Objects.requireNonNull(correoCorporativo, "correoCorporativo");
        Objects.requireNonNull(telefono, "telefono");
        Objects.requireNonNull(contrasena, "contrasena");
    }

    /**
     * Igual que {@link RegistroTurista#validar}: primero el formato de todos los campos y después las políticas.
     *
     * @throws DatosRegistroInvalidosException con un motivo por campo (nombres JSON de la API)
     * @throws PoliticasNoAceptadasException   si los datos son válidos pero {@code aceptoPoliticas} no es true
     */
    public static RegistroPrestador validar(String nombreRepresentanteLegal, String razonSocial, String rnt,
                                            String correoCorporativo, String telefono, String contrasena,
                                            Boolean aceptoPoliticas) {
        ValidadorCampos v = new ValidadorCampos();
        String representante = v.texto("nombreRepresentanteLegal", nombreRepresentanteLegal,
                LONGITUD_MAXIMA_REPRESENTANTE);
        String razon = v.textoOpcional("razonSocial", razonSocial, LONGITUD_MAXIMA_RAZON_SOCIAL);
        String rntValido = v.texto("rnt", rnt, LONGITUD_MAXIMA_RNT);
        Correo correo = v.valor("correoCorporativo", () -> new Correo(correoCorporativo));
        String telefonoValido = v.texto("telefono", telefono, LONGITUD_MAXIMA_TELEFONO);
        ContrasenaNueva contrasenaValida = v.valor("contrasena", () -> new ContrasenaNueva(contrasena));
        v.lanzarSiHayErrores();
        if (!Boolean.TRUE.equals(aceptoPoliticas)) {
            throw new PoliticasNoAceptadasException();
        }
        return new RegistroPrestador(representante, razon, rntValido, correo, telefonoValido, contrasenaValida);
    }
}
