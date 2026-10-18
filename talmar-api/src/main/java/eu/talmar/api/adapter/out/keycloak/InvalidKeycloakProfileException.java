package eu.talmar.api.adapter.out.keycloak;

import eu.talmar.api.shared.exception.BaseException;

/**
 * Thrown when a profile from Keycloak breaks the rules of Talmar's own columns. The realm enforces
 * the same rules, so this means the realm configuration changed; the message names the field, never
 * its value.
 */
class InvalidKeycloakProfileException extends BaseException {

  InvalidKeycloakProfileException(String field) {
    super("Keycloak profile has an invalid " + field);
  }
}
