## Resumen

<!-- Qué cambia y por qué, en dos o tres frases. -->

## Trazabilidad

- **Iteración y sesión:** <!-- p. ej. iteración E, sesión B2 -->
- **Caso de uso:** <!-- enlace a docs/casos-de-uso/ -->
- **Requisitos:** <!-- RF y RNF -->
- **Decisiones:** <!-- DEC, ADR -->
- **Wireframes:** <!-- WF -->

## Cómo se probó

<!-- Pruebas nuevas o modificadas y recorrido manual, si lo hubo. -->

## Lista de verificación

- [ ] `./mvnw verify` pasa en cada servicio Java que cambió, y `npm run lint`, `npm run build` y `npm test` en `frontend/`.
- [ ] Hay pruebas para lo nuevo en la capa que corresponde (domain, application, infrastructure, frontend).
- [ ] No se editó ninguna migración ya aplicada; los cambios de esquema van en migraciones nuevas.
- [ ] `docs/trazabilidad.md`, los contratos de `docs/sad/api/` y los CU están al día.
- [ ] Lo que queda fuera del alcance se muestra deshabilitado y figura como pendiente.
- [ ] No se suben `.env`, llaves de `infra/keys/` ni datos personales.
- [ ] Las dependencias nuevas, si las hay, están justificadas en este PR.
