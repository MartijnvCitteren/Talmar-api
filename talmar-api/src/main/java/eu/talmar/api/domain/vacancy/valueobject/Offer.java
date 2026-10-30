package eu.talmar.api.domain.vacancy.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;

/**
 * What the employer offers: a gross salary range in {@code currency} per {@code salaryPeriod}, and
 * the number of full-time-equivalent holiday days. Every part is optional.
 *
 * <p>A salary is an amount of money, so it always has two decimals: it is rounded half up to cents
 * ({@code 10.745} becomes {@code 10.75}, {@code 10.744} becomes {@code 10.74}, {@code 3800} becomes
 * {@code 3800.00}). Hourly wages need the cents, and one scale keeps amounts comparable and shown
 * the same way.
 *
 * <p>Which combinations are allowed (not empty, a currency and period with every salary, the
 * minimum not above the maximum) is a contract rule, checked by the REST adapter before it gets
 * here.
 */
public record Offer(
    BigDecimal salaryMin,
    BigDecimal salaryMax,
    Currency currency,
    SalaryPeriod salaryPeriod,
    BigDecimal numberOfHolidays) {
  private static final int SALARY_SCALE = 2;

  public Offer {
    salaryMin = roundToCents(salaryMin);
    salaryMax = roundToCents(salaryMax);
  }

  private static BigDecimal roundToCents(BigDecimal salary) {
    return salary == null ? null : salary.setScale(SALARY_SCALE, RoundingMode.HALF_UP);
  }
}
