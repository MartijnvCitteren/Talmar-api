package eu.talmar.api.adapter.out.persistence;

import eu.talmar.api.shared.gdpr.ProcessData;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * Shared audit-column base for all JPA entities: {@code createdAt}, {@code lastModifiedAt} and
 * {@code lastModifiedBy}. {@code createdAt} and {@code lastModifiedAt} are populated by Spring Data
 * JPA auditing (see {@code JpaAuditingConfig}), not by adapter code. {@code lastModifiedBy} is the
 * {@code app_user} id the domain object carries; there is no auditor-aware provider, because nobody
 * is authenticated yet. It is an id, so process data: the audit trail survives anonymization.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseJpaEntity {
  @ProcessData @CreatedDate private LocalDateTime createdAt;
  @ProcessData @LastModifiedDate private LocalDateTime lastModifiedAt;
  @ProcessData private UUID lastModifiedBy;
}
