package eu.talmar.api.application.port.out;

import eu.talmar.api.domain.identity.User;
import eu.talmar.api.domain.identity.valueobject.EmailAddress;
import java.util.Optional;

public interface UserRepository {
  void save(User user);

  Optional<User> findByEmail(EmailAddress email);
}
