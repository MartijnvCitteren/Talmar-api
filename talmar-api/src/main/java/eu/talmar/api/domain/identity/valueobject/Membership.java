package eu.talmar.api.domain.identity.valueobject;

/**
 * A user's place in their company. Derived when the user is created, never chosen: the first user
 * of a company becomes its {@code OWNER}, everyone after that a {@code MEMBER}.
 */
public enum Membership {
  OWNER,
  MEMBER
}
