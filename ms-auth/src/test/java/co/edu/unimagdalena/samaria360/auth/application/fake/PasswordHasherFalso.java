package co.edu.unimagdalena.samaria360.auth.application.fake;

import co.edu.unimagdalena.samaria360.auth.application.port.out.PasswordHasherPort;

import java.util.ArrayList;
import java.util.List;

/** Una contraseña coincide con el hash {@code "hash:" + plana}. Registra cada llamada. */
public class PasswordHasherFalso implements PasswordHasherPort {

    private final List<String> hashesConsultados = new ArrayList<>();

    public static String hashDe(String plana) {
        return "hash:" + plana;
    }

    @Override
    public boolean coincide(String plana, String hash) {
        hashesConsultados.add(hash);
        return hash.equals(hashDe(plana));
    }

    public int llamadas() {
        return hashesConsultados.size();
    }

    public List<String> hashesConsultados() {
        return List.copyOf(hashesConsultados);
    }
}
