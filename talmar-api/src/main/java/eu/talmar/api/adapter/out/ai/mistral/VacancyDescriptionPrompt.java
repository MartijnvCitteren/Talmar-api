package eu.talmar.api.adapter.out.ai.mistral;

import eu.talmar.api.domain.generation.valueobject.GenerationInput;
import eu.talmar.api.domain.vacancy.valueobject.Language;
import java.util.Map;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * The versioned prompt for drafting a vacancy description, rendered from the classpath templates
 * under {@code prompt/vacancy-description/}. A prompt change is a reviewed PR, never a runtime
 * setting: it bumps {@link #VERSION} (major.minor.patch) and adds a changelog line to the header
 * comment of the templates, so the files themselves keep the trail of what changed. The header is a
 * template comment, removed when rendering, so the model never sees it. Every generation records
 * the version it used.
 *
 * <p>The user's answers go into the user message only, each wrapped in fixed delimiters that the
 * system prompt declares to be data. The delimiter brackets are stripped from the answers first, so
 * an answer cannot close its own block and pose as an instruction.
 */
@Component
class VacancyDescriptionPrompt {

  static final String VERSION = "vacancy-description/V1.0.0";

  private static final String DELIMITER_OPEN = "[[[";
  private static final String DELIMITER_CLOSE = "]]]";

  private final PromptTemplate systemTemplate = load("system.st");
  private final PromptTemplate userTemplate = load("user.st");

  /**
   * @param canary a token unique to this call; seeing it in the output means the model leaked its
   *     instructions
   */
  String system(Language language, String canary) {
    return systemTemplate
        .render(Map.of("language", languageName(language), "canary", canary))
        .strip();
  }

  String user(GenerationInput input) {
    return userTemplate
        .render(
            Map.of(
                "jobTitle", stripDelimiters(input.jobTitle()),
                "mostImportantTasks", stripDelimiters(input.mostImportantTasks()),
                "team", stripDelimiters(input.team()),
                "whyNiceJob", stripDelimiters(input.whyNiceJob())))
        .strip();
  }

  static String stripDelimiters(String text) {
    String stripped = text;
    // Repeat until stable: removing "[[[" from "[[[[[[" leaves "[[[" again.
    while (stripped.contains(DELIMITER_OPEN) || stripped.contains(DELIMITER_CLOSE)) {
      stripped = stripped.replace(DELIMITER_OPEN, "").replace(DELIMITER_CLOSE, "");
    }
    return stripped;
  }

  private static String languageName(Language language) {
    return switch (language) {
      case NL -> "Dutch";
      case EN -> "English";
      case DE -> "German";
      case FR -> "French";
    };
  }

  private static PromptTemplate load(String fileName) {
    var resource = new ClassPathResource("prompt/vacancy-description/" + fileName);
    return PromptTemplate.builder().resource(resource).build();
  }
}
