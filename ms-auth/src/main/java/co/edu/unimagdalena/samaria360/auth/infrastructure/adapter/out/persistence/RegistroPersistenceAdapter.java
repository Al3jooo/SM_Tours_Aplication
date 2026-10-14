package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence;

import co.edu.unimagdalena.samaria360.auth.application.port.out.PrestadorNuevo;
import co.edu.unimagdalena.samaria360.auth.application.port.out.RegistroRepositoryPort;
import co.edu.unimagdalena.samaria360.auth.application.port.out.TuristaNuevo;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CorreoYaRegistradoException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.RntYaRegistradoException;
import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoCuenta;
import co.edu.unimagdalena.samaria360.auth.domain.model.EstadoValidacion;
import co.edu.unimagdalena.samaria360.auth.domain.model.RegistroPrestador;
import co.edu.unimagdalena.samaria360.auth.domain.model.RegistroTurista;
import co.edu.unimagdalena.samaria360.auth.domain.model.Rol;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence.entity.PrestadorEntity;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence.entity.RolEntity;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence.entity.TuristaCategoriaInteresEntity;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence.entity.TuristaEntity;
import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence.entity.UsuarioEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Inserta el usuario y su subtipo (RF01, RF02). Exige la transacción del caso de uso (la abren los decoradores
 * {@code Registrar…Transaccional}): si falla el subtipo, no queda el usuario. Con {@code flush} las restricciones únicas
 * se comprueban aquí mismo y se traducen a excepciones del dominio.
 */
@Component
public class RegistroPersistenceAdapter implements RegistroRepositoryPort {

    private final EntityManager em;
    private final UsuarioJpaRepository usuarios;
    private final PrestadorJpaRepository prestadores;
    private final RolJpaRepository roles;

    public RegistroPersistenceAdapter(EntityManager em, UsuarioJpaRepository usuarios,
                                      PrestadorJpaRepository prestadores, RolJpaRepository roles) {
        this.em = em;
        this.usuarios = usuarios;
        this.prestadores = prestadores;
        this.roles = roles;
    }

    @Override
    public Optional<UUID> buscarIdPorCorreo(String correo) {
        return usuarios.buscarIdPorCorreo(correo);
    }

    @Override
    public boolean existeRnt(String rnt) {
        return prestadores.existsByRnt(rnt);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarTurista(TuristaNuevo nuevo) {
        RegistroTurista datos = nuevo.datos();
        UUID id = nuevo.idUsuario();
        insertar(() -> {
            em.persist(usuario(id, Rol.Turista, datos.correo().valor(), nuevo.contrasenaHash(), nuevo.fechaRegistro()));
            em.persist(new TuristaEntity(id, datos.primerNombre(), datos.segundoNombre(), datos.primerApellido(),
                    datos.segundoApellido(), datos.paisOrigen().valor(), true));
            datos.categoriasInteres().forEach(categoria ->
                    em.persist(new TuristaCategoriaInteresEntity(id, categoria.name())));
        });
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void registrarPrestador(PrestadorNuevo nuevo) {
        RegistroPrestador datos = nuevo.datos();
        UUID id = nuevo.idUsuario();
        String correo = datos.correoCorporativo().valor();
        insertar(() -> {
            em.persist(usuario(id, Rol.Prestador, correo, nuevo.contrasenaHash(), nuevo.fechaRegistro()));
            em.persist(new PrestadorEntity(id, datos.razonSocial(), datos.rnt(), datos.telefono(),
                    datos.nombreRepresentanteLegal(), correo, EstadoValidacion.Pendiente.texto(), true));
        });
    }

    private UsuarioEntity usuario(UUID id, Rol rol, String correo, String hash, LocalDate fecha) {
        RolEntity rolEntity = roles.findByNombre(rol.name())
                .orElseThrow(() -> new IllegalStateException("Falta el rol " + rol + " en la tabla rol (V10)"));
        return UsuarioEntity.nueva(id, rolEntity, correo, hash, fecha, EstadoCuenta.Activo);
    }

    /** Persiste y hace flush: una restricción única violada se traduce aquí, dentro de la transacción. */
    private void insertar(Runnable insercion) {
        try {
            insercion.run();
            em.flush();
        } catch (RuntimeException error) {
            Optional<String> restriccion = RestriccionesUnicas.violada(error);
            if (restriccion.filter(RestriccionesUnicas.CORREO::equals).isPresent()) {
                throw new CorreoYaRegistradoException();
            }
            if (restriccion.filter(RestriccionesUnicas.RNT::equals).isPresent()) {
                throw new RntYaRegistradoException();
            }
            throw error;
        }
    }
}
