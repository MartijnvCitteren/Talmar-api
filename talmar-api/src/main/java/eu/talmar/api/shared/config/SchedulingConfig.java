package eu.talmar.api.shared.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables {@code @Scheduled} jobs, such as the purge of unconfirmed registrations and the
 * generation clean-up, unless {@code talmar.scheduling.enabled} is {@code false}. Tests switch it
 * off and call the jobs' use cases directly, so no background job touches test data. Every instance
 * runs the jobs, so a job must be safe to run on several instances at once.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    name = "talmar.scheduling.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class SchedulingConfig {}
