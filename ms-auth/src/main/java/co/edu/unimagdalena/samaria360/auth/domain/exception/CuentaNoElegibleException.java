package co.edu.unimagdalena.samaria360.auth.domain.exception;

import co.edu.unimagdalena.samaria360.auth.domain.model.MotivoNoElegible;

/** La cuenta no admite el cambio de rol pedido (409 CUENTA_NO_ELEGIBLE, DEC-104 y DEC-105). */
public class CuentaNoElegibleException extends RuntimeException {

    private final MotivoNoElegible motivo;

    public CuentaNoElegibleException(MotivoNoElegible motivo) {
        super("La cuenta no admite el cambio de rol: " + motivo);
        this.motivo = motivo;
    }

    public MotivoNoElegible getMotivo() {
        return motivo;
    }
}
