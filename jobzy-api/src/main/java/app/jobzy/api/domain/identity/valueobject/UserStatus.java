package app.jobzy.api.domain.identity.valueobject;

/**
 * Lifecycle of a user account: {@code PENDING_VERIFICATION} until the e-mail address is confirmed,
 * then {@code ACTIVE} for a company owner or {@code PENDING_APPROVAL} for a member, who becomes
 * {@code ACTIVE} once the owner approves them.
 */
public enum UserStatus {
  PENDING_VERIFICATION,
  PENDING_APPROVAL,
  ACTIVE
}
