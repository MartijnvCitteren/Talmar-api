package eu.talmar.api.adapter.in.rest.vacancy.validation;

import eu.talmar.api.adapter.in.rest.vacancy.InvalidVacancyUpdateRequestException;
import eu.talmar.api.vacancy.adapter.in.web.contract.Offer;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyOfferRequest;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Checks the rules for changing a vacancy that the contract promises but the schema cannot express.
 * Bean validation has already checked each field on its own; these rules combine fields.
 *
 * <p>A salary without a currency and period cannot be read in a multi-country market (4000 what,
 * per what?), and an offer without a salary or holidays offers nothing. Hours per week are one
 * range, so a minimum without its maximum cannot be applied to the vacancy.
 */
@Component
@RequiredArgsConstructor
public class VacancyUpdateRequestValidator {
  private static final String OFFER = "offer";
  private static final String CURRENCY = "offer.currency";
  private static final String SALARY_PERIOD = "offer.salaryPeriod";
  private static final String SALARY_MIN = "offer.salaryMin";
  private static final String MIN_HOURS_PER_WEEK = "minHoursPerWeek";
  private static final String MAX_HOURS_PER_WEEK = "maxHoursPerWeek";
  private static final String DESCRIPTION_PREFIX = "description.";

  private final VacancyDescriptionContentValidator descriptionContentValidator;

  /**
   * Validates the sections of a partial update that were sent.
   *
   * @param request the request to validate
   * @throws InvalidVacancyUpdateRequestException naming the first field that breaks a rule
   * @throws eu.talmar.api.adapter.in.rest.vacancy.InvalidVacancyDescriptionRequestException if the
   *     description contains markup or control characters
   */
  public void validate(VacancyUpdateRequest request) {
    requireHoursPerWeekTogether(request);
    var description = request.getDescription();
    if (description != null) {
      descriptionContentValidator.validate(description, DESCRIPTION_PREFIX);
    }
    var offer = request.getOffer();
    if (offer != null) {
      validateOffer(offer);
    }
  }

  /**
   * Validates the offer of the request against the combined-field rules.
   *
   * @param request the request to validate
   * @throws InvalidVacancyUpdateRequestException naming the first field that breaks a rule
   */
  public void validate(VacancyOfferRequest request) {
    validateOffer(request.getOffer());
  }

  private void requireHoursPerWeekTogether(VacancyUpdateRequest request) {
    if (request.getMinHoursPerWeek() != null && request.getMaxHoursPerWeek() == null) {
      throw new InvalidVacancyUpdateRequestException(
          MAX_HOURS_PER_WEEK, "maxHoursPerWeek is required when minHoursPerWeek is given");
    }
    if (request.getMaxHoursPerWeek() != null && request.getMinHoursPerWeek() == null) {
      throw new InvalidVacancyUpdateRequestException(
          MIN_HOURS_PER_WEEK, "minHoursPerWeek is required when maxHoursPerWeek is given");
    }
  }

  private void validateOffer(Offer offer) {
    requireSalaryOrHolidays(offer);
    requireCurrencyAndPeriodWithSalary(offer);
    requireMinimumNotAboveMaximum(offer);
  }

  private void requireSalaryOrHolidays(Offer offer) {
    if (!hasSalary(offer) && offer.getNumberOfHolidays() == null) {
      throw new InvalidVacancyUpdateRequestException(
          OFFER, "offer must contain salaryMin, salaryMax or numberOfHolidays");
    }
  }

  private void requireCurrencyAndPeriodWithSalary(Offer offer) {
    if (!hasSalary(offer)) {
      return;
    }
    if (offer.getCurrency() == null) {
      throw new InvalidVacancyUpdateRequestException(
          CURRENCY, "currency is required when a salary is given");
    }
    if (offer.getSalaryPeriod() == null) {
      throw new InvalidVacancyUpdateRequestException(
          SALARY_PERIOD, "salaryPeriod is required when a salary is given");
    }
  }

  private void requireMinimumNotAboveMaximum(Offer offer) {
    var salaryMin = offer.getSalaryMin();
    var salaryMax = offer.getSalaryMax();
    if (salaryMin != null && salaryMax != null && salaryMin.compareTo(salaryMax) > 0) {
      throw new InvalidVacancyUpdateRequestException(
          SALARY_MIN, "salaryMin must not be greater than salaryMax");
    }
  }

  private boolean hasSalary(Offer offer) {
    return offer.getSalaryMin() != null || offer.getSalaryMax() != null;
  }
}
