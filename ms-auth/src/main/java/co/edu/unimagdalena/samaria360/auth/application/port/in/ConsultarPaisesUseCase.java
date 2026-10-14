package co.edu.unimagdalena.samaria360.auth.application.port.in;

import java.util.List;

/** Países de origen admitidos en el registro (RF01): códigos ISO 3166-1 alfa-2 (decisión D5). */
public interface ConsultarPaisesUseCase {

    List<String> consultar();
}
