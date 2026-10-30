package eu.talmar.api.domain.vacancy.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.util.Currency;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OfferTest {
  private static final Currency EUR = Currency.getInstance("EUR");

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource({
    "10.745, 10.75",
    "10.744, 10.74",
    "4601.925, 4601.93",
    "3800.75, 3800.75",
    "3800.7, 3800.70",
    "3800, 3800.00",
    "0, 0.00"
  })
  @DisplayName("given a salary, when the offer is created then it is rounded half up to 2 decimals")
  void givenSalaryWhenOfferIsCreatedThenItIsRoundedHalfUpToTwoDecimals(
      String salary, String rounded) {
    var offer =
        new Offer(new BigDecimal(salary), new BigDecimal(salary), EUR, SalaryPeriod.MONTHLY, null);

    assertEquals(new BigDecimal(rounded), offer.salaryMin());
    assertEquals(new BigDecimal(rounded), offer.salaryMax());
  }

  @Test
  @DisplayName("given no salary, when the offer is created then the salary stays empty")
  void givenNoSalaryWhenOfferIsCreatedThenTheSalaryStaysEmpty() {
    var offer = new Offer(null, null, null, null, new BigDecimal("25"));

    assertNull(offer.salaryMin());
    assertNull(offer.salaryMax());
  }
}
