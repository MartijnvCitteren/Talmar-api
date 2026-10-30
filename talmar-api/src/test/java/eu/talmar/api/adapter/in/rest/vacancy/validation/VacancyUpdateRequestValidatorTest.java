package eu.talmar.api.adapter.in.rest.vacancy.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import eu.talmar.api.adapter.in.rest.vacancy.InvalidVacancyDescriptionRequestException;
import eu.talmar.api.adapter.in.rest.vacancy.InvalidVacancyUpdateRequestException;
import eu.talmar.api.vacancy.adapter.in.web.contract.ContactPerson;
import eu.talmar.api.vacancy.adapter.in.web.contract.CurrencyCode;
import eu.talmar.api.vacancy.adapter.in.web.contract.Offer;
import eu.talmar.api.vacancy.adapter.in.web.contract.SalaryPeriod;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyDescriptionRequest;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyOfferRequest;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyUpdateRequest;
import java.math.BigDecimal;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class VacancyUpdateRequestValidatorTest {

  private final VacancyUpdateRequestValidator validator =
      new VacancyUpdateRequestValidator(new VacancyDescriptionContentValidator());

  @ParameterizedTest(name = "{0}")
  @MethodSource("validOffers")
  @DisplayName("given an offer that follows the rules, when validated then it is accepted")
  void givenOfferThatFollowsTheRulesWhenValidatedThenItIsAccepted(String description, Offer offer) {
    var request = requestWith(offer);

    assertDoesNotThrow(() -> validator.validate(request));
  }

  static Stream<Arguments> validOffers() {
    return Stream.of(
        Arguments.of(
            "full offer",
            new Offer()
                .salaryMin(new BigDecimal("3800"))
                .salaryMax(new BigDecimal("4600"))
                .currency(CurrencyCode.EUR)
                .salaryPeriod(SalaryPeriod.MONTHLY)
                .numberOfHolidays(new BigDecimal("25"))),
        Arguments.of("holidays only", new Offer().numberOfHolidays(new BigDecimal("25"))),
        Arguments.of(
            "minimum only",
            new Offer()
                .salaryMin(new BigDecimal("20.50"))
                .currency(CurrencyCode.CHF)
                .salaryPeriod(SalaryPeriod.HOURLY)),
        Arguments.of(
            "minimum equals maximum",
            new Offer()
                .salaryMin(new BigDecimal("50000"))
                .salaryMax(new BigDecimal("50000.00"))
                .currency(CurrencyCode.GBP)
                .salaryPeriod(SalaryPeriod.ANNUAL)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidOffers")
  @DisplayName(
      "given an offer that breaks a rule, when validated then it is rejected naming the field")
  void givenOfferThatBreaksRuleWhenValidatedThenRejectedNamingTheField(
      String expectedField, Offer offer) {
    var request = requestWith(offer);

    var exception =
        assertThrows(InvalidVacancyUpdateRequestException.class, () -> validator.validate(request));

    assertEquals(expectedField, exception.getField());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidOffers")
  @DisplayName(
      "given an update with an offer that breaks a rule, when validated then it is rejected naming"
          + " the field")
  void givenUpdateWithOfferThatBreaksRuleWhenValidatedThenRejectedNamingTheField(
      String expectedField, Offer offer) {
    var request = new VacancyUpdateRequest().offer(offer);

    var exception =
        assertThrows(InvalidVacancyUpdateRequestException.class, () -> validator.validate(request));

    assertEquals(expectedField, exception.getField());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("validUpdates")
  @DisplayName("given an update that follows the rules, when validated then it is accepted")
  void givenUpdateThatFollowsTheRulesWhenValidatedThenItIsAccepted(
      String description, VacancyUpdateRequest request) {
    assertDoesNotThrow(() -> validator.validate(request));
  }

  static Stream<Arguments> validUpdates() {
    return Stream.of(
        Arguments.of("empty update", new VacancyUpdateRequest()),
        Arguments.of("job title only", new VacancyUpdateRequest().jobTitle("Chef")),
        Arguments.of(
            "hours per week together",
            new VacancyUpdateRequest()
                .minHoursPerWeek(new BigDecimal("32"))
                .maxHoursPerWeek(new BigDecimal("40"))),
        Arguments.of(
            "plain description",
            new VacancyUpdateRequest()
                .description(new VacancyDescriptionRequest().summary("Line one\nLine two"))));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("updatesWithHalfTheHours")
  @DisplayName(
      "given an update with only one end of the hours per week, when validated then it is"
          + " rejected naming the missing end")
  void givenUpdateWithOneEndOfHoursWhenValidatedThenRejectedNamingTheMissingEnd(
      String expectedField, VacancyUpdateRequest request) {
    var exception =
        assertThrows(InvalidVacancyUpdateRequestException.class, () -> validator.validate(request));

    assertEquals(expectedField, exception.getField());
  }

  static Stream<Arguments> updatesWithHalfTheHours() {
    return Stream.of(
        Arguments.of(
            "maxHoursPerWeek", new VacancyUpdateRequest().minHoursPerWeek(new BigDecimal("32"))),
        Arguments.of(
            "minHoursPerWeek", new VacancyUpdateRequest().maxHoursPerWeek(new BigDecimal("40"))));
  }

  @Test
  @DisplayName(
      "given an update with markup in the description, when validated then it is rejected naming"
          + " the field by its path in the update")
  void givenUpdateWithMarkupInDescriptionWhenValidatedThenRejectedNamingTheFieldPath() {
    var request =
        new VacancyUpdateRequest()
            .description(new VacancyDescriptionRequest().tasks("<script>alert(1)</script>"));

    var exception =
        assertThrows(
            InvalidVacancyDescriptionRequestException.class, () -> validator.validate(request));

    assertEquals("description.tasks", exception.getField());
  }

  static Stream<Arguments> invalidOffers() {
    return Stream.of(
        Arguments.of("offer", new Offer()),
        Arguments.of("offer", new Offer().currency(CurrencyCode.EUR)),
        Arguments.of(
            "offer.currency",
            new Offer().salaryMin(new BigDecimal("3800")).salaryPeriod(SalaryPeriod.MONTHLY)),
        Arguments.of(
            "offer.currency",
            new Offer().salaryMax(new BigDecimal("4600")).salaryPeriod(SalaryPeriod.MONTHLY)),
        Arguments.of(
            "offer.salaryPeriod",
            new Offer().salaryMin(new BigDecimal("3800")).currency(CurrencyCode.EUR)),
        Arguments.of(
            "offer.salaryMin",
            new Offer()
                .salaryMin(new BigDecimal("4600.01"))
                .salaryMax(new BigDecimal("4600"))
                .currency(CurrencyCode.EUR)
                .salaryPeriod(SalaryPeriod.MONTHLY)));
  }

  private static VacancyOfferRequest requestWith(Offer offer) {
    return new VacancyOfferRequest(new ContactPerson("Sanne de Vries", "sanne@acme.nl"), offer);
  }
}
