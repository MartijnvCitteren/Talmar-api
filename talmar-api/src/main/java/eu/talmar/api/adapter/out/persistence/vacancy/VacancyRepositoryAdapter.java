package eu.talmar.api.adapter.out.persistence.vacancy;

import eu.talmar.api.adapter.out.persistence.vacancy.mapper.VacancyDescriptionJpaMapper;
import eu.talmar.api.adapter.out.persistence.vacancy.mapper.VacancyJpaMapper;
import eu.talmar.api.application.port.out.VacancyRepository;
import eu.talmar.api.domain.vacancy.Vacancy;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

/**
 * Persistence adapter for {@code VacancyRepository}. Orchestrates saves and reads via the cascaded
 * {@code @OneToOne} relation between {@code VacancyJpaEntity} and {@code
 * VacancyDescriptionJpaEntity}, wiring the relation before persisting. Note: {@code orphanRemoval}
 * does not fire when merging a freshly-built transient parent graph, so explicit {@code deleteById}
 * is required to clear a description row (see ADR 0005 addendum).
 */
@Component
@RequiredArgsConstructor
@Log4j2
public class VacancyRepositoryAdapter implements VacancyRepository {
  private final VacancyJpaMapper jpaMapper;
  private final VacancyDescriptionJpaMapper descriptionJpaMapper;
  private final VacancyJpaRepository jpaRepository;
  private final VacancyDescriptionJpaRepository descriptionJpaRepository;

  /**
   * Save a vacancy and its description (if present) via the cascaded {@code @OneToOne} relation in
   * a single {@code save()} call. If description is being removed (set to null), an explicit {@code
   * deleteById} is issued afterward, since {@code orphanRemoval} does not fire on a freshly-built
   * transient graph.
   */
  @Override
  public void save(Vacancy vacancy) {
    var jpaVacancy = jpaMapper.toJpaEntity(vacancy);

    if (vacancy.getDescription() != null) {
      var descriptionEntity = descriptionJpaMapper.toJpaEntity(vacancy.getDescription());
      // Set the @MapsId-derived id explicitly: without it, Hibernate's merge cascade cannot tell
      // this is an update of an existing row and always attempts an INSERT, which fails with a
      // duplicate-key error on the second save for the same vacancy.
      descriptionEntity.setId(jpaVacancy.getId());
      descriptionEntity.setVacancy(jpaVacancy);
      jpaVacancy.setDescription(descriptionEntity);
      jpaRepository.save(jpaVacancy);
    } else {
      // orphanRemoval does not fire from merging a freshly-built transient parent graph (verified
      // empirically: no DELETE is ever emitted, even after an explicit flush), since Hibernate has
      // no prior loaded snapshot of the association to diff against. Deleting the row explicitly
      // is required; deleteById is a no-op if no description exists yet.
      jpaVacancy.setDescription(null);
      jpaRepository.save(jpaVacancy);
      descriptionJpaRepository.deleteById(jpaVacancy.getId());
    }

    log.info("Vacancy is saved");
  }

  /**
   * Load a vacancy by id with its description (if present) in a single call, since description is
   * fetched EAGER.
   *
   * @param id the vacancy id
   * @return the vacancy, or empty if not found
   */
  @Override
  public Optional<Vacancy> findById(UUID id) {
    return jpaRepository.findById(id).map(jpaMapper::toDomain);
  }
}
