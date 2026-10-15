package app.jobzy.api.domain.identity.valueobject;

/**
 * A user's place in their company. Derived at registration, never chosen: the first user of a
 * company becomes its {@code OWNER}, everyone after that a {@code MEMBER}.
 */
public enum Membership {
  OWNER,
  MEMBER
}
