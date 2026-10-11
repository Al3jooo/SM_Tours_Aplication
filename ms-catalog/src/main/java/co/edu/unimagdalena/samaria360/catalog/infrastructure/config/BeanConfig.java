package co.edu.unimagdalena.samaria360.catalog.infrastructure.config;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.BuscarOfertaUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.OfertaRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.application.service.BuscarOfertaService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registra como beans las clases de application, que no llevan anotaciones de Spring. */
@Configuration
public class BeanConfig {

    @Bean
    public BuscarOfertaUseCase buscarOfertaUseCase(OfertaRepositoryPort ofertas) {
        return new BuscarOfertaService(ofertas);
    }
}
