package eu.talmar.api.shared.config;

import eu.talmar.api.shared.Constants;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The single source of the current time. Code asks this {@link Clock} for "now" instead of calling
 * {@code now()} directly, so tests can pin time with {@link Clock#fixed}.
 */
@Configuration
public class ClockConfig {

  @Bean
  public Clock clock() {
    return Clock.system(ZoneId.of(Constants.AMS_TIME_ZONE_ID));
  }
}
