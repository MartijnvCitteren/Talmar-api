package eu.talmar.api.testSupport;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static eu.talmar.api.integration.IntegrationCommons.JSON_MAPPER;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import java.util.List;
import java.util.Map;

/**
 * A WireMock stand-in for Mistral's chat completions API, shared by every test that runs the real
 * Mistral adapter. One server for the whole test run, so the Spring contexts that point at it can
 * be cached: a server per test class would get a new port while a cached context keeps the old one.
 * Tests reset it before use.
 *
 * <p>The JDK HTTP client tries an HTTP/2 cleartext upgrade on {@code http://} URLs, which
 * WireMock's Jetty cancels, so plain HTTP/2 is off. Mistral itself is reached over HTTPS, where
 * HTTP/2 is negotiated normally.
 */
public final class MistralStub {
  public static final String COMPLETIONS = "/v1/chat/completions";
  public static final String MODEL_VERSION = "mistral-small-2506";
  public static final String SUMMARY = "A summary";
  public static final String JOB_DESCRIPTION = "A job description";
  public static final String TASKS = "- Task one";

  public static final WireMockServer MISTRAL_WIREMOCK =
      new WireMockServer(wireMockConfig().dynamicPort().http2PlainDisabled(true));

  static {
    MISTRAL_WIREMOCK.start();
  }

  private MistralStub() {}

  /** A completion whose message content is the given text. */
  public static ResponseDefinitionBuilder completion(String content) {
    return aResponse()
        .withHeader("Content-Type", "application/json")
        .withBody(completionBody(content));
  }

  /** A completion with a valid structured draft, or a rejection of the input. */
  public static ResponseDefinitionBuilder draftCompletion(boolean inputRelevant) {
    return completion(draftJson(inputRelevant));
  }

  /** The structured answer the response schema asks for. */
  public static String draftJson(boolean inputRelevant) {
    return JSON_MAPPER.writeValueAsString(
        Map.of(
            "summary", inputRelevant ? SUMMARY : "",
            "jobDescription", inputRelevant ? JOB_DESCRIPTION : "",
            "tasks", inputRelevant ? TASKS : "",
            "inputRelevant", inputRelevant,
            "rejectionReason", inputRelevant ? "" : "The input does not describe a job."));
  }

  /** The shape of a Mistral chat completion response. */
  public static String completionBody(String content) {
    return JSON_MAPPER.writeValueAsString(
        Map.of(
            "id",
            "cmpl-1",
            "object",
            "chat.completion",
            "created",
            1_760_000_000L,
            "model",
            MODEL_VERSION,
            "choices",
            List.of(
                Map.of(
                    "index",
                    0,
                    "message",
                    Map.of("role", "assistant", "content", content),
                    "finish_reason",
                    "stop")),
            "usage",
            Map.of("prompt_tokens", 300, "total_tokens", 400, "completion_tokens", 100)));
  }
}
