package co.edu.unimagdalena.samaria360.catalog.application.port.out;

import co.edu.unimagdalena.samaria360.catalog.domain.model.AtractivoPublico;

import java.util.Optional;

/**
 * Lectura pública de la colección {@code atractivos}: el detalle de WF-06 para atractivos (DEC-217). Un atractivo es
 * visible mientras exista (DEC-87). La gestión del Gestor está en {@link GestionAtractivosPort}: el caso de uso público
 * depende solo de este contrato y no puede escribir (segregación, como en servicios). El id ya está validado.
 */
public interface AtractivoRepositoryPort {

    Optional<AtractivoPublico> buscar(String id);
}
