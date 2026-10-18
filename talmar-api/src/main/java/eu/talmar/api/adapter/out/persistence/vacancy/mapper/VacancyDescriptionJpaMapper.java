package eu.talmar.api.adapter.out.persistence.vacancy.mapper;

import eu.talmar.api.adapter.out.persistence.vacancy.VacancyDescriptionJpaEntity;
import eu.talmar.api.domain.vacancy.valueobject.VacancyDescription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/** Maps between {@code VacancyDescription} domain VO and {@code VacancyDescriptionJpaEntity}. */
@Mapper(
    componentModel = "spring",
    unmappedSourcePolicy = ReportingPolicy.IGNORE,
    unmappedTargetPolicy = ReportingPolicy.WARN)
public interface VacancyDescriptionJpaMapper {

  /**
   * Map a JPA entity to domain value object.
   *
   * @param entity the persisted entity
   * @return the domain VO
   */
  VacancyDescription toDomain(VacancyDescriptionJpaEntity entity);

  /**
   * Map a domain VO to a JPA entity, mapping the five text fields and {@code source} only. The
   * {@code id}/{@code vacancy} relation is wired by the adapter, and {@code createdAt}/{@code
   * lastModifiedAt} are populated by JPA auditing.
   *
   * @param description the domain VO
   * @return the entity ready to be wired onto its parent vacancy
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "vacancy", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "lastModifiedAt", ignore = true)
  @Mapping(target = "lastModifiedBy", ignore = true)
  VacancyDescriptionJpaEntity toJpaEntity(VacancyDescription description);
}
