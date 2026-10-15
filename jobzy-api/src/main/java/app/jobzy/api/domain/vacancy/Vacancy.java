package app.jobzy.api.domain.vacancy;

import app.jobzy.api.domain.BaseObject;
import app.jobzy.api.domain.UuidV7Generator;
import app.jobzy.api.domain.vacancy.valueobject.HoursPerWeek;
import app.jobzy.api.domain.vacancy.valueobject.Language;
import app.jobzy.api.domain.vacancy.valueobject.Location;
import app.jobzy.api.domain.vacancy.valueobject.VacancyCategory;
import app.jobzy.api.domain.vacancy.valueobject.VacancyDescription;
import app.jobzy.api.domain.vacancy.valueobject.VacancyStatus;
import app.jobzy.api.domain.vacancy.valueobject.WorkplaceType;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Vacancy extends BaseObject {

  private final UUID id;
  private String jobTitle;
  private Language language;
  private VacancyCategory category;
  private Location location;
  private WorkplaceType workplaceType;
  private HoursPerWeek hoursPerWeek;
  private VacancyStatus status;
  private VacancyDescription description;

  private Vacancy(
      UUID id,
      String jobTitle,
      Language language,
      VacancyCategory category,
      Location location,
      WorkplaceType workplaceType,
      HoursPerWeek hoursPerWeek,
      VacancyStatus status,
      VacancyDescription description,
      LocalDateTime createdAt) {
    super(createdAt);
    this.id = id;
    this.jobTitle = jobTitle;
    this.language = language;
    this.category = category;
    this.location = location;
    this.workplaceType = workplaceType;
    this.hoursPerWeek = hoursPerWeek;
    this.status = status;
    this.description = description;
  }

  public UUID getId() {
    return id;
  }

  public String getJobTitle() {
    return jobTitle;
  }

  public void setJobTitle(String jobTitle) {
    this.jobTitle = jobTitle;
  }

  public Language getLanguage() {
    return language;
  }

  public void setLanguage(Language language) {
    this.language = language;
  }

  public VacancyCategory getCategory() {
    return category;
  }

  public void setCategory(VacancyCategory category) {
    this.category = category;
  }

  public Location getLocation() {
    return location;
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public WorkplaceType getWorkplaceType() {
    return workplaceType;
  }

  public void setWorkplaceType(WorkplaceType workplaceType) {
    this.workplaceType = workplaceType;
  }

  public HoursPerWeek getHoursPerWeek() {
    return hoursPerWeek;
  }

  public void setHoursPerWeek(HoursPerWeek hoursPerWeek) {
    this.hoursPerWeek = hoursPerWeek;
  }

  public VacancyStatus getStatus() {
    return status;
  }

  public void setStatus(VacancyStatus status) {
    this.status = status;
  }

  public VacancyDescription getDescription() {
    return description;
  }

  public void setDescription(VacancyDescription description) {
    this.description = description;
  }

  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof Vacancy vacancy)) {
      return false;
    }
    return Objects.equals(getId(), vacancy.getId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getId());
  }

  @Override
  public String toString() {
    return "Vacancy{"
        + "id="
        + id
        + ", jobTitle='"
        + jobTitle
        + '\''
        + ", language="
        + language
        + ", category="
        + category
        + ", location="
        + location
        + ", workplaceType="
        + workplaceType
        + ", hoursPerWeek="
        + hoursPerWeek
        + ", status="
        + status
        + '}';
  }

  public static class Builder {
    private UUID explicitId;
    private String jobTitle;
    private Language language;
    private VacancyCategory category;
    private Location location;
    private WorkplaceType workplaceType;
    private HoursPerWeek hoursPerWeek;
    private VacancyStatus status = VacancyStatus.DRAFT;
    private VacancyDescription description;
    private LocalDateTime createdAt;

    private Builder() {}

    public Builder id(UUID id) {
      this.explicitId = id;
      return this;
    }

    public Builder jobTitle(String jobTitle) {
      this.jobTitle = jobTitle;
      return this;
    }

    public Builder language(Language language) {
      this.language = language;
      return this;
    }

    public Builder category(VacancyCategory category) {
      this.category = category;
      return this;
    }

    public Builder location(Location location) {
      this.location = location;
      return this;
    }

    public Builder workplaceType(WorkplaceType workplaceType) {
      this.workplaceType = workplaceType;
      return this;
    }

    public Builder hoursPerWeek(HoursPerWeek hoursPerWeek) {
      this.hoursPerWeek = hoursPerWeek;
      return this;
    }

    public Builder status(VacancyStatus status) {
      this.status = status;
      return this;
    }

    public Builder description(VacancyDescription description) {
      this.description = description;
      return this;
    }

    public Builder createdAt(LocalDateTime createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Vacancy build() {
      UUID id = explicitId != null ? explicitId : UuidV7Generator.getUUID();
      return new Vacancy(
          id,
          Objects.requireNonNull(jobTitle, "jobTitle is required"),
          Objects.requireNonNull(language, "language is required"),
          Objects.requireNonNull(category, "category is required"),
          Objects.requireNonNull(location, "location is required"),
          Objects.requireNonNull(workplaceType, "workplaceType is required"),
          Objects.requireNonNull(hoursPerWeek, "hoursPerWeek is required"),
          status,
          description,
          Objects.requireNonNull(createdAt, "createdAt is required"));
    }
  }
}
