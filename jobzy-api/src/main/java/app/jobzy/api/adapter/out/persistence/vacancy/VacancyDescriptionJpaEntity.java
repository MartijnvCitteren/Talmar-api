package app.jobzy.api.adapter.out.persistence.vacancy;

import app.jobzy.api.adapter.out.persistence.BaseJpaEntity;
import app.jobzy.api.domain.vacancy.valueobject.VacancyDescriptionSource;
import app.jobzy.api.shared.gdpr.ProcessData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity for vacancy descriptions. Shares its primary key with the owning {@code
 * VacancyJpaEntity} via {@code @MapsId}: the description row's id is always its vacancy's id, so a
 * second row for the same vacancy is structurally impossible. See ADR 0005 for the rationale.
 */
@Getter
@Setter
@Entity
public class VacancyDescriptionJpaEntity extends BaseJpaEntity {
  @Id private UUID id;

  @OneToOne
  @MapsId
  @JoinColumn(name = "vacancy_id")
  private VacancyJpaEntity vacancy;

  // Lengths match the contract's VacancyDescriptionRequest; without them every text would be
  // capped at the 255-character default.
  @ProcessData
  @Column(length = 1000)
  private String summary;

  @ProcessData
  @Column(length = 5000)
  private String jobDescription;

  @ProcessData
  @Column(length = 5000)
  private String tasks;

  @ProcessData
  @Column(length = 2500)
  private String whatWeOffer;

  @ProcessData
  @Column(length = 2500)
  private String aboutUs;

  @ProcessData
  @Enumerated(EnumType.STRING)
  private VacancyDescriptionSource source;
}
