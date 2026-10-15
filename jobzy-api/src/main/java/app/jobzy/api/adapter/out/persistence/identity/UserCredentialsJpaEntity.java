package app.jobzy.api.adapter.out.persistence.identity;

import app.jobzy.api.adapter.out.persistence.BaseJpaEntity;
import app.jobzy.api.shared.gdpr.PersonalData;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity for a user's password credentials, in its own table so that an external identity (SSO)
 * can be added later without changing the user row. Shares its primary key with the user via
 * {@code @MapsId}, so a user has at most one row here; it only exists once the e-mail address is
 * confirmed.
 */
@Getter
@Setter
@Entity
@Table(name = "user_credentials")
public class UserCredentialsJpaEntity extends BaseJpaEntity {
  @Id private UUID id;

  @OneToOne
  @MapsId
  @JoinColumn(name = "user_id")
  private UserJpaEntity user;

  @PersonalData
  @Column(nullable = false)
  private String passwordHash;
}
