package eu.talmar.api.domain.generation;

import eu.talmar.api.domain.UuidV7Generator;
import eu.talmar.api.domain.generation.valueobject.GeneratedDraft;
import eu.talmar.api.domain.generation.valueobject.GenerationFailureReason;
import eu.talmar.api.domain.generation.valueobject.GenerationInput;
import eu.talmar.api.domain.generation.valueobject.GenerationStatus;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * An AI-drafted vacancy description, from the moment a user asks for it until the draft is ready or
 * the attempt failed. A separate aggregate from {@code Vacancy} on purpose: the draft is a
 * suggestion owned by the AI, the vacancy's description is owned by the user, and the two have
 * different lifetimes. Nothing here ever changes the vacancy; the generation only references it by
 * id (see ADR 0002).
 *
 * <p>Rules this aggregate owns:
 *
 * <ul>
 *   <li>A generation starts {@code PENDING} and ends exactly once, in {@code COMPLETED} or {@code
 *       FAILED}; after that it is immutable.
 *   <li>A {@code PENDING} generation older than {@link #PENDING_TIMEOUT} has timed out. This covers
 *       a crash or restart during generation without a watchdog process: nobody will ever finish
 *       it, so a poll must not wait forever.
 *   <li>Once that deadline has passed, a late result cannot turn the generation into a success any
 *       more: a poll may already have reported the timeout, and the outcome must not flip.
 * </ul>
 *
 * <p>Input and output are validated where they enter the application (the REST adapter and the AI
 * adapter), not again here.
 */
public final class VacancyDescriptionGeneration {

  /**
   * How long a generation may stay {@code PENDING}. Well above the worst case of a healthy run
   * (three provider attempts of at most 15 seconds plus backoff, under one minute), so it only
   * fires when the work was lost.
   */
  private static final Duration PENDING_TIMEOUT = Duration.ofSeconds(90);

  private final UUID id;
  private final UUID vacancyId;
  private final GenerationInput input;
  private final LocalDateTime requestedAt;
  private GenerationStatus status;
  private GenerationFailureReason failureReason;
  private GeneratedDraft draft;
  private String model;
  private String promptVersion;
  private LocalDateTime completedAt;

  private VacancyDescriptionGeneration(Builder builder) {
    this.id = builder.id != null ? builder.id : UuidV7Generator.getUUID();
    this.vacancyId = builder.vacancyId;
    this.input = builder.input;
    this.requestedAt = builder.requestedAt;
    this.status = builder.status;
    this.failureReason = builder.failureReason;
    this.draft = builder.draft;
    this.model = builder.model;
    this.promptVersion = builder.promptVersion;
    this.completedAt = builder.completedAt;
  }

  /**
   * Creates a new {@code PENDING} generation for a vacancy. Only builds the object: the draft is
   * produced elsewhere, once the generation is saved. Creating one while another is still {@code
   * PENDING} is allowed: the user polls the newest, the old one finishes or times out on its own.
   */
  public static VacancyDescriptionGeneration newPending(
      UUID vacancyId, GenerationInput input, LocalDateTime requestedAt) {
    return builder()
        .vacancyId(vacancyId)
        .input(input)
        .requestedAt(requestedAt)
        .status(GenerationStatus.PENDING)
        .build();
  }

  /**
   * Records the draft a model produced. Ends in {@code COMPLETED}, unless the deadline has already
   * passed ({@code FAILED} / {@code TIMEOUT}).
   *
   * @throws IllegalStateException if the generation already ended
   */
  public void acceptDraft(
      GeneratedDraft draft, String model, String promptVersion, LocalDateTime completedAt) {
    requirePending();
    this.model = model;
    this.promptVersion = promptVersion;
    if (isTimedOutAt(completedAt)) {
      end(GenerationFailureReason.TIMEOUT, completedAt);
      return;
    }
    this.status = GenerationStatus.COMPLETED;
    this.draft = draft;
    this.completedAt = completedAt;
  }

  /**
   * Records that no draft could be produced. Once the deadline has passed the reason is always
   * {@code TIMEOUT}, matching what a poll has been reporting since then.
   *
   * @throws IllegalStateException if the generation already ended
   */
  public void fail(GenerationFailureReason reason, LocalDateTime failedAt) {
    requirePending();
    end(isTimedOutAt(failedAt) ? GenerationFailureReason.TIMEOUT : reason, failedAt);
  }

  /** Whether this generation is still {@code PENDING} past its deadline at the given moment. */
  public boolean isTimedOutAt(LocalDateTime now) {
    return status == GenerationStatus.PENDING && !now.isBefore(requestedAt.plus(PENDING_TIMEOUT));
  }

  private void end(GenerationFailureReason reason, LocalDateTime endedAt) {
    this.status = GenerationStatus.FAILED;
    this.failureReason = reason;
    this.completedAt = endedAt;
  }

  private void requirePending() {
    if (status != GenerationStatus.PENDING) {
      throw new IllegalStateException("Generation " + id + " already ended as " + status);
    }
  }

  public UUID getId() {
    return id;
  }

  public UUID getVacancyId() {
    return vacancyId;
  }

  public GenerationInput getInput() {
    return input;
  }

  public LocalDateTime getRequestedAt() {
    return requestedAt;
  }

  public GenerationStatus getStatus() {
    return status;
  }

  /** Only present when {@code FAILED}. */
  public GenerationFailureReason getFailureReason() {
    return failureReason;
  }

  /** Only present when {@code COMPLETED}. */
  public GeneratedDraft getDraft() {
    return draft;
  }

  /** The model version that produced the draft, as reported by the provider. */
  public String getModel() {
    return model;
  }

  /** The prompt template version that produced the draft. */
  public String getPromptVersion() {
    return promptVersion;
  }

  /** When the generation ended, in either final state. */
  public LocalDateTime getCompletedAt() {
    return completedAt;
  }

  /** Builder for reconstituting a stored generation; new generations use {@link #newPending}. */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof VacancyDescriptionGeneration other)) {
      return false;
    }
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public String toString() {
    return "VacancyDescriptionGeneration{"
        + "id="
        + id
        + ", vacancyId="
        + vacancyId
        + ", status="
        + status
        + ", failureReason="
        + failureReason
        + '}';
  }

  public static class Builder {
    private UUID id;
    private UUID vacancyId;
    private GenerationInput input;
    private LocalDateTime requestedAt;
    private GenerationStatus status;
    private GenerationFailureReason failureReason;
    private GeneratedDraft draft;
    private String model;
    private String promptVersion;
    private LocalDateTime completedAt;

    private Builder() {}

    public Builder id(UUID id) {
      this.id = id;
      return this;
    }

    public Builder vacancyId(UUID vacancyId) {
      this.vacancyId = vacancyId;
      return this;
    }

    public Builder input(GenerationInput input) {
      this.input = input;
      return this;
    }

    public Builder requestedAt(LocalDateTime requestedAt) {
      this.requestedAt = requestedAt;
      return this;
    }

    public Builder status(GenerationStatus status) {
      this.status = status;
      return this;
    }

    public Builder failureReason(GenerationFailureReason failureReason) {
      this.failureReason = failureReason;
      return this;
    }

    public Builder draft(GeneratedDraft draft) {
      this.draft = draft;
      return this;
    }

    public Builder model(String model) {
      this.model = model;
      return this;
    }

    public Builder promptVersion(String promptVersion) {
      this.promptVersion = promptVersion;
      return this;
    }

    public Builder completedAt(LocalDateTime completedAt) {
      this.completedAt = completedAt;
      return this;
    }

    public VacancyDescriptionGeneration build() {
      return new VacancyDescriptionGeneration(this);
    }
  }
}
