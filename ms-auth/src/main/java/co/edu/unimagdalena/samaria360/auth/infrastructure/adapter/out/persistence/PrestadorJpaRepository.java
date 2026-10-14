package co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence;

import co.edu.unimagdalena.samaria360.auth.infrastructure.adapter.out.persistence.entity.PrestadorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PrestadorJpaRepository extends JpaRepository<PrestadorEntity, UUID> {

    boolean existsByRnt(String rnt);
}
