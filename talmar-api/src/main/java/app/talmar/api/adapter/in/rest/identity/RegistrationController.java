package app.talmar.api.adapter.in.rest.identity;

import app.talmar.api.adapter.in.rest.identity.mapper.request.RegistrationConfirmationRequestMapper;
import app.talmar.api.adapter.in.rest.identity.mapper.request.RegistrationRequestMapper;
import app.talmar.api.adapter.in.rest.identity.mapper.response.RegistrationConfirmationResponseMapper;
import app.talmar.api.adapter.in.rest.identity.validation.WorkEmailAddressValidator;
import app.talmar.api.application.port.in.ConfirmRegistrationUseCase;
import app.talmar.api.application.port.in.RegisterUserUseCase;
import app.talmar.api.identity.adapter.in.rest.RegistrationApi;
import app.talmar.api.identity.adapter.in.web.contract.RegistrationConfirmationRequest;
import app.talmar.api.identity.adapter.in.web.contract.RegistrationConfirmationResponse;
import app.talmar.api.identity.adapter.in.web.contract.RegistrationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registration endpoints (ADR 0003). Requests are not logged: they carry names, an e-mail address,
 * a verification token or a password.
 */
@RestController
@RequiredArgsConstructor
public class RegistrationController implements RegistrationApi {
  private final WorkEmailAddressValidator workEmailAddressValidator;
  private final RegistrationRequestMapper registrationRequestMapper;
  private final RegistrationConfirmationRequestMapper confirmationRequestMapper;
  private final RegistrationConfirmationResponseMapper confirmationResponseMapper;
  private final RegisterUserUseCase registerUserUseCase;
  private final ConfirmRegistrationUseCase confirmRegistrationUseCase;

  /** Always 202 for a valid request, whether or not the address was registered before. */
  @Override
  public ResponseEntity<Void> register(RegistrationRequest registrationRequest) {
    workEmailAddressValidator.validate(registrationRequest);
    registerUserUseCase.register(registrationRequestMapper.toCommand(registrationRequest));
    return ResponseEntity.accepted().build();
  }

  @Override
  public ResponseEntity<RegistrationConfirmationResponse> confirmRegistration(
      RegistrationConfirmationRequest registrationConfirmationRequest) {
    var status =
        confirmRegistrationUseCase.confirm(
            confirmationRequestMapper.toCommand(registrationConfirmationRequest));
    return ResponseEntity.ok(confirmationResponseMapper.toResponse(status));
  }
}
