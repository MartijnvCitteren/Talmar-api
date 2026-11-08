package eu.talmar.api.adapter.in.rest.vacancy.mapper.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import eu.talmar.api.domain.vacancy.valueobject.VacancyDescriptionSource;
import eu.talmar.api.vacancy.adapter.in.web.contract.ContactPerson;
import eu.talmar.api.vacancy.adapter.in.web.contract.CurrencyCode;
import eu.talmar.api.vacancy.adapter.in.web.contract.Language;
import eu.talmar.api.vacancy.adapter.in.web.contract.Location;
import eu.talmar.api.vacancy.adapter.in.web.contract.Offer;
import eu.talmar.api.vacancy.adapter.in.web.contract.SalaryPeriod;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyCategory;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyDescriptionRequest;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyOfferRequest;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyUpdateRequest;
import eu.talmar.api.vacancy.adapter.in.web.contract.WorkplaceType;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VacancyUpdateRequestMapperTest {

  private final VacancyUpdateRequestMapper mapper =
      new VacancyUpdateRequestMapperImpl(
          new VacancyCategoryMapperImpl(),
          new VacancyLocationMapperImpl(),
          new WorkplaceTypeMapperImpl());

  @Test
  @DisplayName(
      "given a vacancy id and offer request, when toCommand then maps every field, normalising"
          + " phone number and e-mail address")
  void givenVacancyIdAndOfferRequestWhenToCommandThenMapsEveryField() {
    var id = UUID.randomUUID();
    var contactPerson =
        new ContactPerson("Sanne de Vries", "Sanne.deVries@ACME.nl")
            .role("Hiring manager")
            .phone("+31 (0)6 12 34 56 78");
    var offer =
        new Offer()
            .salaryMin(new BigDecimal("3800.75"))
            .salaryMax(new BigDecimal("4601.92"))
            .currency(CurrencyCode.PLN)
            .salaryPeriod(SalaryPeriod.MONTHLY)
            .numberOfHolidays(new BigDecimal("26"));

    var command = mapper.toCommand(id, new VacancyOfferRequest(contactPerson, offer));

    assertEquals(id, command.vacancyId());
    assertEquals(contactPerson.getName(), command.contactPerson().name());
    assertEquals(contactPerson.getRole(), command.contactPerson().role());
    assertEquals("+31612345678", command.contactPerson().phone().value());
    assertEquals("sanne.devries@acme.nl", command.contactPerson().email().value());
    assertEquals(offer.getSalaryMin(), command.offer().salaryMin());
    assertEquals(offer.getSalaryMax(), command.offer().salaryMax());
    assertEquals(offer.getCurrency().getValue(), command.offer().currency().getCurrencyCode());
    assertEquals(offer.getSalaryPeriod().getValue(), command.offer().salaryPeriod().name());
    assertEquals(offer.getNumberOfHolidays(), command.offer().numberOfHolidays());
    assertNull(command.jobTitle());
  }

  @Test
  @DisplayName(
      "given a contact person without phone and an offer without salary, when toCommand then those"
          + " parts are null")
  void givenNoPhoneAndNoSalaryWhenToCommandThenThosePartsAreNull() {
    var request =
        new VacancyOfferRequest(
            new ContactPerson("Sanne de Vries", "sanne@acme.nl"),
            new Offer().numberOfHolidays(new BigDecimal("25")));

    var command = mapper.toCommand(UUID.randomUUID(), request);

    assertNull(command.contactPerson().phone());
    assertNull(command.offer().currency());
    assertNull(command.offer().salaryPeriod());
  }

  @Test
  @DisplayName(
      "given an update request with every field, when toCommand then maps every field and the"
          + " description is MANUAL")
  void givenUpdateRequestWithEveryFieldWhenToCommandThenMapsEveryField() {
    var id = UUID.randomUUID();
    var request =
        new VacancyUpdateRequest()
            .jobTitle("Senior Sales Manager")
            .language(Language.DE)
            .category(VacancyCategory.MANAGEMENT)
            .location(new Location("DE", "Berlin"))
            .workplaceType(WorkplaceType.REMOTE)
            .minHoursPerWeek(new BigDecimal("36"))
            .maxHoursPerWeek(new BigDecimal("38"))
            .description(new VacancyDescriptionRequest().summary("Summary").tasks("Tasks"))
            .contactPerson(new ContactPerson("Sanne de Vries", "sanne@acme.nl"))
            .offer(new Offer().numberOfHolidays(new BigDecimal("25")));

    var command = mapper.toCommand(id, request);

    assertEquals(id, command.vacancyId());
    assertEquals(request.getJobTitle(), command.jobTitle());
    assertEquals(request.getLanguage().getValue(), command.language().name());
    assertEquals(request.getCategory().getValue(), command.category().name());
    assertEquals(request.getLocation().getCountry(), command.location().country());
    assertEquals(request.getLocation().getCity(), command.location().city());
    assertEquals(request.getWorkplaceType().getValue(), command.workplaceType().name());
    assertEquals(request.getMinHoursPerWeek(), command.hoursPerWeek().minHours());
    assertEquals(request.getMaxHoursPerWeek(), command.hoursPerWeek().maxHours());
    assertEquals(request.getDescription().getSummary(), command.description().summary());
    assertEquals(request.getDescription().getTasks(), command.description().tasks());
    assertEquals(VacancyDescriptionSource.MANUAL, command.description().source());
    assertEquals(request.getContactPerson().getName(), command.contactPerson().name());
    assertEquals(request.getOffer().getNumberOfHolidays(), command.offer().numberOfHolidays());
  }

  @Test
  @DisplayName("given an empty update request, when toCommand then every part is null")
  void givenEmptyUpdateRequestWhenToCommandThenEveryPartIsNull() {
    var command = mapper.toCommand(UUID.randomUUID(), new VacancyUpdateRequest());

    assertNull(command.jobTitle());
    assertNull(command.language());
    assertNull(command.category());
    assertNull(command.location());
    assertNull(command.workplaceType());
    assertNull(command.hoursPerWeek());
    assertNull(command.description());
    assertNull(command.contactPerson());
    assertNull(command.offer());
  }
}
