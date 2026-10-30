package co.edu.unimagdalena.samaria360.auth.domain.model;

import co.edu.unimagdalena.samaria360.auth.domain.exception.DatosRegistroInvalidosException;

import java.util.Objects;

/**
 * Lo que el Prestador edita de su perfil comercial en auth_db (RF43; DEC-188): el correo corporativo de contacto y el
 * teléfono, con las reglas del registro (RF02). El correo corporativo es el contacto público (decisión D2): no es la
 * credencial ({@code usuario.correo}), que no se edita (DEC-79, ADR-006). Sin unicidad: no puede revelar cuentas.
 */
public record DatosComercialesPrestador(Correo correoCorporativo, String telefono) {

    public DatosComercialesPrestador {
        Objects.requireNonNull(correoCorporativo, "correoCorporativo");
        Objects.requireNonNull(telefono, "telefono");
    }

    /**
     * Todos los errores a la vez, con los nombres JSON de la API.
     *
     * @throws DatosRegistroInvalidosException con un motivo por campo
     */
    public static DatosComercialesPrestador validar(String correoCorporativo, String telefono) {
        ValidadorCampos v = new ValidadorCampos();
        Correo correo = v.valor("correoCorporativo", () -> new Correo(correoCorporativo));
        String telefonoValido = v.texto("telefono", telefono, RegistroPrestador.LONGITUD_MAXIMA_TELEFONO);
        v.lanzarSiHayErrores();
        return new DatosComercialesPrestador(correo, telefonoValido);
    }
}
