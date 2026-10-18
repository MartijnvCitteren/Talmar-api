package eu.talmar.api.integration.generation;

import static eu.talmar.api.integration.IntegrationCommons.JSON_MAPPER;
import static io.restassured.RestAssured.given;

import eu.talmar.api.vacancy.adapter.in.web.contract.VacancyDescriptionGeneration;
import io.restassured.http.ContentType;
import java.util.UUID;
import org.springframework.http.HttpStatus;

/** Fixture data and requests shared by the generation integration tests. */
final class GenerationIntegrationCommons {
  /** Seeded by {@code sql/generation-vacancies.sql}: Sales Manager, Dutch. */
  static final UUID SALES_MANAGER_VACANCY_ID =
      UUID.fromString("0199a000-0000-7000-8000-000000000001");

  /** Seeded by {@code sql/generation-vacancies.sql}: Chef, English. */
  static final UUID CHEF_VACANCY_ID = UUID.fromString("0199a000-0000-7000-8000-000000000002");

  static final String SEED_SCRIPT = "/sql/generation-vacancies.sql";
  static final String PROBLEM_JSON = "application/problem+json";
  static final String MOST_IMPORTANT_TASKS = "Visiting customers and closing deals";
  static final String TEAM = "Five account managers and a sales lead";
  static final String WHY_NICE_JOB = "A lot of freedom and a great product";
  static final String VALID_REQUEST =
      """
      {"mostImportantTasks": "%s", "team": "%s", "whyNiceJob": "%s"}
      """
          .formatted(MOST_IMPORTANT_TASKS, TEAM, WHY_NICE_JOB);

  private GenerationIntegrationCommons() {}

  /** Starts a generation with valid answers and returns its id. */
  static UUID startGeneration(UUID vacancyId) {
    var response =
        given()
            .contentType(ContentType.JSON)
            .body(VALID_REQUEST)
            .when()
            .post("/vacancy/{id}/generate-description", vacancyId)
            .then()
            .statusCode(HttpStatus.ACCEPTED.value())
            .extract()
            .asString();
    return JSON_MAPPER.readValue(response, VacancyDescriptionGeneration.class).getGenerationId();
  }
}
