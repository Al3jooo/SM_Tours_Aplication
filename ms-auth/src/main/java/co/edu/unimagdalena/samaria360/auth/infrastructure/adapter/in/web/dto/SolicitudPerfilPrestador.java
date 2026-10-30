package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto;

/**
 * Cuerpo de {@code PUT /api/v1/auth/prestador/perfil} (RF43; DEC-188): solo lo editable. Un {@code correo},
 * {@code rnt}, {@code razonSocial} o {@code representanteLegal} en el cuerpo se ignora.
 */
public record SolicitudPerfilPrestador(String correoCorporativo, String telefono) {
}
