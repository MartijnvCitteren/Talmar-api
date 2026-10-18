package eu.talmar.api.domain.identity;

import static eu.talmar.api.testSupport.IdentityFactory.NOW;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.talmar.api.domain.identity.valueobject.EmailAddress;
import eu.talmar.api.domain.identity.valueobject.Membership;
import eu.talmar.api.domain.identity.valueobject.UserStatus;
import eu.talmar.api.testSupport.IdentityFactory;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserTest {
  private static final UUID COMPANY_ID = UUID.randomUUID();

  @Test
  @DisplayName("given a new company, when a user is created then they become its active owner")
  void givenNewCompanyWhenUserIsCreatedThenTheyBecomeItsActiveOwner() {
    var user = IdentityFactory.owner(COMPANY_ID);

    assertEquals(Membership.OWNER, user.getMembership());
    assertEquals(UserStatus.ACTIVE, user.getStatus());
    assertEquals(COMPANY_ID, user.getCompanyId());
  }

  @Test
  @DisplayName(
      "given an existing company, when a user is created then they become a member pending"
          + " approval")
  void givenExistingCompanyWhenUserIsCreatedThenTheyBecomeMemberPendingApproval() {
    var user = IdentityFactory.member(COMPANY_ID);

    assertEquals(Membership.MEMBER, user.getMembership());
    assertEquals(UserStatus.PENDING_APPROVAL, user.getStatus());
  }

  @Test
  @DisplayName("given an id generated up front, when a user is created then the user has that id")
  void givenIdGeneratedUpFrontWhenUserIsCreatedThenTheUserHasThatId() {
    var id = User.newId();
    var email = new EmailAddress(IdentityFactory.EMAIL);

    var user =
        User.create(
            id,
            IdentityFactory.SUBJECT,
            IdentityFactory.FIRST_NAME,
            IdentityFactory.LAST_NAME,
            email,
            IdentityFactory.JOB_ROLE,
            COMPANY_ID,
            false,
            NOW);

    assertEquals(id, user.getId());
  }

  @Test
  @DisplayName(
      "given a new owner, when marked as organization member then no membership is needed anymore")
  void givenNewOwnerWhenMarkedAsOrganizationMemberThenNoMembershipIsNeededAnymore() {
    var owner = IdentityFactory.owner(COMPANY_ID);

    owner.markOrganizationMember(NOW.plusMinutes(1));

    assertTrue(owner.isOrganizationMember());
    assertFalse(owner.needsOrganizationMembership());
    assertEquals(NOW.plusMinutes(1), owner.getLastModifiedAt());
  }

  @Test
  @DisplayName("given a user, when the name is changed then both names and the change time are set")
  void givenUserWhenNameIsChangedThenBothNamesAndChangeTimeAreSet() {
    var user = IdentityFactory.owner(COMPANY_ID);

    user.changeName("José", "'t Hooft", NOW.plusDays(1));

    assertEquals("José", user.getFirstName());
    assertEquals("'t Hooft", user.getLastName());
    assertEquals(NOW.plusDays(1), user.getLastModifiedAt());
  }

  @Test
  @DisplayName("given a new owner, when asked then they still need the organization membership")
  void givenNewOwnerWhenAskedThenTheyStillNeedTheOrganizationMembership() {
    assertTrue(IdentityFactory.owner(COMPANY_ID).needsOrganizationMembership());
  }

  @Test
  @DisplayName("given a new member, when asked then they need no organization membership yet")
  void givenNewMemberWhenAskedThenTheyNeedNoOrganizationMembershipYet() {
    assertFalse(IdentityFactory.member(COMPANY_ID).needsOrganizationMembership());
  }

  @Test
  @DisplayName("given a builder without a company id, when built then throws naming the field")
  void givenBuilderWithoutCompanyIdWhenBuiltThenThrowsNamingTheField() {
    var builder =
        User.builder()
            .subject(IdentityFactory.SUBJECT)
            .firstName(IdentityFactory.FIRST_NAME)
            .lastName(IdentityFactory.LAST_NAME)
            .email(new EmailAddress(IdentityFactory.EMAIL))
            .jobRole(IdentityFactory.JOB_ROLE);

    var exception = assertThrows(NullPointerException.class, builder::build);

    assertEquals("companyId is required", exception.getMessage());
  }

  @Test
  @DisplayName("given a user, when toString then no personal data is in the output")
  void givenUserWhenToStringThenNoPersonalDataIsInTheOutput() {
    var output = IdentityFactory.owner(COMPANY_ID).toString();

    assertFalse(output.contains(IdentityFactory.FIRST_NAME));
    assertFalse(output.contains(IdentityFactory.EMAIL_DOMAIN));
  }

  @Test
  @DisplayName("given two users, when compared then equality follows the id")
  void givenTwoUsersWhenComparedThenEqualityFollowsTheId() {
    var user = IdentityFactory.owner(COMPANY_ID);

    assertEquals(user, user);
    assertEquals(user.getId().hashCode(), user.hashCode());
    assertNotEquals(user, IdentityFactory.owner(COMPANY_ID));
  }
}
