package eu.talmar.api.shared.gdpr;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a persisted field as process data (events, channel source, rejection reason, audit
 * timestamps, ...): it identifies no person, so it survives anonymization and keeps feeding the
 * advice engine. Every persistence-entity field carries either this or {@link PersonalData},
 * enforced by {@code ArchitectureTest}.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ProcessData {}
