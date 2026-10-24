package co.edu.unimagdalena.samaria360.booking.domain.model;

/** Rol de la cuenta, como lo publica ms-auth en el evento {@code CuentaActualizada} (RNF06). */
public enum RolCuenta {
    Turista,
    Prestador,
    Gestor,
    Administrador;

    /**
     * Gestor y Administrador: sin fila en la proyección, su token no se acepta (DEC-110, cierre de L3). Recibe el claim
     * {@code rol} del token tal como llega; un valor nulo o desconocido no es privilegiado.
     */
    public static boolean privilegiado(String rol) {
        return Gestor.name().equals(rol) || Administrador.name().equals(rol);
    }
}
