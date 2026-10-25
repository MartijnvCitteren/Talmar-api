package eu.talmar.api.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.talmar.api.application.port.in.command.SetVacancyOfferCommand;
import eu.talmar.api.application.port.out.VacancyRepository;
import eu.talmar.api.domain.vacancy.VacancyNotFoundException;
import eu.talmar.api.domain.vacancy.valueobject.ContactPerson;
import eu.talmar.api.domain.vacancy.valueobject.Offer;
import eu.talmar.api.testSupport.VacancyFactory;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SetVacancyOfferServiceTest {

  @Mock private VacancyRepository vacancyRepository;

  @InjectMocks private SetVacancyOfferService service;

  @Test
  @DisplayName(
      "given a vacancy with a contact person and an offer, when setOffer then both are replaced"
          + " as a whole and saved")
  void givenVacancyWithContactPersonAndOfferWhenSetOfferThenBothReplacedAndSaved() {
    var vacancy =
        VacancyFactory.getFilledCoreVacancy()
            .contactPerson(VacancyFactory.CONTACT_PERSON)
            .offer(VacancyFactory.OFFER)
            .build();
    when(vacancyRepository.findById(vacancy.getId())).thenReturn(Optional.of(vacancy));
    var newContactPerson =
        new ContactPerson("Sanne de Vries", null, null, VacancyFactory.CONTACT_PERSON.email());
    var newOffer = new Offer(null, null, null, null, new BigDecimal("30"));
    var command =
        SetVacancyOfferCommand.builder()
            .vacancyId(vacancy.getId())
            .contactPerson(newContactPerson)
            .offer(newOffer)
            .build();

    var result = service.setOffer(command);

    assertEquals(command.contactPerson(), result.getContactPerson());
    assertEquals(command.offer(), result.getOffer());
    verify(vacancyRepository).save(vacancy);
  }

  @Test
  @DisplayName("given an unknown vacancy id, when setOffer then throws and never saves")
  void givenUnknownVacancyIdWhenSetOfferThenThrowsAndNeverSaves() {
    var vacancyId = UUID.randomUUID();
    when(vacancyRepository.findById(vacancyId)).thenReturn(Optional.empty());
    var command = SetVacancyOfferCommand.builder().vacancyId(vacancyId).build();

    assertThrows(VacancyNotFoundException.class, () -> service.setOffer(command));

    verify(vacancyRepository, never()).save(any());
  }
}
