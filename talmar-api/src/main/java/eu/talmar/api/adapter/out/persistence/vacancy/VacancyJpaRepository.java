package eu.talmar.api.adapter.out.persistence.vacancy;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VacancyJpaRepository extends JpaRepository<VacancyJpaEntity, UUID> {}
