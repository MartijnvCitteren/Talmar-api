package eu.talmar.api.integration.identity;

import static eu.talmar.api.integration.IntegrationCommons.JSON_MAPPER;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import eu.talmar.api.adapter.out.persistence.identity.CompanyJpaRepository;
import eu.talmar.api.adapter.out.persistence.identity.UserCredentialsJpaRepository;
import eu.talmar.api.adapter.out.persistence.identity.UserJpaEntity;
import eu.talmar.api.adapter.out.persistence.identity.UserJpaRepository;
import eu.talmar.api.application.port.in.PurgeUnconfirmedRegistrationsUseCase;
import eu.talmar.api.application.port.out.RegistrationMailer;
import eu.talmar.api.domain.identity.User;
import eu.talmar.api.domain.identity.valueobject.Membership;
import eu.talmar.api.domain.identity.valueobject.UserStatus;
import eu.talmar.api.identity.adapter.in.web.contract.AccountStatus;
import eu.talmar.api.identity.adapter.in.web.contract.RegistrationConfirmationResponse;
import eu.talmar.api.integration.BaseIntegrationTest;
import eu.talmar.api.vacancy.adapter.in.web.contract.ProblemDetails;
import io.restassured.http.ContentType;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Runs registration and confirmation end to end against an in-memory H2 database, through the
 * security filter chain, the REST adapter, the use cases and persistence. The mailer is replaced by
 * a mock so the test can read the raw token that would be in the confirmation link.
 */
class RegistrationIntegrationTest extends BaseIntegrationTest {
  private static final String PROBLEM_JSON = "application/problem+json";
  private static final String PASSWORD = "correct horse battery staple";

  @MockitoBean private RegistrationMailer registrationMailer;

  @Autowired private UserJpaRepository userJpaRepository;
  @Autowired private UserCredentialsJpaRepository credentialsJpaRepository;
  @Autowired private CompanyJpaRepository companyJpaRepository;
  @Autowired private PurgeUnconfirmedRegistrationsUseCase purgeUseCase;

  @BeforeEach
  void setUp() {
    credentialsJpaRepository.deleteAll();
    userJpaRepository.deleteAll();
    companyJpaRepository.deleteAll();
  }

  @Test
  @DisplayName(
      "given a new work domain, when registering then 202 through the security filter chain"
          + " without a CSRF token, the company is founded and the user is a pending owner")
  void givenNewWorkDomainWhenRegisteringThen202AndCompanyFoundedAndUserPendingOwner() {
    given()
        .contentType(ContentType.JSON)
        .body(registration("Jane.Doe@Acme.nl"))
        .when()
        .post("/registration")
        .then()
        .statusCode(HttpStatus.ACCEPTED.value())
        .header("X-Content-Type-Options", "nosniff");

    var company = companyJpaRepository.findByEmailDomain("acme.nl").orElseThrow();
    assertEquals("Acme", company.getName());
    var user = userJpaRepository.findByEmail("jane.doe@acme.nl").orElseThrow();
    assertEquals(company.getId(), user.getCompany().getId());
    assertEquals(user.getId(), company.getLastModifiedBy());
    assertEquals(Membership.OWNER, user.getMembership());
    assertEquals(UserStatus.PENDING_VERIFICATION, user.getStatus());
    assertNotEquals(capturedRawToken(), user.getVerificationTokenHash());
  }

  @Test
  @DisplayName(
      "given a registration, when confirmed with the mailed token then the owner is active, the"
          + " password is stored as Argon2id and the token works only once")
  void givenRegistrationWhenConfirmedWithMailedTokenThenOwnerActiveAndTokenSingleUse() {
    register("jane.doe@acme.nl");
    var token = capturedRawToken();

    var response = confirm(token, PASSWORD, HttpStatus.OK);

    assertEquals(
        AccountStatus.ACTIVE,
        JSON_MAPPER.readValue(response, RegistrationConfirmationResponse.class).getStatus());
    var user = userJpaRepository.findByEmail("jane.doe@acme.nl").orElseThrow();
    assertEquals(UserStatus.ACTIVE, user.getStatus());
    assertNull(user.getVerificationTokenHash());
    var credentials = credentialsJpaRepository.findById(user.getId()).orElseThrow();
    assertTrue(credentials.getPasswordHash().startsWith("$argon2id$"));

    assertProblemOnField(confirm(token, PASSWORD, HttpStatus.BAD_REQUEST), "token");
  }

  @Test
  @DisplayName(
      "given a company exists for the domain, when a colleague registers and confirms then they"
          + " are a member pending approval")
  void givenCompanyExistsWhenColleagueRegistersAndConfirmsThenMemberPendingApproval() {
    register("jane.doe@acme.nl");
    register("john.smith@acme.nl");
    var colleagueToken = capturedRawTokens().getLast();

    var response = confirm(colleagueToken, PASSWORD, HttpStatus.OK);

    assertEquals(
        AccountStatus.PENDING_APPROVAL,
        JSON_MAPPER.readValue(response, RegistrationConfirmationResponse.class).getStatus());
    assertEquals(1, companyJpaRepository.count());
    var colleague = userJpaRepository.findByEmail("john.smith@acme.nl").orElseThrow();
    assertEquals(Membership.MEMBER, colleague.getMembership());
  }

  @Test
  @DisplayName(
      "given an address on a public mail domain, when registering then 400 on email and nothing"
          + " is stored or sent")
  void givenAddressOnPublicMailDomainWhenRegisteringThen400OnEmailAndNothingStored() {
    var response = postRegistration(registration("jane@gmail.com"), HttpStatus.BAD_REQUEST);

    assertProblemOnField(response, "email");
    assertEquals(0, userJpaRepository.count());
    assertEquals(0, companyJpaRepository.count());
    verify(registrationMailer, never()).sendVerificationMail(any(), anyString());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "not-an-address",
        "jane@acme",
        "@acme.nl",
        "jane@@acme.nl",
        "jane@doe@acme.nl",
        "jane doe@acme.nl",
        " jane@acme.nl",
        "jane@.acme.nl",
        "jane@acme.nl.",
        "jane@acme..nl"
      })
  @DisplayName(
      "given a malformed address, when registering then 400 on email before anything is stored")
  void givenMalformedAddressWhenRegisteringThen400OnEmail(String email) {
    assertProblemOnField(postRegistration(registration(email), HttpStatus.BAD_REQUEST), "email");
    assertEquals(0, companyJpaRepository.count());
  }

  @Test
  @DisplayName("given an address of 76 characters, when registering then 400 on email")
  void givenAddressOf76CharactersWhenRegisteringThen400OnEmail() {
    var email = "j".repeat(76 - "@acme.nl".length()) + "@acme.nl";

    assertProblemOnField(postRegistration(registration(email), HttpStatus.BAD_REQUEST), "email");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "José",
        "Anne-Marie",
        "'t Hooft",
        "O’Brien",
        "St. John",
        "Zoë",
        "Łukasz",
        "Nguyễn",
        "李",
        "محمد"
      })
  @DisplayName(
      "given a name with letters of any script and name punctuation, when registering then 202")
  void givenNameWithLettersOfAnyScriptAndNamePunctuationWhenRegisteringThen202(String name) {
    postRegistration(registration(name, name, "jane.doe@acme.nl"), HttpStatus.ACCEPTED);

    var user = userJpaRepository.findByEmail("jane.doe@acme.nl").orElseThrow();
    assertEquals(name, user.getFirstName());
    assertEquals(name, user.getLastName());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "   ",
        "-",
        "'",
        " Jane",
        "Jane ",
        "Jane  Doe",
        "Jane1",
        "Jane_Doe",
        "J@ne",
        "Jane#",
        "Jane$",
        "Jane%",
        "Jane&",
        "Jane*",
        "Jane(",
        "Jane[",
        "Jane{",
        "Jane!",
        "Jane/",
        "Jane|",
        "Jane\\",
        "Jane§",
        "Jane±",
        "Jane~",
        "Jane^",
        "abcdefghijklmnopqrstuvwxyzabcdefghij"
      })
  @DisplayName(
      "given a name with symbols, digits, stray spaces or more than 35 characters, when"
          + " registering then 400 on that name")
  void givenInvalidNameWhenRegisteringThen400OnThatName(String name) {
    assertProblemOnField(
        postRegistration(registration(name, "Doe", "jane.doe@acme.nl"), HttpStatus.BAD_REQUEST),
        "firstName");
    assertProblemOnField(
        postRegistration(registration("Jane", name, "jane.doe@acme.nl"), HttpStatus.BAD_REQUEST),
        "lastName");
  }

  @Test
  @DisplayName(
      "given a confirmed account, when registering again with that address then still 202, an"
          + " account-exists mail and nothing changes")
  void givenConfirmedAccountWhenRegisteringAgainThen202AndAccountExistsMail() {
    register("jane.doe@acme.nl");
    confirm(capturedRawToken(), PASSWORD, HttpStatus.OK);

    register("jane.doe@acme.nl");

    verify(registrationMailer).sendAccountExistsMail(any(User.class));
    assertEquals(1, userJpaRepository.count());
    assertEquals(
        UserStatus.ACTIVE,
        userJpaRepository.findByEmail("jane.doe@acme.nl").orElseThrow().getStatus());
  }

  @Test
  @DisplayName(
      "given an unconfirmed registration, when registering again then a new link is mailed and"
          + " only the new link works")
  void givenUnconfirmedRegistrationWhenRegisteringAgainThenOnlyTheNewLinkWorks() {
    register("jane.doe@acme.nl");
    register("jane.doe@acme.nl");
    var tokens = capturedRawTokens();

    assertProblemOnField(confirm(tokens.getFirst(), PASSWORD, HttpStatus.BAD_REQUEST), "token");
    confirm(tokens.getLast(), PASSWORD, HttpStatus.OK);
    assertEquals(1, userJpaRepository.count());
  }

  @Test
  @DisplayName("given an expired token, when confirming then 400 on token")
  void givenExpiredTokenWhenConfirmingThen400OnToken() {
    register("jane.doe@acme.nl");
    expireVerificationToken("jane.doe@acme.nl");

    assertProblemOnField(confirm(capturedRawToken(), PASSWORD, HttpStatus.BAD_REQUEST), "token");
  }

  @Test
  @DisplayName("given an unknown token, when confirming then 400 on token")
  void givenUnknownTokenWhenConfirmingThen400OnToken() {
    assertProblemOnField(confirm("unknown-token", PASSWORD, HttpStatus.BAD_REQUEST), "token");
  }

  @Test
  @DisplayName(
      "given a password shorter than 12 characters, when confirming then 400 on password and the"
          + " user stays unconfirmed")
  void givenTooShortPasswordWhenConfirmingThen400OnPassword() {
    register("jane.doe@acme.nl");

    assertProblemOnField(
        confirm(capturedRawToken(), "too-short", HttpStatus.BAD_REQUEST), "password");
    assertEquals(
        UserStatus.PENDING_VERIFICATION,
        userJpaRepository.findByEmail("jane.doe@acme.nl").orElseThrow().getStatus());
  }

  @Test
  @DisplayName(
      "given an expired unconfirmed registration, when the purge runs then the user and the"
          + " company it founded are deleted")
  void givenExpiredUnconfirmedRegistrationWhenPurgeRunsThenUserAndCompanyAreDeleted() {
    register("jane.doe@acme.nl");
    register("pending@other.nl");
    expireVerificationToken("jane.doe@acme.nl");

    var purged = purgeUseCase.purgeUnconfirmedRegistrations();

    assertEquals(1, purged);
    assertTrue(userJpaRepository.findByEmail("jane.doe@acme.nl").isEmpty());
    assertTrue(companyJpaRepository.findByEmailDomain("acme.nl").isEmpty());
    assertTrue(userJpaRepository.findByEmail("pending@other.nl").isPresent());
  }

  private void register(String email) {
    postRegistration(registration(email), HttpStatus.ACCEPTED);
  }

  private String postRegistration(String body, HttpStatus expectedStatus) {
    return given()
        .contentType(ContentType.JSON)
        .body(body)
        .when()
        .post("/registration")
        .then()
        .statusCode(expectedStatus.value())
        .extract()
        .asString();
  }

  private String confirm(String token, String password, HttpStatus expectedStatus) {
    return given()
        .contentType(ContentType.JSON)
        .body("{\"token\": \"%s\", \"password\": \"%s\"}".formatted(token, password))
        .when()
        .post("/registration/confirm")
        .then()
        .statusCode(expectedStatus.value())
        .contentType(expectedStatus.isError() ? PROBLEM_JSON : ContentType.JSON.toString())
        .extract()
        .asString();
  }

  private void expireVerificationToken(String email) {
    UserJpaEntity user = userJpaRepository.findByEmail(email).orElseThrow();
    user.setVerificationTokenExpiresAt(LocalDateTime.of(2000, 1, 1, 0, 0));
    userJpaRepository.save(user);
  }

  private String capturedRawToken() {
    return capturedRawTokens().getLast();
  }

  private List<String> capturedRawTokens() {
    var rawTokens = ArgumentCaptor.forClass(String.class);
    verify(registrationMailer, times(countVerificationMails()))
        .sendVerificationMail(any(User.class), rawTokens.capture());
    return rawTokens.getAllValues();
  }

  private int countVerificationMails() {
    return (int)
        mockingDetails(registrationMailer).getInvocations().stream()
            .filter(invocation -> invocation.getMethod().getName().equals("sendVerificationMail"))
            .count();
  }

  private static void assertProblemOnField(String response, String field) {
    var problem = JSON_MAPPER.readValue(response, ProblemDetails.class);
    assertEquals(HttpStatus.BAD_REQUEST.value(), problem.getStatus());
    assertEquals(field, problem.getErrors().getFirst().getField());
  }

  private static String registration(String email) {
    return registration("Jane", "Doe", email);
  }

  private static String registration(String firstName, String lastName, String email) {
    var body = new LinkedHashMap<String, String>();
    body.put("firstName", firstName);
    body.put("lastName", lastName);
    body.put("email", email);
    body.put("jobRole", "RECRUITER");
    return JSON_MAPPER.writeValueAsString(body);
  }
}
