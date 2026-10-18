package eu.talmar.api.domain.vacancy.valueobject;

/**
 * Vacancy description: five optional text fields plus the source (MANUAL/GENERATED). Plain data
 * holder with no validation logic — content-safety checks (raw HTML, control characters) are
 * performed at the REST adapter edge before this object is constructed.
 */
public record VacancyDescription(
    String summary,
    String jobDescription,
    String tasks,
    String whatWeOffer,
    String aboutUs,
    VacancyDescriptionSource source) {}
