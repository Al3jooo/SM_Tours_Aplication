package co.edu.unimagdalena.samaria360.booking.application.service;

import co.edu.unimagdalena.samaria360.booking.application.port.in.VerificarSesionUseCase;
import co.edu.unimagdalena.samaria360.booking.application.port.out.CuentaRepositoryPort;
import co.edu.unimagdalena.samaria360.booking.domain.service.PoliticaSesion;

import java.util.Objects;
import java.util.UUID;

/** Compara el claim {@code ver} contra la versión de la proyección {@code cuenta} (DEC-29). */
public class VerificarSesionService implements VerificarSesionUseCase {

    private final CuentaRepositoryPort cuentas;
    private final PoliticaSesion politica;

    public VerificarSesionService(CuentaRepositoryPort cuentas, PoliticaSesion politica) {
        this.cuentas = Objects.requireNonNull(cuentas);
        this.politica = Objects.requireNonNull(politica);
    }

    @Override
    public boolean sesionVigente(UUID idUsuario, long ver) {
        return politica.vigente(cuentas.versionDe(idUsuario), ver);
    }
}
