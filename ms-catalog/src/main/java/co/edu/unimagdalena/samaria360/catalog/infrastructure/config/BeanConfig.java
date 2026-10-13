package co.edu.unimagdalena.samaria360.catalog.infrastructure.config;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.BuscarOfertaUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.in.ConsultarOpcionesFiltroUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.OfertaRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.OpcionesFiltroRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.application.service.BuscarOfertaService;
import co.edu.unimagdalena.samaria360.catalog.application.service.ConsultarOpcionesFiltroService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra como beans las clases de application, que no llevan anotaciones de Spring. */
@Configuration
public class BeanConfig {

    @Bean
    public BuscarOfertaUseCase buscarOfertaUseCase(OfertaRepositoryPort ofertas) {
        return new BuscarOfertaService(ofertas);
    }

    @Bean
    public ConsultarOpcionesFiltroUseCase consultarOpcionesFiltroUseCase(OpcionesFiltroRepositoryPort opciones) {
        return new ConsultarOpcionesFiltroService(opciones);
    }
}
