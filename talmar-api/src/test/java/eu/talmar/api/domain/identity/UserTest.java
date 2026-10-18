package eu.talmar.api.domain.identity;

import static eu.talmar.api.testSupport.IdentityFactory.NOW;
import static eu.talmar.api.testSupport.IdentityFactory.TOKEN_EXPIRES_AT;
import static eu.talmar.api.testSupport.IdentityFactory.TOKEN_HASH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.talmar.api.domain.identity.valueobject.Membership;
import eu.talmar.api.domain.identity.valueobject.UserCredentials;
import eu.talmar.api.domain.identity.valueobject.UserStatus;
import eu.talmar.api.domain.identity.valueobject.VerificationToken;
import eu.talmar.api.testSupport.IdentityFactory;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {
  private static final UUID COMPANY_ID = UUID.randomUUID();
  private static final UserCredentials CREDENTIALS = new UserCredentials("hash");

  @Test
  @DisplayName(
      "given a new company, when a user registers then they become its owner, pending"
          + " verification")
  void givenNewCompanyWhenUserRegistersThenTheyBecomeOwnerPendingVerification() {
    var user = IdentityFactory.pendingOwner(COMPANY_ID);

    assertEquals(Membership.OWNER, user.getMembership());
    assertEquals(UserStatus.PENDING_VERIFICATION, user.getStatus());
    assertEquals(COMPANY_ID, user.getCompanyId());
    assertEquals(IdentityFactory.validToken(), user.getVerificationToken());
    assertNull(user.getCredentials());
    assertTrue(user.isAwaitingVerification());
  }

  @Test
  @DisplayName("given an id generated up front, when a user registers then the user has that id")
  void givenIdGeneratedUpFrontWhenUserRegistersThenTheUserHasThatId() {
    var id = User.newId();

    var user =
        User.register(
            id, IdentityFactory.registrant(), COMPANY_ID, false, IdentityFactory.validToken(), NOW);

    assertEquals(id, user.getId());
  }

  @Test
  @DisplayName(
      "given an existing company, when a user registers then they become a member, pending"
          + " verification")
  void givenExistingCompanyWhenUserRegistersThenTheyBecomeMemberPendingVerification() {
    var user = IdentityFactory.pendingMember(COMPANY_ID);

    assertEquals(Membership.MEMBER, user.getMembership());
    assertEquals(UserStatus.PENDING_VERIFICATION, user.getStatus());
  }

  @Test
  @DisplayName(
      "given a pending owner, when confirmed with a valid token then active with credentials and"
          + " the token cleared")
  void givenPendingOwnerWhenConfirmedWithValidTokenThenActiveWithCredentialsAndTokenCleared() {
    var user = IdentityFactory.pendingOwner(COMPANY_ID);

    user.confirm(TOKEN_HASH, CREDENTIALS, NOW.plusHours(1));

    assertEquals(UserStatus.ACTIVE, user.getStatus());
    assertEquals(CREDENTIALS, user.getCredentials());
    assertNull(user.getVerificationToken());
    assertEquals(NOW.plusHours(1), user.getLastModifiedAt());
  }

  @Test
  @DisplayName("given a pending member, when confirmed with a valid token then pending approval")
  void givenPendingMemberWhenConfirmedWithValidTokenThenPendingApproval() {
    var user = IdentityFactory.pendingMember(COMPANY_ID);

    user.confirm(TOKEN_HASH, CREDENTIALS, NOW.plusHours(1));

    assertEquals(UserStatus.PENDING_APPROVAL, user.getStatus());
  }

  @Test
  @DisplayName("given a pending user, when confirmed with another token then it is rejected")
  void givenPendingUserWhenConfirmedWithAnotherTokenThenItIsRejected() {
    var user = IdentityFactory.pendingOwner(COMPANY_ID);
    var otherHash = VerificationToken.hashOf("other");

    assertThrows(
        InvalidVerificationTokenException.class, () -> user.confirm(otherHash, CREDENTIALS, NOW));
    assertEquals(UserStatus.PENDING_VERIFICATION, user.getStatus());
  }

  @Test
  @DisplayName("given a pending user, when confirmed at the token expiry then it is rejected")
  void givenPendingUserWhenConfirmedAtTokenExpiryThenItIsRejected() {
    var user = IdentityFactory.pendingOwner(COMPANY_ID);

    assertThrows(
        InvalidVerificationTokenException.class,
        () -> user.confirm(TOKEN_HASH, CREDENTIALS, TOKEN_EXPIRES_AT));
  }

  @Test
  @DisplayName("given a confirmed user, when confirmed again with the same token then rejected")
  void givenConfirmedUserWhenConfirmedAgainWithSameTokenThenRejected() {
    var user = IdentityFactory.pendingOwner(COMPANY_ID);
    user.confirm(TOKEN_HASH, CREDENTIALS, NOW);

    assertFalse(user.canBeConfirmedWith(TOKEN_HASH, NOW));
    assertThrows(
        InvalidVerificationTokenException.class,
        () -> user.confirm(TOKEN_HASH, new UserCredentials("other"), NOW));
    assertEquals(CREDENTIALS, user.getCredentials());
  }

  @Test
  @DisplayName(
      "given a pending user, when the token is renewed then only the new token confirms the user")
  void givenPendingUserWhenTokenIsRenewedThenOnlyTheNewTokenConfirmsTheUser() {
    var user = IdentityFactory.pendingOwner(COMPANY_ID);
    var newHash = VerificationToken.hashOf("new");

    user.renewVerificationToken(new VerificationToken(newHash, TOKEN_EXPIRES_AT), NOW);

    assertFalse(user.canBeConfirmedWith(TOKEN_HASH, NOW));
    assertTrue(user.canBeConfirmedWith(newHash, NOW));
  }

  @Test
  @DisplayName("given an active user, when the token is renewed then it is refused")
  void givenActiveUserWhenTokenIsRenewedThenItIsRefused() {
    var user = IdentityFactory.activeOwner(COMPANY_ID);
    var token = IdentityFactory.validToken();

    assertThrows(IllegalStateException.class, () -> user.renewVerificationToken(token, NOW));
  }

  @Test
  @DisplayName("given a builder without a company id, when built then throws naming the field")
  void givenBuilderWithoutCompanyIdWhenBuiltThenThrowsNamingTheField() {
    var builder = User.builder().firstName("Jane").lastName("Doe");
    var registrant = IdentityFactory.registrant();
    builder.email(registrant.email()).jobRole(registrant.jobRole());

    var exception = assertThrows(NullPointerException.class, builder::build);

    assertEquals("companyId is required", exception.getMessage());
  }

  @Test
  @DisplayName("given a user, when toString then no personal data is in the output")
  void givenUserWhenToStringThenNoPersonalDataIsInTheOutput() {
    var output = IdentityFactory.pendingOwner(COMPANY_ID).toString();

    assertFalse(output.contains(IdentityFactory.FIRST_NAME));
    assertFalse(output.contains(IdentityFactory.EMAIL_DOMAIN));
  }

  @Test
  @DisplayName("given two users, when compared then equality follows the id")
  void givenTwoUsersWhenComparedThenEqualityFollowsTheId() {
    var user = IdentityFactory.pendingOwner(COMPANY_ID);

    assertEquals(user, user);
    assertEquals(user.getId().hashCode(), user.hashCode());
    assertNotEquals(user, IdentityFactory.pendingOwner(COMPANY_ID));
  }
}
