package eu.talmar.api.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enables {@code @Async}. Spring Boot has no property for this; the annotation is the only switch.
 * The executor itself is configured in {@code application.yml}: it is Spring Boot's application
 * task executor, which runs on virtual threads because {@code spring.threads.virtual.enabled} is
 * set. Integration tests replace it with an executor that runs each task on its own thread and
 * waits for it, so asynchronous flows are tested without sleeping or polling.
 */
@Configuration
@EnableAsync
public class AsyncConfig {}
