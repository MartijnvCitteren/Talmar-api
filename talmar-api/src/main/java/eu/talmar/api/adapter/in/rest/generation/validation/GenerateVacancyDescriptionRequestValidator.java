package eu.talmar.api.adapter.in.rest.generation.validation;

import eu.talmar.api.adapter.in.rest.generation.InvalidGenerationRequestException;
import eu.talmar.api.shared.validation.TextContentRules;
import eu.talmar.api.vacancy.adapter.in.web.contract.GenerateVacancyDescriptionRequest;
import org.springframework.stereotype.Component;

/**
 * Applies the content rules for vacancy text to the three answers before they reach a use case.
 * Presence and length come from the contract (bean validation, which runs first). This is the first
 * line of defence against prompt injection: markup and control characters never reach the model.
 */
@Component
public class GenerateVacancyDescriptionRequestValidator {

  /**
   * Validates the three answers.
   *
   * @throws InvalidGenerationRequestException naming the first field that contains raw HTML-like
   *     tags or disallowed control characters
   */
  public void validate(GenerateVacancyDescriptionRequest request) {
    validateField("mostImportantTasks", request.getMostImportantTasks());
    validateField("team", request.getTeam());
    validateField("whyNiceJob", request.getWhyNiceJob());
  }

  private void validateField(String fieldName, String value) {
    if (TextContentRules.containsRawTag(value)) {
      throw new InvalidGenerationRequestException(
          fieldName, fieldName + " must not contain raw HTML or script-like tags");
    }
    if (TextContentRules.containsDisallowedControlCharacter(value)) {
      throw new InvalidGenerationRequestException(
          fieldName, fieldName + " must not contain disallowed control characters");
    }
  }
}
