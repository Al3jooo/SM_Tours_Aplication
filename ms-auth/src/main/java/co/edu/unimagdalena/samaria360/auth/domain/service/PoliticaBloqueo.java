package co.edu.unimagdalena.samaria360.auth.domain.service;

import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaBloqueadaException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaInactivaException;
import co.edu.unimagdalena.samaria360.auth.domain.model.Usuario;

import java.time.Duration;
import java.time.Instant;

/**
 * Política de bloqueo del inicio de sesión (RF15): con 3 intentos fallidos consecutivos la cuenta
 * queda bloqueada 15 minutos y el conteo vuelve a 0; un login exitoso también reinicia el conteo.
 */
public class PoliticaBloqueo {

    public static final int MAX_INTENTOS_FALLIDOS = 3;
    public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

    /**
     * Debe evaluarse antes de verificar la contraseña: una cuenta bloqueada se rechaza aunque la
     * contraseña sea correcta. No modifica el usuario.
     *
     * @throws CuentaInactivaException si la cuenta no está Activa (RF22, RF62)
     * @throws CuentaBloqueadaException si {@code ahora} es anterior a {@code bloqueadoHasta}
     */
    public void verificarAcceso(Usuario usuario, Instant ahora) {
        if (!usuario.getEstadoCuenta().permiteIniciarSesion()) {
            throw new CuentaInactivaException(usuario.getEstadoCuenta());
        }
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

    public void registrarLoginExitoso(Usuario usuario) {
        usuario.registrarAccesoExitoso();
    }
}
