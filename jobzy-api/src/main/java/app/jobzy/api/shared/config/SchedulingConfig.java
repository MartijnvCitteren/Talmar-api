package app.jobzy.api.shared.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables {@code @Scheduled} jobs, unless {@code jobzy.scheduling.enabled} is {@code false}. Tests
 * switch it off and call the jobs' use cases directly, so no background job touches test data.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(
    name = "jobzy.scheduling.enabled",
    havingValue = "true",
    matchIfMissing = true)
public class SchedulingConfig {}
