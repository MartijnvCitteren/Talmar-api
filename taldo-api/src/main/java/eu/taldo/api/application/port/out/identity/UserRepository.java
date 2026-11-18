package eu.taldo.api.application.port.out.identity;

import eu.taldo.api.domain.identity.User;
import eu.taldo.api.domain.identity.valueobject.EmailAddress;
import java.util.Optional;

public interface UserRepository {
  void save(User user);

  Optional<User> findByEmail(EmailAddress email);

  /**
   * Finds the user linked to an identity-provider account.
   *
   * @param subject the account's id at the identity provider
   */
  Optional<User> findBySubject(String subject);
}
