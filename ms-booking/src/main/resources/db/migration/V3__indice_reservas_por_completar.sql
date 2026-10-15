-- RF52: la tarea que pasa de Confirmada a Completada busca solo las reservas Confirmadas que ya terminaron. El índice
-- parcial deja fuera las Canceladas y las Completadas, que no vuelven a cambiar.
CREATE INDEX ix_reserva_confirmada_fin ON reserva (fecha_fin) WHERE estado = 'Confirmada';
