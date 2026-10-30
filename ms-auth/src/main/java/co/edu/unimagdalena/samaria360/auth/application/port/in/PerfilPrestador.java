package co.edu.unimagdalena.samaria360.auth.application.port.in;

/**
 * Perfil comercial del Prestador (RF43; DEC-188). Solo lectura: {@code correo} (la credencial), el representante
 * legal, la razón social y el RNT (cierra O16: el Prestador lo ve antes de registrar un servicio). Editables: el
 * correo corporativo de contacto y el teléfono.
 *
 * @param razonSocial opcional (V5)
 */
public record PerfilPrestador(String correo, String representanteLegal, String razonSocial, String rnt,
                              String correoCorporativo, String telefono) {
}
