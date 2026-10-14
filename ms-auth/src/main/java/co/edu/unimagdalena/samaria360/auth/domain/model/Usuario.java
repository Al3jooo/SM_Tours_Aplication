package co.edu.unimagdalena.samaria360.auth.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Cuenta de usuario con el estado necesario para el inicio de sesión (RF15).
 * La hora actual siempre llega como parámetro; el dominio nunca la consulta.
 */
public class Usuario {

    private final UUID id;
    private final String correo;
    private final String contrasenaHash;
    private final Rol rol;
    private final EstadoCuenta estadoCuenta;
    private int intentosFallidos;
    private Instant bloqueadoHasta;

    public Usuario(UUID id, String correo, String contrasenaHash, Rol rol, EstadoCuenta estadoCuenta,
                   int intentosFallidos, Instant bloqueadoHasta) {
        this.id = Objects.requireNonNull(id, "id");
        this.correo = requerirTexto(correo, "correo");
        this.contrasenaHash = requerirTexto(contrasenaHash, "contrasenaHash");
        this.rol = Objects.requireNonNull(rol, "rol");
        this.estadoCuenta = Objects.requireNonNull(estadoCuenta, "estadoCuenta");
        if (intentosFallidos < 0) {
            throw new IllegalArgumentException("intentosFallidos no puede ser negativo");
        }
        this.intentosFallidos = intentosFallidos;
        this.bloqueadoHasta = bloqueadoHasta;
    }

    /** Bloqueada mientras {@code ahora < bloqueadoHasta}; al llegar a esa hora se desbloquea sola. */
    public boolean estaBloqueadoEn(Instant ahora) {
        Objects.requireNonNull(ahora, "ahora");
        return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
    }

    /** Suma un intento fallido consecutivo y devuelve el nuevo conteo. */
    public int registrarIntentoFallido() {
        return ++intentosFallidos;
    }

    /** Bloquea la cuenta hasta la hora indicada y reinicia el conteo de intentos. */
    public void bloquearHasta(Instant hasta) {
        this.bloqueadoHasta = Objects.requireNonNull(hasta, "hasta");
        this.intentosFallidos = 0;
    }

    /** Solo el conteo de intentos; no toca el bloqueo ni cuenta como acceso. */
    public void reiniciarIntentos() {
        this.intentosFallidos = 0;
    }

    /** Acceso concedido (se emitió el token): reinicia el conteo y quita un bloqueo ya vencido. */
    public void registrarAccesoExitoso() {
        this.intentosFallidos = 0;
        this.bloqueadoHasta = null;
    }

    public UUID getId() {
        return id;
    }

    public String getCorreo() {
        return correo;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public Rol getRol() {
        return rol;
    }

    public EstadoCuenta getEstadoCuenta() {
        return estadoCuenta;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public Optional<Instant> getBloqueadoHasta() {
        return Optional.ofNullable(bloqueadoHasta);
    }

    private static String requerirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " es obligatorio");
        }
        return valor;
    }
}
