package co.edu.unimagdalena.samaria360.catalog.application.port.in;

import co.edu.unimagdalena.samaria360.catalog.domain.model.EventoTuristico;
import co.edu.unimagdalena.samaria360.catalog.domain.model.PaginaResultados;

/** CU «Consultar agenda de eventos» (RF25). Público, sin token (DEC-239). */
public interface ConsultarAgendaUseCase {

    int TAMANO_PAGINA = 10;

    /** Los eventos vigentes hoy en Bogotá, por {@code fechaInicio} ascendente, en páginas de 10 desde 0 (DEC-215). */
    PaginaResultados<EventoTuristico> agenda(int pagina);
}
