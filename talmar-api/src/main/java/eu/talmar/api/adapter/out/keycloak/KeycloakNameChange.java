package eu.talmar.api.adapter.out.keycloak;

/** The part of Keycloak's {@code UserRepresentation} a name change sends. */
record KeycloakNameChange(String firstName, String lastName) {

  /** Redacted: names are personal data and must not end up in logs. */
  @Override
  public String toString() {
    return "KeycloakNameChange[redacted]";
  }
}
