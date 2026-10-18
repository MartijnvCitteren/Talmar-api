package eu.talmar.api.shared.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.SecurityFilterChain;

/**
 * The security filter chain. The backend is an OAuth2 resource server for the access tokens
 * Keycloak issues (ADR 0006, section 6): it checks their signature with Keycloak's public keys, the
 * issuer and the expiry, without calling Keycloak per request.
 *
 * <p>Only the identity endpoints that act on the logged-in account require a token so far. Every
 * other request is still permitted; securing them and scoping them to a company is the next story
 * (#103). A request that does send a token is checked everywhere, so an expired or forged token is
 * a {@code 401} also on an open endpoint.
 *
 * <p>A {@code 401} carries the bearer challenge of RFC 6750 and nothing more: Spring's default
 * would also tell the caller why its token was rejected (see {@link #sendBearerChallenge}).
 *
 * <p>CSRF protection, form login, HTTP basic and logout are disabled because the API is stateless
 * and will authenticate with bearer tokens, not cookies or sessions. CORS defers to the Spring MVC
 * configuration in {@link WebConfig}.
 *
 * <p>Only in a servlet web application: a filter chain has nothing to filter without one, and a
 * test that starts the context without a web environment has no {@code HttpSecurity} to build it.
 */
@Log4j2
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {
  private static final String NO_TOKEN_CHALLENGE = "Bearer";
  private static final String REJECTED_TOKEN_CHALLENGE = "Bearer error=\"invalid_token\"";

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            requests ->
                requests.requestMatchers("/onboarding").authenticated().anyRequest().permitAll())
        .oauth2ResourceServer(
            resourceServer ->
                resourceServer
                    .jwt(Customizer.withDefaults())
                    .authenticationEntryPoint(SecurityConfig::sendBearerChallenge))
        .build();
  }

  /**
   * Answers {@code 401} with only what RFC 6750 requires: {@code error="invalid_token"} when a
   * token was sent, a plain {@code Bearer} when none was. Either way the client has to log in
   * again, so it needs no more. Spring's default adds an {@code error_description} that says why
   * the token was rejected (expired, signed with another key, malformed) in the words of the JWT
   * library, and a link to the resource metadata; that only helps someone probing with forged
   * tokens. The reason is logged instead.
   */
  private static void sendBearerChallenge(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) {
    if (exception instanceof OAuth2AuthenticationException) {
      log.warn("Rejected access token: {}", exception.getMessage());
      response.setHeader(HttpHeaders.WWW_AUTHENTICATE, REJECTED_TOKEN_CHALLENGE);
    } else {
      response.setHeader(HttpHeaders.WWW_AUTHENTICATE, NO_TOKEN_CHALLENGE);
    }
    response.setStatus(HttpStatus.UNAUTHORIZED.value());
  }
}
