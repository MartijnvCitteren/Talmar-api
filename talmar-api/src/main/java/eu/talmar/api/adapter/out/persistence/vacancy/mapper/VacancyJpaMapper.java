package eu.talmar.api.adapter.out.persistence.vacancy.mapper;

import eu.talmar.api.adapter.out.persistence.vacancy.VacancyJpaEntity;
import eu.talmar.api.domain.identity.valueobject.EmailAddress;
import eu.talmar.api.domain.vacancy.Vacancy;
import eu.talmar.api.domain.vacancy.valueobject.ContactPerson;
import eu.talmar.api.domain.vacancy.valueobject.PhoneNumber;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * Maps between {@code Vacancy} domain entity and {@code VacancyJpaEntity}. The contact person is
 * flattened into four columns of the entity (see {@code VacancyJpaEntity} for why).
 */
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
  @Mapping(target = "contactPersonName", source = "contactPerson.name")
  @Mapping(target = "contactPersonRole", source = "contactPerson.role")
  @Mapping(target = "contactPersonPhone", source = "contactPerson.phone.value")
  @Mapping(target = "contactPersonEmail", source = "contactPerson.email.value")
  VacancyJpaEntity toJpaEntity(Vacancy vacancy);

  /**
   * Map a JPA entity to a domain entity, including the description if present (fetched EAGER).
   *
   * @param vacancyEntity the persisted entity
   * @return the domain entity
   */
  @Mapping(target = "contactPerson", source = ".")
  Vacancy toDomain(VacancyJpaEntity vacancyEntity);

  /**
   * Rebuild the contact person from its columns. The name is required for a contact person, so a
   * row without one has no contact person.
   *
   * @param vacancyEntity the persisted entity
   * @return the contact person, or {@code null} when the vacancy has none
   */
  default ContactPerson toContactPerson(VacancyJpaEntity vacancyEntity) {
    if (vacancyEntity.getContactPersonName() == null) {
      return null;
    }
    var phone = vacancyEntity.getContactPersonPhone();
    return new ContactPerson(
        vacancyEntity.getContactPersonName(),
        vacancyEntity.getContactPersonRole(),
        phone == null ? null : new PhoneNumber(phone),
        new EmailAddress(vacancyEntity.getContactPersonEmail()));
  }
}
