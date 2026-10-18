package eu.talmar.api.adapter.out.persistence.generation;

import eu.talmar.api.adapter.out.persistence.BaseJpaEntity;
import eu.talmar.api.domain.generation.valueobject.GenerationFailureReason;
import eu.talmar.api.domain.generation.valueobject.GenerationStatus;
import eu.talmar.api.domain.vacancy.valueobject.Language;
import eu.talmar.api.shared.gdpr.ProcessData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity for {@code VacancyDescriptionGeneration}, flattened into one row.
 *
 * <p>{@code vacancyId} is a plain column, not an association: the generation only references the
 * vacancy and never loads it. Every column is process data: the answers and the draft describe a
 * job and a team, not a person, and the job title and language are copies of vacancy fields. The
 * index on {@code requestedAt} serves the retention clean-up.
 */
@Getter
@Setter
@Entity
@Table(
    name = "vacancy_description_generation",
    indexes = @Index(name = "ix_generation_requested_at", columnList = "requestedAt"))
public class VacancyDescriptionGenerationJpaEntity extends BaseJpaEntity {
  @Id private UUID id;

  @ProcessData
  @Column(nullable = false)
  private UUID vacancyId;

  @ProcessData
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private GenerationStatus status;

  @ProcessData
  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private GenerationFailureReason failureReason;

  @ProcessData
  @Column(nullable = false, length = 200)
  private String jobTitle;

  @ProcessData
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 2)
  private Language language;

  @ProcessData
  @Column(nullable = false, length = 1000)
  private String mostImportantTasks;

  @ProcessData
  @Column(nullable = false, length = 1000)
  private String team;

  @ProcessData
  @Column(nullable = false, length = 1000)
  private String whyNiceJob;

  @ProcessData
  @Column(length = 1000)
  private String draftSummary;

  @ProcessData
  @Column(length = 5000)
  private String draftJobDescription;

  @ProcessData
  @Column(length = 5000)
  private String draftTasks;

  @ProcessData
  @Column(length = 100)
  private String model;

  @ProcessData
  @Column(length = 100)
  private String promptVersion;

  @ProcessData
  @Column(nullable = false)
  private LocalDateTime requestedAt;

  @ProcessData private LocalDateTime completedAt;
}
