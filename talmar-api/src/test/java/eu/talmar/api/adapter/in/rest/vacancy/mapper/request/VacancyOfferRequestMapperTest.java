package eu.talmar.api.adapter.in.rest.vacancy.mapper.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import eu.talmar.api.vacancy.adapter.in.web.contract.ContactPerson;
import eu.talmar.api.vacancy.adapter.in.web.contract.CurrencyCode;
import eu.talmar.api.vacancy.adapter.in.web.contract.Offer;
import eu.talmar.api.vacancy.adapter.in.web.contract.SalaryPeriod;
import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyOfferRequest;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VacancyOfferRequestMapperTest {

  @InjectMocks private VacancyOfferRequestMapperImpl mapper;

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
            .salaryMin(3800)
            .salaryMax(4600)
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
}
