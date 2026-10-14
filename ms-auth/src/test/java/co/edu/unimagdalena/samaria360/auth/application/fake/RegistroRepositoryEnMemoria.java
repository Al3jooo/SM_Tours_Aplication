package co.edu.unimagdalena.samaria360.auth.application.fake;

import co.edu.unimagdalena.samaria360.auth.application.port.out.PrestadorNuevo;
import co.edu.unimagdalena.samaria360.auth.application.port.out.RegistroRepositoryPort;
import co.edu.unimagdalena.samaria360.auth.application.port.out.TuristaNuevo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Registro en memoria. Anota el orden de las consultas para comprobar que el RNT se mira antes que el correo. */
public class RegistroRepositoryEnMemoria implements RegistroRepositoryPort {

    private final Map<String, UUID> correos = new HashMap<>();
    private final Set<String> rnts = new HashSet<>();
    private final List<TuristaNuevo> turistas = new ArrayList<>();
    private final List<PrestadorNuevo> prestadores = new ArrayList<>();
    private final List<String> consultas = new ArrayList<>();

    public RegistroRepositoryEnMemoria conCorreo(String correo, UUID id) {
        correos.put(correo, id);
        return this;
    }

    public RegistroRepositoryEnMemoria conRnt(String rnt) {
        rnts.add(rnt);
        return this;
    }

    @Override
    public Optional<UUID> buscarIdPorCorreo(String correo) {
        consultas.add("correo");
        return Optional.ofNullable(correos.get(correo));
    }

    @Override
    public boolean existeRnt(String rnt) {
        consultas.add("rnt");
        return rnts.contains(rnt);
    }

    @Override
    public void registrarTurista(TuristaNuevo turista) {
        turistas.add(turista);
        correos.put(turista.datos().correo().valor(), turista.idUsuario());
    }

    @Override
    public void registrarPrestador(PrestadorNuevo prestador) {
        prestadores.add(prestador);
        correos.put(prestador.datos().correoCorporativo().valor(), prestador.idUsuario());
        rnts.add(prestador.datos().rnt());
    }

    public List<TuristaNuevo> turistas() {
        return List.copyOf(turistas);
    }

    public List<PrestadorNuevo> prestadores() {
        return List.copyOf(prestadores);
    }

    /** «rnt» y «correo», en el orden en que se consultaron. */
    public List<String> consultas() {
        return List.copyOf(consultas);
    }
}
