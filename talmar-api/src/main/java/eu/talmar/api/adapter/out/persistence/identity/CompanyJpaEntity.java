package eu.talmar.api.adapter.out.persistence.identity;

import eu.talmar.api.adapter.out.persistence.BaseJpaEntity;
import eu.talmar.api.shared.gdpr.ProcessData;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity for companies. The e-mail domain is unique: it is how a registrant is matched to their
 * company, so two companies on one domain would make that match ambiguous.
 *
 * <p>{@code last_modified_by} is required here: every change to a company is traced to the {@code
 * app_user} who made it. It holds the user id without a foreign key, so deleting a user (the purge
 * of unconfirmed registrations) never fails on, or rewrites, the audit trail of a company.
 */
@Getter
@Setter
@Entity
@Table(name = "company")
@AttributeOverride(name = "lastModifiedBy", column = @Column(nullable = false))
public class CompanyJpaEntity extends BaseJpaEntity {
  @Id private UUID id;

  @ProcessData
  @Column(nullable = false)
  private String name;

  @ProcessData
  @Column(nullable = false, unique = true)
  private String emailDomain;
}
