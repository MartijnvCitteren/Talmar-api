package app.jobzy.api.adapter.out.persistence.vacancy;

import app.jobzy.api.adapter.out.persistence.BaseJpaEntity;
import app.jobzy.api.domain.vacancy.valueobject.HoursPerWeek;
import app.jobzy.api.domain.vacancy.valueobject.Language;
import app.jobzy.api.domain.vacancy.valueobject.Location;
import app.jobzy.api.domain.vacancy.valueobject.VacancyCategory;
import app.jobzy.api.domain.vacancy.valueobject.VacancyStatus;
import app.jobzy.api.domain.vacancy.valueobject.WorkplaceType;
import app.jobzy.api.shared.gdpr.ProcessData;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity for vacancies. Holds the inverse (non-owning) side of the {@code @OneToOne} relation
 * to {@code VacancyDescriptionJpaEntity}, with cascade and orphanRemoval enabled so saving or
 * clearing the description cascades through this entity. Description is fetched EAGER (see ADR
 * 0005) since it is always loaded as part of the single aggregate.
 */
@Getter
@Setter
@Entity
public class VacancyJpaEntity extends BaseJpaEntity {
  @Id private UUID id;
  @ProcessData private String jobTitle;

  @ProcessData
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 2)
  private Language language;

  @OneToOne(
      mappedBy = "vacancy",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  private VacancyDescriptionJpaEntity description;

  @ProcessData
  @Enumerated(EnumType.STRING)
  private VacancyCategory category;

  @ProcessData @Embedded private Location location;

  @ProcessData
  @Enumerated(EnumType.STRING)
  private WorkplaceType workplaceType;

  @ProcessData @Embedded private HoursPerWeek hoursPerWeek;

  @ProcessData
  @Enumerated(EnumType.STRING)
  private VacancyStatus status;
}
