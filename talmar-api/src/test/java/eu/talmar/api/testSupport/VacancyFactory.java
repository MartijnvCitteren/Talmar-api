package eu.talmar.api.testSupport;

import eu.talmar.api.application.port.in.command.CreateCoreVacancyCommand;
import eu.talmar.api.domain.identity.valueobject.EmailAddress;
import eu.talmar.api.domain.vacancy.Vacancy;
import eu.talmar.api.domain.vacancy.valueobject.ContactPerson;
import eu.talmar.api.domain.vacancy.valueobject.HoursPerWeek;
import eu.talmar.api.domain.vacancy.valueobject.Language;
import eu.talmar.api.domain.vacancy.valueobject.Location;
import eu.talmar.api.domain.vacancy.valueobject.Offer;
import eu.talmar.api.domain.vacancy.valueobject.PhoneNumber;
import eu.talmar.api.domain.vacancy.valueobject.SalaryPeriod;
import eu.talmar.api.domain.vacancy.valueobject.VacancyCategory;
import eu.talmar.api.domain.vacancy.valueobject.VacancyStatus;
import eu.talmar.api.domain.vacancy.valueobject.WorkplaceType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

public class VacancyFactory {
  public static final String JOB_TITLE = "Sales Manager";
  public static final Language LANGUAGE = Language.NL;
  public static final VacancyCategory CATEGORY = VacancyCategory.SALES;
  public static final BigDecimal MIN_HOURS_PER_WEEK = BigDecimal.valueOf(32);
  public static final BigDecimal MAX_HOURS_PER_WEEK = BigDecimal.valueOf(40);
  public static final String COUNTRY = "The Netherlands";
  public static final String CITY = "Amsterdam";
  public static final VacancyStatus STATUS = VacancyStatus.DRAFT;
  public static final WorkplaceType WORKPLACE_TYPE = WorkplaceType.HYBRID;
  public static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 1, 1, 9, 0);
  public static final ContactPerson CONTACT_PERSON =
      new ContactPerson(
          "Łukasz Żółć-Kowalski",
          "Hiring manager",
          new PhoneNumber("+48 601 234 567"),
          new EmailAddress("lukasz.kowalski@acme.pl"));
  public static final Offer OFFER =
      new Offer(
          3800, 4600, Currency.getInstance("EUR"), SalaryPeriod.MONTHLY, new BigDecimal("25.50"));

  public static Vacancy.Builder getFilledCoreVacancy() {
    return Vacancy.builder()
        .jobTitle(JOB_TITLE)
        .language(LANGUAGE)
        .category(CATEGORY)
        .hoursPerWeek(new HoursPerWeek(MIN_HOURS_PER_WEEK, MAX_HOURS_PER_WEEK))
        .location(new Location(COUNTRY, CITY))
        .status(STATUS)
        .workplaceType(WORKPLACE_TYPE)
        .createdAt(CREATED_AT);
  }

  public static CreateCoreVacancyCommand.CreateCoreVacancyCommandBuilder
      getFilledCreateCoreVacancyCommand() {
    return CreateCoreVacancyCommand.builder()
        .jobTitle(JOB_TITLE)
        .language(LANGUAGE)
        .category(CATEGORY)
        .location(new Location(COUNTRY, CITY))
        .workplaceType(WORKPLACE_TYPE)
        .hoursPerWeek(new HoursPerWeek(MIN_HOURS_PER_WEEK, MAX_HOURS_PER_WEEK));
  }
}
