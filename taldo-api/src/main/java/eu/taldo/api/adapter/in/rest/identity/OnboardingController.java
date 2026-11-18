package eu.taldo.api.adapter.in.rest.identity;

import eu.taldo.api.adapter.in.rest.AuthenticatedUser;
import eu.taldo.api.adapter.in.rest.identity.mapper.request.OnboardingRequestMapper;
import eu.taldo.api.adapter.in.rest.identity.mapper.response.OnboardingResponseMapper;
import eu.taldo.api.application.port.in.identity.OnboardUserUseCase;
import eu.taldo.api.identity.adapter.in.rest.OnboardingApi;
import eu.taldo.api.identity.adapter.in.web.contract.OnboardingRequest;
import eu.taldo.api.identity.adapter.in.web.contract.OnboardingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Onboarding after the first login (ADR 0006, section 2). The account comes from the token. */
@RestController
@RequiredArgsConstructor
public class OnboardingController implements OnboardingApi {
  private final OnboardingRequestMapper requestMapper;
  private final OnboardingResponseMapper responseMapper;
  private final OnboardUserUseCase onboardUserUseCase;

  @Override
  public ResponseEntity<OnboardingResponse> onboard(OnboardingRequest onboardingRequest) {
    var command = requestMapper.toCommand(AuthenticatedUser.subject(), onboardingRequest);
    return ResponseEntity.ok(responseMapper.toResponse(onboardUserUseCase.onboard(command)));
  }
}
