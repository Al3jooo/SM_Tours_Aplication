# Llaves del JWT (RS256)

Las llaves reales nunca se suben al repositorio (ver `.gitignore`). Cada integrante genera las suyas:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-privada.pem
openssl rsa -in jwt-privada.pem -pubout -out jwt-publica.pem
```

ms-auth firma con la llave privada y publica la pública en su endpoint JWKS. El api-gateway **no** valida los
tokens: los reenvía, y cada servicio los valida con ese JWKS (ADR-005).

- **Arranque local (`./mvnw`)**: ms-auth lee las llaves de las rutas `JWT_LLAVE_PRIVADA` y `JWT_LLAVE_PUBLICA`
  (`.env`).
- **Docker Compose**: las llaves se montan como *secrets* de solo lectura en `/run/secrets/` de ms-auth y nunca se
  copian a una imagen (los `.dockerignore` excluyen `*.pem`). En Linux, el archivo debe ser legible por el usuario
  del contenedor (`chmod 644 jwt-*.pem`).
