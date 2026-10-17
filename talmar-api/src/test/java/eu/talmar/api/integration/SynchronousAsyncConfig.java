package eu.talmar.api.integration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskExecutor;

/**
 * Replaces Spring Boot's application task executor with one that runs each {@code @Async} task on
 * its own virtual thread and waits for it. A started description generation is then completed
 * before the {@code POST} returns, so tests can assert on the outcome without sleeping or polling.
 *
 * <p>A new thread rather than the calling thread on purpose: the task then starts without the
 * request's (already committed) transaction bound to it, exactly as in production, so the
 * completion code needs no transaction workarounds for the tests. Spring Boot backs off from
 * creating its own executor when this bean exists.
 */
@TestConfiguration(proxyBeanMethods = false)
public class SynchronousAsyncConfig {

  @Bean(name = {"applicationTaskExecutor", "taskExecutor"})
  TaskExecutor applicationTaskExecutor() {
    return task -> {
      var worker = Thread.ofVirtual().start(task);
      try {
        worker.join();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IllegalStateException("Interrupted while waiting for an async task", e);
      }
    };
  }
}
