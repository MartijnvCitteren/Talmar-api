package eu.talmar.api.adapter.out.ai.mistral;

import eu.talmar.api.application.port.out.GenerationResult;
import eu.talmar.api.application.port.out.VacancyDescriptionGenerator;
import eu.talmar.api.domain.generation.valueobject.GeneratedDraft;
import eu.talmar.api.domain.generation.valueobject.GenerationFailureReason;
import eu.talmar.api.domain.generation.valueobject.GenerationInput;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.mistralai.MistralAiChatOptions;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.stereotype.Component;

/**
 * Drafts a vacancy description with Mistral through Spring AI. The only class that knows the
 * provider; model, temperature, token limit, timeouts and retries are configuration (see {@code
 * application.yml}).
 *
 * <p>Defences against prompt injection, besides the input validation at the REST edge: one fixed
 * task in one call with no conversation, user text only inside delimited data blocks, the output
 * forced into a strict JSON schema in which the model can refuse ({@code inputRelevant}), a
 * per-call canary token in the system prompt that marks the output invalid when it leaks, and the
 * content and length rules of a description applied to the draft ({@link GeneratedDraftValidator}).
 *
 * <p>Provider errors are split by whether retrying helps (Mistral's own guidance): 429 and 5xx
 * responses and I/O failures are retried by Spring AI's retry, and when the retries run out they
 * become {@code PROVIDER_ERROR} or {@code TIMEOUT}, which the user may retry later. Every other 4xx
 * (bad request, invalid key, no access, unknown model, validation error) is not retried: it fails
 * the same way until we fix our configuration, so it becomes {@code PROVIDER_UNAVAILABLE} at once.
 *
 * <p>Never throws for a failed generation; every failure becomes a {@link
 * GenerationResult.Rejected}. Nothing here logs the prompt, the answers or the output.
 */
@Log4j2
@Component
@RequiredArgsConstructor
public class MistralVacancyDescriptionGenerator implements VacancyDescriptionGenerator {
  private static final BeanOutputConverter<MistralDraftResponse> OUTPUT_CONVERTER =
      new BeanOutputConverter<>(MistralDraftResponse.class);

  private final ChatClient chatClient;
  private final VacancyDescriptionPrompt prompt;

  @Override
  public GenerationResult generate(GenerationInput input) {
    var canary = "ref-" + UUID.randomUUID();
    try {
      return processResponse(callMistral(input, canary), canary);
    } catch (InvalidModelOutputException e) {
      log.warn("Mistral output is invalid: {}", e.getMessage());
      return rejected(GenerationFailureReason.INVALID_OUTPUT);
    } catch (NonTransientAiException e) {
      log.error("Mistral refused the request, AI generation is unavailable: {}", e.getMessage());
      return rejected(GenerationFailureReason.PROVIDER_UNAVAILABLE);
    } catch (RuntimeException e) {
      return handleProviderFailure(e);
    }
  }

  private ChatResponse callMistral(GenerationInput input, String canary) {
    return chatClient
        .prompt()
        .options(MistralAiChatOptions.builder().outputSchema(OUTPUT_CONVERTER.getJsonSchema()))
        .system(prompt.system(input.language(), canary))
        .user(prompt.user(input))
        .call()
        .chatResponse();
  }

  private GenerationResult processResponse(ChatResponse response, String canary) {
    var text = textOf(response);
    var model = response.getMetadata().getModel();
    validateResponse(text, model, canary);

    var answer = parse(text);
    if (!answer.inputRelevant()) {
      log.info("Mistral rejected the input as not describing a job");
      return rejected(GenerationFailureReason.INPUT_REJECTED);
    }

    var draft = new GeneratedDraft(answer.summary(), answer.jobDescription(), answer.tasks());
    GeneratedDraftValidator.validate(draft);
    return new GenerationResult.Drafted(draft, model, VacancyDescriptionPrompt.VERSION);
  }

  private static String textOf(ChatResponse response) {
    var result = response == null ? null : response.getResult();
    if (result == null) {
      throw new InvalidModelOutputException("has no result");
    }
    return result.getOutput().getText();
  }

  /** Checks the raw answer before it is parsed. */
  private static void validateResponse(String text, String model, String canary) {
    if (text == null || text.isBlank()) {
      throw new InvalidModelOutputException("is an empty message");
    }
    if (text.contains(canary)) {
      throw new InvalidModelOutputException("contains the canary token; the instructions leaked");
    }
    if (model == null || model.isBlank()) {
      throw new InvalidModelOutputException("reports no model version");
    }
  }

  private static MistralDraftResponse parse(String text) {
    try {
      return OUTPUT_CONVERTER.convert(text);
    } catch (RuntimeException e) {
      throw new InvalidModelOutputException(
          "does not match the response schema: " + e.getClass().getName());
    }
  }

  /**
   * A failure that was worth retrying and still failed after the last attempt. A timeout surfaces
   * as an I/O exception somewhere in the cause chain; anything else (5xx, 429, connection refused)
   * is a provider error.
   */
  private static GenerationResult handleProviderFailure(RuntimeException e) {
    if (hasTimeoutCause(e)) {
      log.warn("Mistral did not answer in time: {}", e.toString());
      return rejected(GenerationFailureReason.TIMEOUT);
    }
    log.warn("Mistral call failed after its retries: {}", e.toString());
    return rejected(GenerationFailureReason.PROVIDER_ERROR);
  }

  private static boolean hasTimeoutCause(Throwable e) {
    for (Throwable cause = e; cause != null; cause = cause.getCause()) {
      if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException) {
        return true;
      }
    }
    return false;
  }

  private static GenerationResult rejected(GenerationFailureReason reason) {
    return new GenerationResult.Rejected(reason);
  }
}
