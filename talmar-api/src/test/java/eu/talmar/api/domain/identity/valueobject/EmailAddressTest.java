package eu.talmar.api.domain.identity.valueobject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmailAddressTest {

  @Test
  @DisplayName("given an address with upper case, when created then it is lower case")
  void givenAddressWithUpperCaseWhenCreatedThenItIsLowerCase() {
    var email = new EmailAddress("Jane.Doe@ACME.nl");

    assertEquals("jane.doe@acme.nl", email.value());
  }

  @Test
  @DisplayName("given an address, when domain then returns the part after the at sign")
  void givenAddressWhenDomainThenReturnsThePartAfterTheAtSign() {
    var email = new EmailAddress("jane@sales.acme.co.uk");

    assertEquals("sales.acme.co.uk", email.domain());
  }

  @Test
  @DisplayName("given an address, when toString then the address is not in the output")
  void givenAddressWhenToStringThenTheAddressIsNotInTheOutput() {
    var email = new EmailAddress("jane.doe@acme.nl");

    assertFalse(email.toString().contains("jane"));
  }
}
