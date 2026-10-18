package eu.talmar.api.application.port.out;

import java.util.UUID;

/**
 * The identity provider that owns accounts, credentials and login (ADR 0006). Implementations wrap
 * one provider and are the only place that knows it.
 *
 * <p>Talmar and the provider cannot share a transaction, so every operation is safe to repeat: the
 * caller finishes an interrupted onboarding by running it again (ADR 0006, section 7). Every
 * operation throws {@link IdentityProviderUnavailableException} when the provider cannot be reached
 * or answers in a way we did not expect; nothing is retried inside the call.
 */
public interface IdentityProvider {

  /**
   * The profile of an account as the provider stores it.
   *
   * @param subject the account's id at the provider, the {@code sub} claim of its access token
   */
  IdentityProfile getProfile(String subject);

  /**
   * Returns the provider's organization of a company, and creates it first when it does not exist
   * yet. Its name and its only domain are the company's e-mail domain, its alias the company id.
   *
   * @return the provider's id of the organization
   */
  String findOrCreateOrganization(String emailDomain, UUID companyId);

  /**
   * Makes an account a member of an organization. An account that is a member already counts as
   * success.
   */
  void addOrganizationMember(String organizationId, String subject);
}
