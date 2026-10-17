package eu.talmar.api.testSupport;

import eu.talmar.api.domain.generation.VacancyDescriptionGeneration;
import eu.talmar.api.domain.generation.valueobject.GeneratedDraft;
import eu.talmar.api.domain.generation.valueobject.GenerationInput;
import eu.talmar.api.domain.vacancy.valueobject.Language;
import java.time.LocalDateTime;
import java.util.UUID;

public class GenerationFactory {
  public static final UUID VACANCY_ID = UUID.fromString("01a10000-0000-7000-8000-000000000001");
  public static final LocalDateTime REQUESTED_AT = LocalDateTime.of(2026, 1, 1, 9, 0);
  public static final String MODEL = "mistral-small-2506";
  public static final String PROMPT_VERSION = "vacancy-description/V1.0.0";

  public static GenerationInput input() {
    return new GenerationInput(
        "Sales Manager",
        Language.NL,
        "Visiting customers",
        "Five account managers",
        "A lot of freedom");
  }

  public static GeneratedDraft draft() {
    return new GeneratedDraft("A summary", "A job description", "- Task one\n- Task two");
  }

  public static VacancyDescriptionGeneration pending() {
    return VacancyDescriptionGeneration.newPending(VACANCY_ID, input(), REQUESTED_AT);
  }

  public static VacancyDescriptionGeneration completed() {
    var generation = pending();
    generation.acceptDraft(draft(), MODEL, PROMPT_VERSION, REQUESTED_AT.plusSeconds(10));
    return generation;
  }
}
