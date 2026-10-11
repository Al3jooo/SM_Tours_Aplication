package co.edu.unimagdalena.samaria360.auth.infrastructure.config;

import co.edu.unimagdalena.samaria360.auth.application.port.in.IniciarSesionUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.out.PasswordHasherPort;
import co.edu.unimagdalena.samaria360.auth.application.port.out.RelojPort;
import co.edu.unimagdalena.samaria360.auth.application.port.out.TokenProviderPort;
import co.edu.unimagdalena.samaria360.auth.application.port.out.UsuarioRepositoryPort;
import co.edu.unimagdalena.samaria360.auth.application.service.ConsultarUsuarioActualService;
import co.edu.unimagdalena.samaria360.auth.application.service.IniciarSesionService;
import co.edu.unimagdalena.samaria360.auth.domain.service.PoliticaBloqueo;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/** Registra como beans las clases de domain y application, que no llevan anotaciones de Spring. */
@Configuration
public class BeanConfig {

    @Bean
    public PoliticaBloqueo politicaBloqueo() {
        return new PoliticaBloqueo();
    }

    @Bean
    public IniciarSesionService iniciarSesionService(UsuarioRepositoryPort usuarios, PasswordHasherPort hasher,
                                                     TokenProviderPort tokens, RelojPort reloj,
                                                     PoliticaBloqueo politica) {
        return new IniciarSesionService(usuarios, hasher, tokens, reloj, politica);
    }

    /** El que reciben los adaptadores de entrada: el servicio puro envuelto en la transacción. */
    @Bean
    @Primary
    public IniciarSesionUseCase iniciarSesionUseCase(IniciarSesionService servicio) {
        return new IniciarSesionTransaccional(servicio);
    }

    @Bean
    public ConsultarUsuarioActualService consultarUsuarioActualService(UsuarioRepositoryPort usuarios) {
        return new ConsultarUsuarioActualService(usuarios);
    }
}
