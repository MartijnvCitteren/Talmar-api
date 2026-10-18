package eu.talmar.api.adapter.out.persistence.identity;

import eu.talmar.api.adapter.out.persistence.BaseJpaEntity;
import eu.talmar.api.domain.identity.valueobject.JobRole;
import eu.talmar.api.domain.identity.valueobject.Membership;
import eu.talmar.api.domain.identity.valueobject.UserStatus;
import eu.talmar.api.shared.gdpr.PersonalData;
import eu.talmar.api.shared.gdpr.ProcessData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

/**
 * JPA entity for users, in table {@code app_user} because {@code user} is a reserved word in SQL
 * Server. A company has many users: {@code company_id} is a foreign key to {@code company}, mapped
 * as a lazy {@code @ManyToOne} so the database guards the relation. The domain still references the
 * company by id only, because company and user are separate aggregates.
 *
 * <p>The names are {@code nvarchar}: Keycloak accepts names in any script ({@code Łukasz}, {@code
 * 李}), and SQL Server's default {@code varchar} collation would silently replace what Latin-1
 * cannot hold. H2 in the tests is Unicode either way, so only this annotation guards it.
 *
 * <p>{@code keycloak_subject} links the row to the Keycloak account (ADR 0006): a random id that
 * identifies nobody without Keycloak, so process data.
 */
@Getter
@Setter
@Entity
@Table(
    name = "app_user",
    indexes = {@Index(name = "ix_app_user_company_id", columnList = "company_id")})
public class UserJpaEntity extends BaseJpaEntity {
  @Id private UUID id;

  @ProcessData
  @Column(name = "keycloak_subject", nullable = false, unique = true, length = 36)
  private String subject;

  @PersonalData
  @Nationalized
  @Column(nullable = false)
  private String firstName;

  @PersonalData
  @Nationalized
  @Column(nullable = false)
  private String lastName;

  @PersonalData
  @Column(nullable = false, unique = true)
  private String email;

  @ProcessData
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private JobRole jobRole;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(
      name = "company_id",
      nullable = false,
      foreignKey = @ForeignKey(name = "fk_app_user_company"))
  private CompanyJpaEntity company;

  @ProcessData
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Membership membership;

  @ProcessData
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private UserStatus status;

  @ProcessData
  @Column(nullable = false)
  private boolean organizationMember;
}
