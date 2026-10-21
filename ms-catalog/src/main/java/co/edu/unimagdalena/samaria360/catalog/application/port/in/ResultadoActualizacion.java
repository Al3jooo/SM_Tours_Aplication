package co.edu.unimagdalena.samaria360.catalog.application.port.in;

/** Qué pasó con una proyección recibida: se aplicó, o se ignoró por ser de una versión menor o igual a la guardada. */
public enum ResultadoActualizacion {
    APLICADA,
    IGNORADA
}
