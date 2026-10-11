package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Clave pública del JWT para que el API Gateway valide los tokens sin consultar a ms-auth. */
@RestController
public class JwksController {

    private final Map<String, Object> jwks;

    public JwksController(RSAKey llaveJwt) {
        this.jwks = new JWKSet(llaveJwt.toPublicJWK()).toJSONObject();
    }

    @GetMapping(value = "/api/v1/auth/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() {
        return jwks;
    }
}
