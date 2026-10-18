package eu.talmar.api.shared.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  private final String[] allowedOrigins;

  public WebConfig(@Value("${talmar.cors.allowed-origins:}") String[] allowedOrigins) {
    // Defensive copy: keeping the caller's array would let it change our CORS config later
    // (SpotBugs EI_EXPOSE_REP2).
    this.allowedOrigins = allowedOrigins.clone();
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    if (allowedOrigins.length == 0) {
      return;
    }
    registry
        .addMapping("/**")
        .allowedOrigins(allowedOrigins)
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
  }
}
