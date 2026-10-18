package eu.talmar.api.adapter.in.rest.identity;

import eu.talmar.api.adapter.in.rest.AuthenticatedUser;
import eu.talmar.api.adapter.in.rest.identity.mapper.request.NameChangeRequestMapper;
import eu.talmar.api.application.port.in.ChangeNameUseCase;
import eu.talmar.api.identity.adapter.in.rest.MeApi;
import eu.talmar.api.identity.adapter.in.web.contract.NameChangeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * The logged-in user's own profile ({@code /me}). The user comes from the token. Requests are not
 * logged: they carry names.
 */
@RestController
@RequiredArgsConstructor
public class ProfileController implements MeApi {
  private final NameChangeRequestMapper nameChangeRequestMapper;
  private final ChangeNameUseCase changeNameUseCase;

  @Override
  public ResponseEntity<Void> changeName(NameChangeRequest nameChangeRequest) {
    changeNameUseCase.changeName(
        nameChangeRequestMapper.toCommand(AuthenticatedUser.subject(), nameChangeRequest));
    return ResponseEntity.noContent().build();
  }
}
