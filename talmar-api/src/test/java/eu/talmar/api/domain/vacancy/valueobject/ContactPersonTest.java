package eu.talmar.api.domain.vacancy.valueobject;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.talmar.api.testSupport.VacancyFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ContactPersonTest {

  @Test
  @DisplayName(
      "given a contact person, when toString then only the role is in the output, no personal"
          + " data")
  void givenContactPersonWhenToStringThenOnlyTheRoleIsInTheOutput() {
    var contactPerson = VacancyFactory.CONTACT_PERSON;

    var text = contactPerson.toString();

    assertTrue(text.contains(contactPerson.role()));
    assertFalse(text.contains(contactPerson.name()));
    assertFalse(text.contains(contactPerson.email().value()));
    assertFalse(text.contains(contactPerson.phone().value()));
  }
}
