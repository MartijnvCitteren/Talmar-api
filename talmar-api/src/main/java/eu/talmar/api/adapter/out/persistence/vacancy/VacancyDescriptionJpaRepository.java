package eu.talmar.api.adapter.out.persistence.vacancy;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Low-level repository for {@code VacancyDescriptionJpaEntity}. The shared-PK {@code @OneToOne}
 * relation (ADR 0005) makes save/find of a present description cascade through {@code
 * VacancyJpaEntity}, so {@code VacancyRepositoryAdapter} no longer uses this repository for that
 * path. It is still used directly by the adapter to delete the description row when a vacancy's
 * description is cleared, since {@code orphanRemoval} does not reliably fire when merging a
 * freshly-built transient parent graph (see {@code VacancyRepositoryAdapter}).
 */
@Repository
public interface VacancyDescriptionJpaRepository
    extends JpaRepository<VacancyDescriptionJpaEntity, UUID> {
  /**
   * Fetch the description entity for a given vacancy. Since the shared-PK relation makes the
   * vacancy id the description row's primary key, this is a one-line delegation to {@link
   * #findById}.
   *
   * @param vacancyId the vacancy id
   * @return the description entity, or empty if never set
   */
  default Optional<VacancyDescriptionJpaEntity> findByVacancyId(UUID vacancyId) {
    return findById(vacancyId);
  }
}
