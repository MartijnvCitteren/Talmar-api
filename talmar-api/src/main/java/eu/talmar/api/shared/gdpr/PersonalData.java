package eu.talmar.api.shared.gdpr;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a persisted field as personal data under the GDPR (name, CV, email, ...): anonymization
 * wipes it. Every persistence-entity field carries either this or {@link ProcessData}, enforced by
 * {@code ArchitectureTest}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PersonalData {}
