package app.jobzy.api.adapter.out.persistence.identity;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Credentials are saved and deleted through the cascade from {@code UserJpaEntity}; this repository
 * exists for direct reads, e.g. in tests that check what was stored.
 */
@Repository
public interface UserCredentialsJpaRepository
    extends JpaRepository<UserCredentialsJpaEntity, UUID> {}
