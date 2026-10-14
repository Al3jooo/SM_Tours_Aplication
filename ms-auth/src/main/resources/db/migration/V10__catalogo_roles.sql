-- Catálogo de roles (RNF06). Hasta ahora solo lo cargaba la semilla V2 (perfiles dev y test), así que sin semilla la
-- tabla rol quedaba vacía y el registro (RF01, RF02) fallaría por fk_usuario_rol. Mismos id y nombres que V2: con la
-- semilla ya aplicada no cambia nada.
INSERT INTO rol (id_rol, nombre) VALUES
    (1, 'Turista'),
    (2, 'Prestador'),
    (3, 'Gestor'),
    (4, 'Administrador')
ON CONFLICT (id_rol) DO NOTHING;
