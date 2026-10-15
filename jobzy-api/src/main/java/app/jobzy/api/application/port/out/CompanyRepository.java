package app.jobzy.api.application.port.out;

import app.jobzy.api.domain.identity.Company;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository {
  void save(Company company);

  /**
   * Finds the company an e-mail domain belongs to.
   *
   * @param emailDomain the e-mail domain in lower case
   */
  Optional<Company> findByEmailDomain(String emailDomain);

  void deleteById(UUID id);
}
