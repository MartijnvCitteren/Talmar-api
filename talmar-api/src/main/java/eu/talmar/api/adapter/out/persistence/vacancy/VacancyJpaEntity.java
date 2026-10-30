package eu.talmar.api.adapter.out.persistence.vacancy;

import eu.talmar.api.adapter.out.persistence.BaseJpaEntity;
import eu.talmar.api.domain.vacancy.valueobject.HoursPerWeek;
import eu.talmar.api.domain.vacancy.valueobject.Language;
import eu.talmar.api.domain.vacancy.valueobject.Location;
import eu.talmar.api.domain.vacancy.valueobject.VacancyCategory;
import eu.talmar.api.domain.vacancy.valueobject.VacancyStatus;
import eu.talmar.api.domain.vacancy.valueobject.WorkplaceType;
import eu.talmar.api.shared.gdpr.PersonalData;
import eu.talmar.api.shared.gdpr.ProcessData;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Version;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Nationalized;

/**
 * JPA entity for vacancies. Holds the inverse (non-owning) side of the {@code @OneToOne} relation
 * to {@code VacancyDescriptionJpaEntity}, with cascade and orphanRemoval enabled so saving or
 * clearing the description cascades through this entity. Description is fetched EAGER (see ADR
 * 0005) since it is always loaded as part of the single aggregate.
 *
 * <p>The contact person is four flat columns rather than an embeddable: its name, phone number and
 * e-mail address are personal data and its role is not, and anonymization needs that split per
 * column. Name and role are {@code nvarchar}, like the user's names, so names in any script survive
 * SQL Server's Latin-1 {@code varchar} collation.
 */
@Getter
@Setter
@Entity
public class VacancyJpaEntity extends BaseJpaEntity {
  @Id private UUID id;

  /**
   * Optimistic lock. Not null with a default of 0, so Hibernate's schema update can add the column
   * to a table that already has rows. A null version in a mapped entity means "not saved yet" to
   * Spring Data, so a new vacancy is inserted and a loaded one is updated with a version check.
   */
  @Version
  @ColumnDefault("0")
  @Column(nullable = false)
  private Long version;

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

  @PersonalData
  @Nationalized
  @Column(length = 35)
  private String contactPersonName;

  @ProcessData
  @Nationalized
  @Column(length = 50)
  private String contactPersonRole;

  @PersonalData
  @Column(length = 16)
  private String contactPersonPhone;

  @PersonalData
  @Column(length = 75)
  private String contactPersonEmail;

  @ProcessData @Embedded private OfferJpaEmbeddable offer;
}
