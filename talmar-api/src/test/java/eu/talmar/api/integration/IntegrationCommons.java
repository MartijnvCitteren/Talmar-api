package eu.talmar.api.integration;

import tools.jackson.databind.json.JsonMapper;

public class IntegrationCommons {
  public static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
}
