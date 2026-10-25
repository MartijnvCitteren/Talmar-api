package eu.talmar.api.adapter.out.persistence.vacancy;

import eu.talmar.api.domain.vacancy.valueobject.SalaryPeriod;
import eu.talmar.api.shared.gdpr.ProcessData;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.util.Currency;
import lombok.Getter;
import lombok.Setter;

/**
 * The offer of a vacancy, as columns of the {@code vacancy} table. Hibernate loads it as {@code
 * null} when every column is {@code null}, which matches a vacancy without an offer. Process data:
 * it describes the job, not a person.
 */
@Getter
@Setter
@Embeddable
public class OfferJpaEmbeddable {
  @ProcessData
  @Column(name = "offer_salary_min")
  private Integer salaryMin;

  @ProcessData
  @Column(name = "offer_salary_max")
  private Integer salaryMax;

  @ProcessData
  @Column(name = "offer_currency", length = 3)
  private Currency currency;

  @ProcessData
  @Enumerated(EnumType.STRING)
  @Column(name = "offer_salary_period", length = 7)
  private SalaryPeriod salaryPeriod;

  @ProcessData
  @Column(name = "offer_number_of_holidays", precision = 4, scale = 2)
  private BigDecimal numberOfHolidays;
}
