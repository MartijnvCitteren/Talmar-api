package eu.talmar.api.domain.identity.valueobject;

/**
 * Lifecycle of a user account: {@code ACTIVE} for a company owner, {@code PENDING_APPROVAL} for a
 * member, who becomes {@code ACTIVE} once the owner approves them.
 */
public enum UserStatus {
  PENDING_APPROVAL,
  ACTIVE
}
