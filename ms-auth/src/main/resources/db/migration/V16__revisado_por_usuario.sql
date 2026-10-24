-- «Cambiar rol de cuenta» (RF45, DEC-109): revocar o modificar a un Administrador borra su fila de «administrador». La
-- revisión del prestador (V11) apuntaba a esa tabla, así que borrar a quien revisó fallaba. Es la única FK hacia un
-- subtipo de personal. Ahora apunta a «usuario»: se conserva quién revisó (RF28) aunque después pierda el rol.
ALTER TABLE prestador
    DROP CONSTRAINT fk_prestador_revisado_por,
    ADD CONSTRAINT fk_prestador_revisado_por FOREIGN KEY (revisado_por) REFERENCES usuario (id_usuario);
