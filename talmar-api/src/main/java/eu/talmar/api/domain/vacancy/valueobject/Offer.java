package eu.talmar.api.domain.vacancy.valueobject;

import java.math.BigDecimal;
import java.util.Currency;

/**
 * What the employer offers: a gross salary range in whole units of {@code currency} per {@code
 * salaryPeriod}, and the number of full-time-equivalent holiday days. Every part is optional.
 *
 * <p>Which combinations are allowed (not empty, a currency and period with every salary, the
 * minimum not above the maximum) is a contract rule, checked by the REST adapter before it gets
 * here.
 */
public record Offer(
    Integer salaryMin,
    Integer salaryMax,
    Currency currency,
    SalaryPeriod salaryPeriod,
    BigDecimal numberOfHolidays) {}
