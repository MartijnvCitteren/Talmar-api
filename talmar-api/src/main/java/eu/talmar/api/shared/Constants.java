package eu.talmar.api.shared;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Constants {
  public static final String AMS_TIME_ZONE_ID = "Europe/Amsterdam";

  public static final String VALIDATION_FAILED_TITLE = "Validation failed";
  public static final String VALIDATION_FAILED_DETAIL =
      "The request failed validation on one or more fields, see 'errors' for details.";
  public static final String UNREADABLE_REQUEST_DETAIL =
      "The request body is not valid JSON or contains a value the contract does not allow.";
  public static final String NOT_FOUND_TITLE = "Not found";
  public static final String NOT_IMPLEMENTED_TITLE = "Not implemented";
  public static final String GENERATION_FAILED_TITLE = "Description generation failed";
  public static final String GENERATION_REJECTED_DETAIL =
      "The answers were not recognised as a description of this job. Adjust them and start a new"
          + " generation.";
  public static final String GENERATION_FAILED_DETAIL =
      "The description could not be generated. Start a new generation.";
  public static final String GENERATION_UNAVAILABLE_TITLE = "Description generation unavailable";
  public static final String GENERATION_UNAVAILABLE_DETAIL =
      "AI description generation is not available right now. Write the description yourself or"
          + " try again later.";
  public static final String INTERNAL_SERVER_ERROR_TITLE = "Internal server error";
  public static final String INTERNAL_SERVER_ERROR_DETAIL =
      "An unexpected error occurred while processing the request";
}
