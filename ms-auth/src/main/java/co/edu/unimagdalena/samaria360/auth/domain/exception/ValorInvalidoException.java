package co.edu.unimagdalena.samaria360.auth.domain.exception;

import co.edu.unimagdalena.samaria360.auth.domain.model.MotivoInvalido;

import java.util.Objects;

/** Un objeto de valor rechazó su valor. El validador del registro lo asocia al campo que lo recibió. */
public class ValorInvalidoException extends RuntimeException {

    private final MotivoInvalido motivo;

    public ValorInvalidoException(MotivoInvalido motivo) {
        super(motivo.name());
        this.motivo = Objects.requireNonNull(motivo);
    }

    public MotivoInvalido getMotivo() {
        return motivo;
    }
}
