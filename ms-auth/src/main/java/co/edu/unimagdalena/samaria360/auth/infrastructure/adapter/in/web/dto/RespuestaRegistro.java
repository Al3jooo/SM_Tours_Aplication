package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.dto;

/**
 * Cuerpo del 202 del registro: siempre el mismo, se haya creado la cuenta o no (ADR-006). Sin id ni {@code Location}.
 */
public record RespuestaRegistro(String estado) {

    public static final RespuestaRegistro RECIBIDO = new RespuestaRegistro("RECIBIDO");
}
