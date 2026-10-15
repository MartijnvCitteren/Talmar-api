package app.jobzy.api.application.port.out;

import app.jobzy.api.domain.identity.User;
import app.jobzy.api.domain.identity.valueobject.EmailAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
  void save(User user);

  Optional<User> findByEmail(EmailAddress email);

  /**
   * Finds the user a verification token was issued to.
   *
   * @param tokenHash the SHA-256 hash of a verification token, as stored
   */
  Optional<User> findByVerificationTokenHash(String tokenHash);

  /**
   * Users still awaiting verification whose verification token expired at or before {@code moment}.
   */
  List<User> findAwaitingVerificationExpiredAt(LocalDateTime moment);

  long countByCompanyId(UUID companyId);

  void delete(User user);
}
