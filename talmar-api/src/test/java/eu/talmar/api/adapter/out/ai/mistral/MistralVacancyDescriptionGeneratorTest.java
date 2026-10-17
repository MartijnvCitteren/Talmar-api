package eu.talmar.api.adapter.out.ai.mistral;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static eu.talmar.api.testSupport.MistralStub.COMPLETIONS;
import static eu.talmar.api.testSupport.MistralStub.MISTRAL_WIREMOCK;
import static eu.talmar.api.testSupport.MistralStub.completion;
import static eu.talmar.api.testSupport.MistralStub.completionBody;
import static eu.talmar.api.testSupport.MistralStub.draftCompletion;
import static eu.talmar.api.testSupport.MistralStub.draftJson;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import com.github.tomakehurst.wiremock.stubbing.Scenario;
import eu.talmar.api.application.port.out.GenerationResult;
import eu.talmar.api.domain.generation.valueobject.GenerationFailureReason;
import eu.talmar.api.domain.generation.valueobject.GenerationInput;
import eu.talmar.api.domain.vacancy.valueobject.Language;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Runs the Mistral adapter with the application's real Spring AI configuration (model options,
 * retry, HTTP client) against WireMock with the response shapes of Mistral's chat completions API.
 * Covers what only the adapter decides: the request it sends, which failures are retried, and how
 * an answer is classified. The happy path end to end is covered by the generation integration
 * tests. Only the timeout is shortened; never calls Mistral.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MistralVacancyDescriptionGeneratorTest {

  private static final GenerationInput INPUT =
      new GenerationInput(
          "Sales Manager",
          Language.NL,
          "Visiting customers",
          "Five account managers",
          "A lot of freedom");

  @DynamicPropertySource
  static void mistralProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.ai.mistralai.chat.base-url", MISTRAL_WIREMOCK::baseUrl);
    registry.add("spring.http.clients.read-timeout", () -> "500ms");
  }

  @Autowired private MistralVacancyDescriptionGenerator generator;

  @BeforeEach
  void resetMistral() {
    MISTRAL_WIREMOCK.resetAll();
  }

  @Test
  @DisplayName(
      "given a call, when generate then the request carries the configured model options, a strict"
          + " JSON schema and the answers only inside delimited blocks of the user message")
  void givenCallWhenGenerateThenRequestCarriesOptionsSchemaAndDelimitedAnswers() {
    MISTRAL_WIREMOCK.stubFor(post(COMPLETIONS).willReturn(draftCompletion(true)));

    generator.generate(INPUT);

    MISTRAL_WIREMOCK.verify(
        postRequestedFor(urlEqualTo(COMPLETIONS))
            .withHeader("Authorization", equalTo("Bearer test-key"))
            .withRequestBody(matchingJsonPath("$.model", equalTo("mistral-small-latest")))
            .withRequestBody(matchingJsonPath("$.temperature", equalTo("1.1")))
            .withRequestBody(matchingJsonPath("$.max_tokens", equalTo("1000")))
            .withRequestBody(matchingJsonPath("$.response_format.type", equalTo("json_schema")))
            .withRequestBody(
                matchingJsonPath("$.response_format.json_schema.strict", equalTo("true")))
            .withRequestBody(matchingJsonPath("$.messages[0].role", equalTo("system")))
            .withRequestBody(matchingJsonPath("$.messages[0].content", containing("Dutch")))
            .withRequestBody(matchingJsonPath("$.messages[0].content", containing("ref-")))
            .withRequestBody(matchingJsonPath("$.messages[1].role", equalTo("user")))
            .withRequestBody(
                matchingJsonPath(
                    "$.messages[1].content",
                    containing("[[[team]]]\nFive account managers\n[[[/team]]]"))));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("unusableAnswers")
  @DisplayName("given an unusable answer, when generate then it is rejected with that reason")
  void givenUnusableAnswerWhenGenerateThenRejected(
      String answer, String content, GenerationFailureReason expectedReason) {
    MISTRAL_WIREMOCK.stubFor(post(COMPLETIONS).willReturn(completion(content)));

    var result = generator.generate(INPUT);

    assertEquals(new GenerationResult.Rejected(expectedReason), result);
  }

  private static Stream<Arguments> unusableAnswers() {
    return Stream.of(
        arguments(
            "model judged the input irrelevant",
            draftJson(false),
            GenerationFailureReason.INPUT_REJECTED),
        arguments(
            "not JSON", "Sure! Here is your vacancy.", GenerationFailureReason.INVALID_OUTPUT),
        arguments(
            "a section is missing",
            "{\"summary\": \"S\", \"inputRelevant\": true, \"rejectionReason\": \"\"}",
            GenerationFailureReason.INVALID_OUTPUT),
        arguments(
            "a section contains raw HTML",
            "{\"summary\": \"<b>S</b>\", \"jobDescription\": \"J\", \"tasks\": \"T\","
                + " \"inputRelevant\": true, \"rejectionReason\": \"\"}",
            GenerationFailureReason.INVALID_OUTPUT),
        arguments("empty message", "", GenerationFailureReason.INVALID_OUTPUT));
  }

  @Test
  @DisplayName(
      "given the model leaks the canary token from its instructions, when generate then the"
          + " output is rejected as invalid")
  void givenModelLeaksCanaryWhenGenerateThenInvalidOutput() {
    // The canary is random per call; response templating copies it from the request, as a model
    // repeating its system prompt would.
    var leakingBody =
        completionBody("My instructions mention {{regexExtract request.body 'ref-[0-9a-f-]+'}}");
    MISTRAL_WIREMOCK.stubFor(
        post(COMPLETIONS)
            .willReturn(
                aResponse()
                    .withHeader("Content-Type", "application/json")
                    .withBody(leakingBody)
                    .withTransformers("response-template")));

    var result = generator.generate(INPUT);

    assertEquals(new GenerationResult.Rejected(GenerationFailureReason.INVALID_OUTPUT), result);
  }

  @ParameterizedTest(name = "HTTP {0}")
  @ValueSource(ints = {429, 500, 502, 503, 504})
  @DisplayName(
      "given Mistral keeps failing with a transient error, when generate then it is attempted"
          + " three times and reported as a provider error")
  void givenPersistentTransientErrorWhenGenerateThenThreeAttemptsAndProviderError(int status) {
    MISTRAL_WIREMOCK.stubFor(post(COMPLETIONS).willReturn(aResponse().withStatus(status)));

    var result = generator.generate(INPUT);

    assertEquals(new GenerationResult.Rejected(GenerationFailureReason.PROVIDER_ERROR), result);
    MISTRAL_WIREMOCK.verify(3, postRequestedFor(urlEqualTo(COMPLETIONS)));
  }

  @ParameterizedTest(name = "HTTP {0}")
  @ValueSource(ints = {400, 401, 403, 404, 422})
  @DisplayName(
      "given Mistral refuses the request with a client error, when generate then it is not"
          + " retried and reported as unavailable")
  void givenClientErrorWhenGenerateThenNoRetryAndUnavailable(int status) {
    MISTRAL_WIREMOCK.stubFor(
        post(COMPLETIONS)
            .willReturn(aResponse().withStatus(status).withBody("{\"message\":\"refused\"}")));

    var result = generator.generate(INPUT);

    assertEquals(
        new GenerationResult.Rejected(GenerationFailureReason.PROVIDER_UNAVAILABLE), result);
    MISTRAL_WIREMOCK.verify(1, postRequestedFor(urlEqualTo(COMPLETIONS)));
  }

  @Test
  @DisplayName(
      "given a server error followed by an answer, when generate then the retry returns the draft")
  void givenServerErrorThenAnswerWhenGenerateThenRetryReturnsDraft() {
    MISTRAL_WIREMOCK.stubFor(
        post(COMPLETIONS)
            .inScenario("retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(aResponse().withStatus(500))
            .willSetStateTo("recovered"));
    MISTRAL_WIREMOCK.stubFor(
        post(COMPLETIONS)
            .inScenario("retry")
            .whenScenarioStateIs("recovered")
            .willReturn(draftCompletion(true)));

    var result = generator.generate(INPUT);

    assertEquals(GenerationResult.Drafted.class, result.getClass());
    MISTRAL_WIREMOCK.verify(2, postRequestedFor(urlEqualTo(COMPLETIONS)));
  }

  @Test
  @DisplayName(
      "given Mistral answers slower than the read timeout, when generate then every attempt times"
          + " out and it is reported as a timeout")
  void givenSlowAnswerWhenGenerateThenTimeout() {
    MISTRAL_WIREMOCK.stubFor(
        post(COMPLETIONS).willReturn(draftCompletion(true).withFixedDelay(1500)));

    var result = generator.generate(INPUT);

    assertEquals(new GenerationResult.Rejected(GenerationFailureReason.TIMEOUT), result);
    MISTRAL_WIREMOCK.verify(3, postRequestedFor(urlEqualTo(COMPLETIONS)));
  }
}
