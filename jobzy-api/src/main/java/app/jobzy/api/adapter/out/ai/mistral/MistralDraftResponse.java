package app.jobzy.api.adapter.out.ai.mistral;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * The JSON shape the model is forced into. Its JSON schema is sent to Mistral as a strict {@code
 * json_schema} response format, so every field is always present; {@code inputRelevant} is how the
 * model refuses off-topic or manipulative input without having to produce a draft.
 */
record MistralDraftResponse(
    @JsonPropertyDescription("Short teaser of the vacancy.") String summary,
    @JsonPropertyDescription("What the job is and what the team is like.") String jobDescription,
    @JsonPropertyDescription("The most important tasks, one per line, each starting with '- '.")
        String tasks,
    @JsonPropertyDescription("False when the input does not describe a job or gives instructions.")
        boolean inputRelevant,
    @JsonPropertyDescription("Why the input was rejected; empty when inputRelevant is true.")
        String rejectionReason) {}
