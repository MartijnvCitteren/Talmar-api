package eu.talmar.api.adapter.out.keycloak;

/** The fields we read from Keycloak's {@code UserRepresentation}; the rest is ignored. */
record KeycloakUser(
    String id, String email, Boolean emailVerified, String firstName, String lastName) {

  /** Redacted: names and e-mail address are personal data and must not end up in logs. */
  @Override
  public String toString() {
    return "KeycloakUser[id=" + id + "]";
  }
}
