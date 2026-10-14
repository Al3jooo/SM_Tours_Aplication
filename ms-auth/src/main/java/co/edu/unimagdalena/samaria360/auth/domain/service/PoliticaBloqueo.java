package co.edu.unimagdalena.samaria360.auth.domain.service;

import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaBloqueadaException;
import co.edu.unimagdalena.samaria360.auth.domain.model.Usuario;

import java.time.Duration;
import java.time.Instant;

/**
 * Política de bloqueo del inicio de sesión (RF15): con 3 intentos fallidos consecutivos la cuenta
 * queda bloqueada 15 minutos y el conteo vuelve a 0; una contraseña correcta también reinicia el conteo.
 * El estado de la cuenta (RF22) y la validación del prestador (RF02) los evalúa {@link PoliticaAcceso},
 * después de la contraseña.
 */
public class PoliticaBloqueo {

    public static final int MAX_INTENTOS_FALLIDOS = 3;
    public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

    /**
     * Lo único que se evalúa antes de verificar la contraseña: una cuenta bloqueada se rechaza aunque la
     * contraseña sea correcta. Revela que la cuenta existe (riesgo residual aceptado en ADR-006). No modifica el usuario.
     *
     * @throws CuentaBloqueadaException si {@code ahora} es anterior a {@code bloqueadoHasta}
     */
    public void verificarBloqueo(Usuario usuario, Instant ahora) {
        if (usuario.estaBloqueadoEn(ahora)) {
            throw new CuentaBloqueadaException(usuario.getBloqueadoHasta().orElseThrow());
        }
    }

    /** Con la cuenta bloqueada no suma intentos ni extiende el bloqueo. */
    public void registrarIntentoFallido(Usuario usuario, Instant ahora) {
        if (usuario.estaBloqueadoEn(ahora)) {
            return;
        }
        if (usuario.registrarIntentoFallido() >= MAX_INTENTOS_FALLIDOS) {
            usuario.bloquearHasta(ahora.plus(DURACION_BLOQUEO));
        }
    }

    /**
     * Contraseña correcta, pero el acceso se niega por el estado de la cuenta (403): no es un intento fallido y el
     * conteo vuelve a 0. No se registra como acceso.
     */
    public void reiniciarIntentos(Usuario usuario) {
        usuario.reiniciarIntentos();
    }

    /** Solo cuando se emite el token. */
    public void registrarLoginExitoso(Usuario usuario) {
        usuario.registrarAccesoExitoso();
    }
}
