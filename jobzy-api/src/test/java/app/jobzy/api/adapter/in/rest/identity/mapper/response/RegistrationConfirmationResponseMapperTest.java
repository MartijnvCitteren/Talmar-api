package app.jobzy.api.adapter.in.rest.identity.mapper.response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.jobzy.api.domain.identity.valueobject.UserStatus;
import app.jobzy.api.identity.adapter.in.web.contract.AccountStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegistrationConfirmationResponseMapperTest {

  private final RegistrationConfirmationResponseMapper mapper =
      new RegistrationConfirmationResponseMapperImpl();

  @Test
  @DisplayName("given a confirmed status, when mapped then the contract status has the same name")
  void givenConfirmedStatusWhenMappedThenContractStatusHasTheSameName() {
    assertEquals(AccountStatus.ACTIVE, mapper.toResponse(UserStatus.ACTIVE).getStatus());
    assertEquals(
        AccountStatus.PENDING_APPROVAL, mapper.toResponse(UserStatus.PENDING_APPROVAL).getStatus());
  }

  @Test
  @DisplayName("given pending verification, when mapped then it fails because it is a bug")
  void givenPendingVerificationWhenMappedThenItFails() {
    assertThrows(
        IllegalArgumentException.class, () -> mapper.toResponse(UserStatus.PENDING_VERIFICATION));
  }
}
