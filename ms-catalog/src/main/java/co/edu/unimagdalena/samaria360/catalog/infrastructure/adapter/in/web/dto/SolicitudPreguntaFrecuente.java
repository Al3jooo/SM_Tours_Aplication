package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.dto;

/** Cuerpo de POST /admin/faq y PUT /admin/faq/{id}. Lo valida el dominio (DEC-61). */
public record SolicitudPreguntaFrecuente(String titulo, String cuerpo) {
}
