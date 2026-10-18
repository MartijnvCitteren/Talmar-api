package eu.talmar.api.adapter.out.persistence.vacancy.mapper;

import eu.talmar.api.adapter.out.persistence.vacancy.VacancyJpaEntity;
import eu.talmar.api.domain.vacancy.Vacancy;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

/** Maps between {@code Vacancy} domain entity and {@code VacancyJpaEntity}. */
@Mapper(
    componentModel = "spring",
    injectionStrategy = InjectionStrategy.CONSTRUCTOR,
    uses = {VacancyDescriptionJpaMapper.class},
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface VacancyJpaMapper {
  /**
   * Map a domain entity to a JPA entity. The cascade relation to description (if present) is wired
   * by the adapter before saving.
   *
   * @param vacancy the domain entity
   * @return the JPA entity
   */
  VacancyJpaEntity toJpaEntity(Vacancy vacancy);

  /**
   * Map a JPA entity to a domain entity, including the description if present (fetched EAGER).
   *
   * @param vacancyEntity the persisted entity
   * @return the domain entity
   */
  Vacancy toDomain(VacancyJpaEntity vacancyEntity);
}
