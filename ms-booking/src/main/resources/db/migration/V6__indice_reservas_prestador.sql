-- Historial de reservas del Prestador (CU «Consultar historial», RF12; DEC-191): por prestador, de la actividad que
-- empieza más tarde a la que empieza más temprano, con desempate por id, como V2 para el turista. id_prestador ya está
-- en reserva desde V1 (se copia del catálogo al reservar): no hay relleno. ix_reserva_prestador_entrada (V1) se
-- conserva.
CREATE INDEX ix_reserva_prestador_inicio ON reserva (id_prestador, fecha_inicio DESC, id_reserva DESC);
