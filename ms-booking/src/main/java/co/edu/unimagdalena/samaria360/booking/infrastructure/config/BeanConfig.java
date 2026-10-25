package co.edu.unimagdalena.samaria360.booking.infrastructure.config;

import co.edu.unimagdalena.samaria360.booking.application.port.in.ActualizarCuentaUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.in.CancelarReservaUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ResumirDisponibilidadUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.in.ReservarServicioUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.in.VerificarSesionUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.out.CatalogoPort;
import co.edu.unimagdalena.samaria360.booking.application.port.out.CuentaRepositoryPort;
import co.edu.unimagdalena.samaria360.booking.application.port.out.InventarioPort;
import co.edu.unimagdalena.samaria360.booking.application.port.out.OfertaMaterializadaPort;
import co.edu.unimagdalena.samaria360.booking.application.port.out.RelojPort;
import co.edu.unimagdalena.samaria360.booking.application.port.out.ReservaRepositoryPort;
import co.edu.unimagdalena.samaria360.booking.application.service.ActualizarCuentaService;
import co.edu.unimagdalena.samaria360.booking.application.service.CancelarReservaService;
import co.edu.unimagdalena.samaria360.booking.application.service.CompletarReservasService;
import co.edu.unimagdalena.samaria360.booking.application.service.ConsultarReservasService;
import co.edu.unimagdalena.samaria360.booking.application.service.DisponibilidadService;
import co.edu.unimagdalena.samaria360.booking.application.service.MaterializarInventarioService;
import co.edu.unimagdalena.samaria360.booking.application.service.ReservarServicioService;
import co.edu.unimagdalena.samaria360.booking.application.service.VerificarSesionService;
import co.edu.unimagdalena.samaria360.booking.domain.model.HorarioReservas;
import co.edu.unimagdalena.samaria360.booking.domain.service.PoliticaReserva;
import co.edu.unimagdalena.samaria360.booking.domain.service.PoliticaSesion;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;

/** Registra como beans las clases de domain y application, que no llevan anotaciones de Spring. */
@Configuration
public class BeanConfig {

    /** Check-in y check-out de los alojamientos: los usan las reglas y la respuesta de la disponibilidad. */
    @Bean
    public HorarioReservas horarioReservas(ReservasPropiedades propiedades) {
        return new HorarioReservas(propiedades.horaCheckIn(), propiedades.horaCheckOut());
    }

    @Bean
    public PoliticaReserva politicaReserva(HorarioReservas horario) {
        return new PoliticaReserva(horario);
    }

    /** DEC-135: materializa la oferta del catálogo bajo demanda; lo usan la disponibilidad, el resumen y reservar. */
    @Bean
    public MaterializarInventarioService materializarInventarioService(CatalogoPort catalogo,
                                                                       InventarioPort inventario,
                                                                       OfertaMaterializadaPort marcas,
                                                                       RelojPort reloj) {
        return new MaterializarInventarioService(catalogo, inventario, marcas, reloj);
    }

    @Bean
    public ReservarServicioService reservarServicioService(ReservaRepositoryPort reservas, InventarioPort inventario,
                                                           CatalogoPort catalogo,
                                                           MaterializarInventarioService materializacion,
                                                           RelojPort reloj, PoliticaReserva politica) {
        return new ReservarServicioService(reservas, inventario, catalogo, materializacion, reloj, politica);
    }

    /** El que reciben los adaptadores de entrada: las fases del servicio coordinadas con las transacciones. */
    @Bean
    @Primary
    public ReservarServicioUseCase reservarServicioUseCase(ReservarServicioService servicio,
                                                           PlatformTransactionManager transacciones) {
        return new ReservarServicioTransaccional(servicio, new TransactionTemplate(transacciones));
    }

    @Bean
    public CancelarReservaService cancelarReservaService(ReservaRepositoryPort reservas, InventarioPort inventario,
                                                         RelojPort reloj) {
        return new CancelarReservaService(reservas, inventario, reloj);
    }

    @Bean
    @Primary
    public CancelarReservaUseCase cancelarReservaUseCase(CancelarReservaService servicio,
                                                         PlatformTransactionManager transacciones) {
        return new CancelarReservaTransaccional(servicio, new TransactionTemplate(transacciones));
    }

    @Bean
    public ConsultarReservasService consultarReservasService(ReservaRepositoryPort reservas) {
        return new ConsultarReservasService(reservas);
    }

    @Bean
    public DisponibilidadService disponibilidadService(InventarioPort inventario,
                                                       MaterializarInventarioService materializacion,
                                                       RelojPort reloj, HorarioReservas horario) {
        return new DisponibilidadService(inventario, materializacion, reloj, horario);
    }

    /** El que recibe el adaptador de entrada: el resumen con un plazo total para materializar (DEC-155). */
    @Bean
    @Primary
    public ResumirDisponibilidadUseCase resumirDisponibilidadUseCase(
            DisponibilidadService servicio,
            @Value("${samaria360.disponibilidad.plazo-materializacion-resumen:2500ms}") Duration plazo) {
        return new ResumenDisponibilidadConPlazo(servicio, plazo);
    }

    /** RF52: un único UPDATE condicional; la transacción la abre el repositorio. */
    @Bean
    public CompletarReservasService completarReservasService(ReservaRepositoryPort reservas, RelojPort reloj) {
        return new CompletarReservasService(reservas, reloj);
    }

    @Bean
    public PoliticaSesion politicaSesion() {
        return new PoliticaSesion();
    }

    @Bean
    public VerificarSesionUseCase verificarSesionUseCase(CuentaRepositoryPort cuentas, PoliticaSesion politica) {
        return new VerificarSesionService(cuentas, politica);
    }

    /** ADR-008: la proyección de cuentas es un solo UPSERT condicional; la transacción la abre el repositorio. */
    @Bean
    public ActualizarCuentaUseCase actualizarCuentaUseCase(CuentaRepositoryPort cuentas, RelojPort reloj) {
        return new ActualizarCuentaService(cuentas, reloj);
    }
}
