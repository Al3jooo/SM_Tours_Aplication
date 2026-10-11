package co.edu.unimagdalena.samaria360.auth.domain.model;

/**
 * Rol del usuario (RNF06). Los nombres coinciden con {@code rol.nombre} y con el claim {@code rol} del JWT.
 */
public enum Rol {
    Turista,
    Prestador,
    Gestor,
    Administrador
}
