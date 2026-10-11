package co.edu.unimagdalena.samaria360.catalog.infrastructure.adapter.in.web.error;

import co.edu.unimagdalena.samaria360.catalog.domain.exception.CriteriosBusquedaInvalidosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Errores como ProblemDetail (RFC 9457) con un {@code codigo} estable, igual que ms-auth. */
@RestControllerAdvice
public class ManejadorErrores extends ResponseEntityExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(CriteriosBusquedaInvalidosException.class)
    ProblemDetail criteriosInvalidos(CriteriosBusquedaInvalidosException ex) {
        return Problemas.crear(HttpStatus.BAD_REQUEST, CodigoError.CRITERIOS_INVALIDOS, ex.getMessage());
    }

    /** Las de Spring Security las resuelven su entry point y su access denied handler, no este advice. */
    @ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
    void relanzarExcepcionDeSeguridad(RuntimeException ex) {
        throw ex;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail errorInterno(Exception ex) {
        LOG.error("Error no controlado", ex);
        return Problemas.crear(HttpStatus.INTERNAL_SERVER_ERROR, CodigoError.ERROR_INTERNO, "Ocurrió un error inesperado");
    }

    /** Un parámetro con tipo o valor inválido (p. ej. categoria=Inexistente o precioMax=abc) es un criterio inválido. */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        String parametro = ex instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName()
                : ex.getPropertyName();
        ProblemDetail problema = Problemas.crear(HttpStatus.BAD_REQUEST, CodigoError.CRITERIOS_INVALIDOS,
                "El parámetro «" + parametro + "» tiene un valor inválido: " + ex.getValue());
        return handleExceptionInternal(ex, problema, headers, HttpStatus.BAD_REQUEST, request);
    }

    /** Agrega un código genérico a los errores estándar de Spring MVC (404, 405…). */
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
