package eu.taldo.api.shared.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings of the AI description generation under {@code taldo.generation}.
 *
 * @param retention how long a generation and its draft are kept before the clean-up deletes them
 */
@ConfigurationProperties("taldo.generation")
public record GenerationProperties(@DefaultValue("PT32H") Duration retention) {}
