# Llaves del JWT (RS256)

Las llaves reales nunca se suben al repositorio (ver `.gitignore`). Cada integrante genera las suyas:

```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out jwt-privada.pem
openssl rsa -in jwt-privada.pem -pubout -out jwt-publica.pem
```

ms-auth firma con la llave privada y publica la pública en su endpoint JWKS;
el api-gateway valida los tokens con ese JWKS sin consultar a ms-auth en cada petición.
