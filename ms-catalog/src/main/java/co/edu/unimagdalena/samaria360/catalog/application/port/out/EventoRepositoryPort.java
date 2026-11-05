package co.edu.unimagdalena.samaria360.catalog.application.port.out;

import co.edu.unimagdalena.samaria360.catalog.domain.model.EventoTuristico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;

import java.time.LocalDate;

/**
 * Colección {@code eventos} (RF25, M013). Un solo puerto para el Gestor y la agenda pública, como los avisos: el filtro
 * de la agenda es de fechas, no una regla de visibilidad que deba protegerse (a diferencia de RF16 en servicios).
 */
public interface EventoRepositoryPort {

    /** Un ObjectId nuevo, en su forma textual, para un evento que todavía no se guardó. */
    String nuevoId();

    void insertar(EventoTuristico evento);

    /** Todos los eventos, del más reciente al más antiguo por {@code fechaRegistro} (desempate por id; DEC-238). */
    PaginaResultados<EventoTuristico> listar(int pagina, int tamano);

    /** Los eventos con {@code fechaFin} igual o posterior a {@code desde}, por {@code fechaInicio} ascendente (DEC-215). */
    PaginaResultados<EventoTuristico> vigentes(LocalDate desde, int pagina, int tamano);
}
