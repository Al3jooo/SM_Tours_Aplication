package co.edu.unimagdalena.samaria360.catalog.infrastructure.config;

import co.edu.unimagdalena.samaria360.catalog.application.port.in.ActualizarCuentaUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.in.BuscarOfertaUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.in.ConsultarOpcionesFiltroUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.in.ConsultarServicioUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.in.VerificarSesionUseCase;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.CuentaRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.OfertaRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.OpcionesFiltroRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.ServicioRepositoryPort;
import co.edu.unimagdalena.samaria360.catalog.application.port.out.VisibilidadPrestadorPort;
import co.edu.unimagdalena.samaria360.catalog.application.service.ActualizarCuentaService;
import co.edu.unimagdalena.samaria360.catalog.application.service.BuscarOfertaService;
import co.edu.unimagdalena.samaria360.catalog.application.service.ConsultarOpcionesFiltroService;
import co.edu.unimagdalena.samaria360.catalog.application.service.ConsultarServicioService;
import co.edu.unimagdalena.samaria360.catalog.application.service.VerificarSesionService;
import co.edu.unimagdalena.samaria360.catalog.domain.service.PoliticaSesion;
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

    @Bean
    public ConsultarServicioUseCase consultarServicioUseCase(ServicioRepositoryPort servicios) {
        return new ConsultarServicioService(servicios);
    }

    @Bean
    public PoliticaSesion politicaSesion() {
        return new PoliticaSesion();
    }

    @Bean
    public VerificarSesionUseCase verificarSesionUseCase(CuentaRepositoryPort cuentas, PoliticaSesion politica) {
        return new VerificarSesionService(cuentas, politica);
    }

    @Bean
    public ActualizarCuentaUseCase actualizarCuentaUseCase(CuentaRepositoryPort cuentas,
                                                           VisibilidadPrestadorPort visibilidad) {
        return new ActualizarCuentaService(cuentas, visibilidad);
    }
}
