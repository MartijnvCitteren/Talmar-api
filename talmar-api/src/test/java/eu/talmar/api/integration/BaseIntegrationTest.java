package eu.talmar.api.integration;

import static eu.talmar.api.testSupport.MistralStub.MISTRAL_WIREMOCK;

import io.restassured.RestAssured;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for RestAssured integration tests: boots the application on a random port against the
 * in-memory H2 database of the {@code test} profile and points RestAssured at it.
 *
 * <p>Everything inside the application is real. Only the world outside it is replaced: Mistral by
 * WireMock ({@code MistralStub}), so no test ever calls an AI provider. {@code @Async} work runs to
 * completion before the request returns ({@link SynchronousAsyncConfig}). Declared here rather than
 * per test class so every integration test shares one application context.
 */
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(SynchronousAsyncConfig.class)
public abstract class BaseIntegrationTest {

  @LocalServerPort protected int port;

  @Value("${server.servlet.context-path}")
  private String contextPath;

  @DynamicPropertySource
  static void mistralProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.ai.mistralai.chat.base-url", MISTRAL_WIREMOCK::baseUrl);
  }

  @BeforeEach
  void configureRestAssured() {
    RestAssured.port = port;
    RestAssured.basePath = contextPath;
  }

  @AfterEach
  void resetRestAssured() {
    RestAssured.reset();
  }
}
