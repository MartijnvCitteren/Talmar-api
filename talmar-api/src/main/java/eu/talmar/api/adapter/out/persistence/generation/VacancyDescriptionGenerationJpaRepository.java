package eu.talmar.api.adapter.out.persistence.generation;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VacancyDescriptionGenerationJpaRepository
    extends JpaRepository<VacancyDescriptionGenerationJpaEntity, UUID> {

  /**
   * Bulk delete in one statement instead of loading every expired row first; the entity has no
   * associations or lifecycle callbacks that a bulk delete would skip.
   */
  @Modifying
  @Query(
      "delete from VacancyDescriptionGenerationJpaEntity generation"
          + " where generation.requestedAt < :cutoff")
  int deleteByRequestedAtBefore(@Param("cutoff") LocalDateTime cutoff);
}
