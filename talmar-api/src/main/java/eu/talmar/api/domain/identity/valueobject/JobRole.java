package eu.talmar.api.domain.identity.valueobject;

/**
 * The role a user picks when they sign up. A profile field, not a right: what a user may do follows
 * from {@link Membership}, never from this.
 */
public enum JobRole {
  RECRUITER,
  HIRING_MANAGER,
  HR,
  FOUNDER_OR_DIRECTOR,
  OTHER
}
