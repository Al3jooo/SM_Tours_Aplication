package co.edu.unimagdalena.samaria360.catalog.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Validaciones comunes de las invariantes del modelo. */
final class Validaciones {

    private Validaciones() {
    }

    static String texto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(campo + " es obligatorio");
        }
        return valor;
    }

    static String textoMaximo(String valor, int maximo, String campo) {
        texto(valor, campo);
        if (valor.length() > maximo) {
            throw new IllegalArgumentException(campo + " admite como máximo " + maximo + " caracteres");
        }
        return valor;
    }

    static int positivo(int valor, String campo) {
        if (valor <= 0) {
            throw new IllegalArgumentException(campo + " debe ser mayor que 0");
        }
        return valor;
    }

    static int noNegativo(int valor, String campo) {
        if (valor < 0) {
            throw new IllegalArgumentException(campo + " no puede ser negativo");
        }
        return valor;
    }

    static BigDecimal positivo(BigDecimal valor, String campo) {
        Objects.requireNonNull(valor, campo);
        if (valor.signum() <= 0) {
            throw new IllegalArgumentException(campo + " debe ser mayor que 0");
        }
        return valor;
    }

    static BigDecimal noNegativoOpcional(BigDecimal valor, String campo) {
        if (valor != null && valor.signum() < 0) {
            throw new IllegalArgumentException(campo + " no puede ser negativo");
        }
        return valor;
    }

    static List<String> lista(List<String> valores) {
        return valores == null ? List.of() : List.copyOf(valores);
    }
}
