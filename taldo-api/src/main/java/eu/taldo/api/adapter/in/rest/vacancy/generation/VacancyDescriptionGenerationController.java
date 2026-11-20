package eu.taldo.api.adapter.in.rest.vacancy.generation;

import eu.taldo.api.adapter.in.rest.vacancy.generation.mapper.request.GenerateVacancyDescriptionRequestMapper;
import eu.taldo.api.adapter.in.rest.vacancy.generation.mapper.response.VacancyDescriptionGenerationResponseMapper;
import eu.taldo.api.adapter.in.rest.vacancy.generation.validation.GenerateVacancyDescriptionRequestValidator;
import eu.taldo.api.application.port.in.vacancy.generation.GetVacancyDescriptionGenerationUseCase;
import eu.taldo.api.application.port.in.vacancy.generation.StartVacancyDescriptionGenerationUseCase;
import eu.taldo.api.vacancy.adapter.in.rest.GenerationApi;
import eu.taldo.api.vacancy.adapter.in.web.contract.GenerateVacancyDescriptionRequest;
import eu.taldo.api.vacancy.adapter.in.web.contract.VacancyDescriptionGeneration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * AI-drafted vacancy descriptions. The paths stay under {@code /vacancy/{id}/...} because that is
 * how the client thinks about it, while the generation is its own aggregate; this controller maps
 * between the two (ADR 0002).
 */
@RestController
@RequiredArgsConstructor
public class VacancyDescriptionGenerationController implements GenerationApi {
  private final GenerateVacancyDescriptionRequestValidator requestValidator;
  private final GenerateVacancyDescriptionRequestMapper requestMapper;
  private final VacancyDescriptionGenerationResponseMapper responseMapper;
  private final StartVacancyDescriptionGenerationUseCase startGenerationUseCase;
  private final GetVacancyDescriptionGenerationUseCase getGenerationUseCase;

  @Override
  public ResponseEntity<VacancyDescriptionGeneration> generateVacancyDescription(
      UUID id, GenerateVacancyDescriptionRequest generateVacancyDescriptionRequest) {
    requestValidator.validate(generateVacancyDescriptionRequest);
    var command = requestMapper.toCommand(id, generateVacancyDescriptionRequest);
    var pendingGeneration = startGenerationUseCase.start(command);
    var pollLocation =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{generationId}")
            .buildAndExpand(pendingGeneration.getId())
            .toUri();
    return ResponseEntity.accepted()
        .location(pollLocation)
        .body(responseMapper.toResponse(pendingGeneration));
  }

  @Override
  public ResponseEntity<VacancyDescriptionGeneration> getVacancyDescriptionGeneration(
      UUID id, UUID generationId) {
    var generation = getGenerationUseCase.get(id, generationId);
    return ResponseEntity.ok(responseMapper.toResponse(generation));
  }
}
