package eu.talmar.api.adapter.out.keycloak;

import java.util.List;

/**
 * Keycloak's {@code OrganizationRepresentation}, with the fields we write and read.
 *
 * @param id Keycloak's id; {@code null} when creating
 */
record KeycloakOrganization(
    String id, String name, String alias, boolean enabled, List<Domain> domains) {

  /** A domain of the organization; {@code verified} is left to Keycloak's default. */
  record Domain(String name) {}

  /**
   * The organization of a company: name and domain are the e-mail domain, because the derived
   * company name is not unique (ADR 0006, section 4), and the alias is the company id.
   */
  static KeycloakOrganization of(String emailDomain, String companyId) {
    return new KeycloakOrganization(
        null, emailDomain, companyId, true, List.of(new Domain(emailDomain)));
  }

  boolean hasDomain(String emailDomain) {
    return domains != null
        && domains.stream().anyMatch(domain -> emailDomain.equals(domain.name()));
  }
}
