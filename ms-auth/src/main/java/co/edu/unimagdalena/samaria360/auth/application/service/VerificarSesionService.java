package co.edu.unimagdalena.samaria360.auth.application.service;

import co.edu.unimagdalena.samaria360.auth.application.port.in.VerificarSesionUseCase;
import co.edu.unimagdalena.samaria360.auth.application.port.out.UsuarioRepositoryPort;
import co.edu.unimagdalena.samaria360.auth.domain.service.PoliticaSesion;

import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;

/** ms-auth compara el claim {@code ver} contra su propia tabla {@code usuario} (DEC-29). */
public class VerificarSesionService implements VerificarSesionUseCase {

    private final UsuarioRepositoryPort usuarios;
    private final PoliticaSesion politica;

    public VerificarSesionService(UsuarioRepositoryPort usuarios, PoliticaSesion politica) {
        this.usuarios = Objects.requireNonNull(usuarios);
        this.politica = Objects.requireNonNull(politica);
    }

    @Override
    public boolean sesionVigente(UUID idUsuario, long ver) {
        OptionalLong version = usuarios.buscarVersionCuenta(idUsuario);
        return politica.vigente(version, ver);
    }
}
