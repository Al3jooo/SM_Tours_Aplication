package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.in.web.error;

import co.edu.unimagdalena.samaria360.auth.domain.exception.CredencialesInvalidasException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaBloqueadaException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.CuentaInactivaException;
import co.edu.unimagdalena.samaria360.auth.domain.exception.UsuarioNoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Errores como ProblemDetail (RFC 9457) con un {@code codigo} estable. Extiende
 * ResponseEntityExceptionHandler para que los errores estándar de Spring MVC tengan el mismo formato.
 */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

    /** Cuerpo idéntico para correo inexistente y contraseña incorrecta: no revela qué correos existen. */
    @ExceptionHandler(CredencialesInvalidasException.class)
    ProblemDetail credencialesInvalidas(CredencialesInvalidasException ex) {
        return Problemas.crear(HttpStatus.UNAUTHORIZED, CodigoError.CREDENCIALES_INVALIDAS, ex.getMessage());
    }

    @ExceptionHandler(CuentaBloqueadaException.class)
    ProblemDetail cuentaBloqueada(CuentaBloqueadaException ex) {
        ProblemDetail problema = Problemas.crear(HttpStatus.LOCKED, CodigoError.CUENTA_BLOQUEADA, ex.getMessage());
        problema.setProperty("bloqueadoHasta", ex.getBloqueadoHasta());
        return problema;
    }

    @ExceptionHandler(CuentaInactivaException.class)
    ProblemDetail cuentaInactiva(CuentaInactivaException ex) {
        return Problemas.crear(HttpStatus.FORBIDDEN, CodigoError.CUENTA_INACTIVA, ex.getMessage());
    }

    /** Token válido de un usuario que ya no existe. */
    @ExceptionHandler(UsuarioNoEncontradoException.class)
    ProblemDetail usuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        return Problemas.crear(HttpStatus.UNAUTHORIZED, CodigoError.NO_AUTENTICADO, "La sesión no es válida");
    }

    /** Las de Spring Security las resuelven su entry point y su access denied handler, no este advice. */
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    void relanzarExcepcionDeSeguridad(RuntimeException ex) {
        throw ex;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail errorInterno(Exception ex) {
        LOG.error("Error no controlado", ex);
        return Problemas.crear(HttpStatus.INTERNAL_SERVER_ERROR, CodigoError.ERROR_INTERNO,
                "Ocurrió un error inesperado");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            campos.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        ProblemDetail problema = Problemas.crear(status, CodigoError.DATOS_INVALIDOS, "Datos de entrada inválidos");
        problema.setProperty("campos", campos);
        return handleExceptionInternal(ex, problema, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail problema = Problemas.crear(status, CodigoError.DATOS_INVALIDOS,
                "El cuerpo de la solicitud no es un JSON válido");
        return handleExceptionInternal(ex, problema, headers, status, request);
    }

    /** Agrega un código genérico a los errores estándar de Spring MVC (404, 405, 415...). */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> respuesta = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (respuesta != null && respuesta.getBody() instanceof ProblemDetail problema
                && (problema.getProperties() == null || !problema.getProperties().containsKey(Problemas.CODIGO))) {
            CodigoError codigo = statusCode.is5xxServerError() ? CodigoError.ERROR_INTERNO : CodigoError.SOLICITUD_INVALIDA;
            problema.setProperty(Problemas.CODIGO, codigo.name());
        }
        return respuesta;
    }
}
