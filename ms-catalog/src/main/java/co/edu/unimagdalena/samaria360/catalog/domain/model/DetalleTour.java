package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.math.BigDecimal;

/**
 * Tour o recorrido turístico (RF10): punto de encuentro, duración en horas, nivel de dificultad (opcional en
 * el esquema) y aforo máximo por grupo (obligatorio).
 */
public record DetalleTour(String puntoEncuentro, BigDecimal duracionHoras, String nivelDificultad,
                          int aforoMaxGrupo) implements DetalleCategoria {

    public DetalleTour {
        Validaciones.texto(puntoEncuentro, "puntoEncuentro");
        Validaciones.positivo(duracionHoras, "duracionHoras");
        Validaciones.positivo(aforoMaxGrupo, "aforoMaxGrupo");
    }

    @Override
    public Categoria categoria() {
        return Categoria.Tour;
    }
}
