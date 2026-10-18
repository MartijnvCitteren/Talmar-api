package eu.talmar.api.domain.vacancy.valueobject;

/**
 * Language the texts of a vacancy are written in (ISO 639-1). Restricted to the markets Talmar
 * serves; AI generation writes the draft in this language.
 */
public enum Language {
  NL,
  EN,
  DE,
  FR
}
