-- «Mis reservas» con filtros (RF12, del lado del turista): por turista, de la actividad que empieza más tarde a la que
-- empieza más temprano, con desempate por id para que la paginación sea estable. La consulta filtra además por
-- fecha_fin y estado sobre las filas de ese turista.
CREATE INDEX ix_reserva_turista_inicio ON reserva (id_turista, fecha_inicio DESC, id_reserva DESC);
