package app.jobzy.api.adapter.out.persistence.identity;

import app.jobzy.api.domain.identity.valueobject.UserStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {
  Optional<UserJpaEntity> findByEmail(String email);

  Optional<UserJpaEntity> findByVerificationTokenHash(String verificationTokenHash);

  List<UserJpaEntity> findByStatusAndVerificationTokenExpiresAtLessThanEqual(
      UserStatus status, LocalDateTime moment);

  long countByCompanyId(UUID companyId);
}
