package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence;

import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence.entity.UsuarioEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, UUID> {

    /**
     * Bloquea la fila del usuario (SELECT ... FOR UPDATE) para que dos intentos simultáneos no pierdan un
     * incremento de intentos_fallidos. Sin join fetch: el bloqueo no debe alcanzar la fila de rol, que comparten
     * todos los usuarios del mismo rol.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from UsuarioEntity u where u.correo = :correo")
    Optional<UsuarioEntity> buscarPorCorreoParaActualizar(@Param("correo") String correo);
}
