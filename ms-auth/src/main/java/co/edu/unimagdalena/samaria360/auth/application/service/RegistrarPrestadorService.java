package co.edu.unimagdalena.samaria360.auth.application.service;

import co.edu.unimagdalena.samaria360.auth.application.port.in.ComandoRegistroPrestador;
import co.edu.unimagdalena.samaria360.auth.application.port.in.RegistrarPrestadorUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.in.RegistroRecibido;
import co.edu.unimagdalena.samaria360.auth.application.port.out.NotificadorCuentaPort;
import co.edu.unimagdalena.samaria360.auth.application.port.out.PasswordHasherPort;
import co.edu.unimagdalena.samaria360.auth.application.port.out.PrestadorNuevo;
import co.edu.unimagdalena.samaria360.auth.application.port.out.RegistroRepositoryPort;
import co.edu.unimagdalena.samaria360.auth.application.port.out.RelojPort;
import co.edu.unimagdalena.samaria360.auth.domain.exception.RntYaRegistradoException;
import co.edu.unimagdalena.samaria360.auth.domain.model.RegistroPrestador;

import java.util.Objects;
import java.util.UUID;

/**
 * Caso de uso Registrarse, prestador (RF02). Orden (ADR-006): validación de formato y políticas, hash BCrypt siempre, RNT
 * (409, dato público; no depende del correo) y, al final, el correo. La transacción la abre la infraestructura.
 */
public class RegistrarPrestadorService implements RegistrarPrestadorUseCase {

    private final RegistroRepositoryPort registros;
    private final PasswordHasherPort hasher;
    private final NotificadorCuentaPort notificador;
    private final RelojPort reloj;

    public RegistrarPrestadorService(RegistroRepositoryPort registros, PasswordHasherPort hasher,
                                     NotificadorCuentaPort notificador, RelojPort reloj) {
        this.registros = Objects.requireNonNull(registros);
        this.hasher = Objects.requireNonNull(hasher);
        this.notificador = Objects.requireNonNull(notificador);
        this.reloj = Objects.requireNonNull(reloj);
    }

    @Override
    public RegistroRecibido registrar(ComandoRegistroPrestador c) {
        RegistroPrestador datos = RegistroPrestador.validar(c.nombreRepresentanteLegal(), c.razonSocial(), c.rnt(),
                c.correoCorporativo(), c.telefono(), c.contrasena(), c.aceptoPoliticas());
        String hash = hasher.codificar(datos.contrasena().valor());
        if (registros.existeRnt(datos.rnt())) {
            throw new RntYaRegistradoException();
        }
        if (!RegistroComun.atenderCorreoExistente(registros, notificador, datos.correoCorporativo().valor())) {
            registros.registrarPrestador(new PrestadorNuevo(UUID.randomUUID(), datos, hash, RegistroComun.hoy(reloj)));
        }
        return RegistroRecibido.INSTANCIA;
    }
}
