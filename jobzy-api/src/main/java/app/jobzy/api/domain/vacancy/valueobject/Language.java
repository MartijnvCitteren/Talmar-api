package app.jobzy.api.domain.vacancy.valueobject;

/**
 * Language the texts of a vacancy are written in (ISO 639-1). Restricted to the markets Jobzy
 * serves; AI generation writes the draft in this language.
 */
public enum Language {
  NL,
  EN,
  DE,
  FR
}
