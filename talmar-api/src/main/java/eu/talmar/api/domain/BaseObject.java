package eu.talmar.api.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public abstract class BaseObject {
  private final LocalDateTime createdAt;
  private LocalDateTime lastModifiedAt;
  private UUID lastModifiedBy;

  protected BaseObject(LocalDateTime createdAt) {
    this.createdAt = createdAt;
    this.lastModifiedAt = createdAt;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getLastModifiedAt() {
    return lastModifiedAt;
  }

  public void setLastModifiedAt(LocalDateTime lastModifiedAt) {
    this.lastModifiedAt = lastModifiedAt;
  }

  /**
   * The id of the user who last created or changed this object, or {@code null} when that is not
   * tracked yet. An id, never a name, so the audit trail holds no personal data.
   */
  public UUID getLastModifiedBy() {
    return lastModifiedBy;
  }

  public void setLastModifiedBy(UUID lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }
}
