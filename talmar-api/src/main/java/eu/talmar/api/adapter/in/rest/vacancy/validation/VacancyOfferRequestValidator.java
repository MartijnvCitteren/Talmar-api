package eu.talmar.api.adapter.in.rest.vacancy.validation;

import eu.talmar.api.adapter.in.rest.vacancy.InvalidVacancyOfferRequestException;
import eu.talmar.api.vacancy.adapter.in.web.contract.Offer;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyOfferRequest;
import org.springframework.stereotype.Component;

/**
 * Checks the offer rules the contract promises but the schema cannot express. Bean validation has
 * already checked each field on its own; these rules combine fields.
 *
 * <p>A salary without a currency and period cannot be read in a multi-country market (4000 what,
 * per what?), and an offer without a salary or holidays offers nothing.
 */
@Component
public class VacancyOfferRequestValidator {
  private static final String OFFER = "offer";
  private static final String CURRENCY = "offer.currency";
  private static final String SALARY_PERIOD = "offer.salaryPeriod";
  private static final String SALARY_MIN = "offer.salaryMin";

  /**
   * Validates the offer of the request against the combined-field rules.
   *
   * @param request the request to validate
   * @throws InvalidVacancyOfferRequestException naming the first field that breaks a rule
   */
  public void validate(VacancyOfferRequest request) {
    var offer = request.getOffer();
    requireSalaryOrHolidays(offer);
    requireCurrencyAndPeriodWithSalary(offer);
    requireMinimumNotAboveMaximum(offer);
  }

  private void requireSalaryOrHolidays(Offer offer) {
    if (!hasSalary(offer) && offer.getNumberOfHolidays() == null) {
      throw new InvalidVacancyOfferRequestException(
          OFFER, "offer must contain salaryMin, salaryMax or numberOfHolidays");
    }
  }

  private void requireCurrencyAndPeriodWithSalary(Offer offer) {
    if (!hasSalary(offer)) {
      return;
    }
    if (offer.getCurrency() == null) {
      throw new InvalidVacancyOfferRequestException(
          CURRENCY, "currency is required when a salary is given");
    }
    if (offer.getSalaryPeriod() == null) {
      throw new InvalidVacancyOfferRequestException(
          SALARY_PERIOD, "salaryPeriod is required when a salary is given");
    }
  }

  private void requireMinimumNotAboveMaximum(Offer offer) {
    var salaryMin = offer.getSalaryMin();
    var salaryMax = offer.getSalaryMax();
    if (salaryMin != null && salaryMax != null && salaryMin.compareTo(salaryMax) > 0) {
      throw new InvalidVacancyOfferRequestException(
          SALARY_MIN, "salaryMin must not be greater than salaryMax");
    }
  }

  private boolean hasSalary(Offer offer) {
    return offer.getSalaryMin() != null || offer.getSalaryMax() != null;
  }
}
