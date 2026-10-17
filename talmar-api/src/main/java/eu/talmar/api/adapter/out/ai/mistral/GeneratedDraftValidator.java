package eu.talmar.api.adapter.out.ai.mistral;

import eu.talmar.api.domain.generation.valueobject.GeneratedDraft;
import eu.talmar.api.shared.validation.TextContentRules;

/**
 * Validates a draft where it enters the application, at the AI adapter: it must be saveable
 * unchanged through the manual description endpoint ({@code POST /vacancy/{id}/description}). Every
 * text is present, within the contract's length limit for that section and passes the same content
 * rules as user input. A draft the user could not save is useless to them and a sign that the
 * output was manipulated, so it is never shown.
 */
final class GeneratedDraftValidator {

  /** Same limits as the contract's {@code VacancyDescriptionRequest}. */
  static final int SUMMARY_MAX_LENGTH = 1000;

  static final int JOB_DESCRIPTION_MAX_LENGTH = 5000;
  static final int TASKS_MAX_LENGTH = 5000;

  private GeneratedDraftValidator() {}

  /**
   * @throws InvalidModelOutputException naming the first section that breaks a rule
   */
  static void validate(GeneratedDraft draft) {
    validateSection("summary", draft.summary(), SUMMARY_MAX_LENGTH);
    validateSection("jobDescription", draft.jobDescription(), JOB_DESCRIPTION_MAX_LENGTH);
    validateSection("tasks", draft.tasks(), TASKS_MAX_LENGTH);
  }

  private static void validateSection(String section, String text, int maxLength) {
    if (text == null || text.isBlank()) {
      throw new InvalidModelOutputException("misses " + section);
    }
    if (text.length() > maxLength) {
      throw new InvalidModelOutputException(section + " exceeds " + maxLength + " characters");
    }
    if (!TextContentRules.isSafe(text)) {
      throw new InvalidModelOutputException(section + " breaks the content rules");
    }
  }
}
