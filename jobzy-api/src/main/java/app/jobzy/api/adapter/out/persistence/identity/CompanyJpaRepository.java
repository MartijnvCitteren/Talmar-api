package app.jobzy.api.adapter.out.persistence.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyJpaRepository extends JpaRepository<CompanyJpaEntity, UUID> {
  Optional<CompanyJpaEntity> findByEmailDomain(String emailDomain);
}
