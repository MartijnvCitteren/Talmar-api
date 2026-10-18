package eu.talmar.api.shared.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
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
 * <p>CSRF protection, form login, HTTP basic and logout are disabled because the API is stateless
 * and will authenticate with bearer tokens, not cookies or sessions. CORS defers to the Spring MVC
 * configuration in {@link WebConfig}.
 *
 * <p>Only in a servlet web application: a filter chain has nothing to filter without one, and a
 * test that starts the context without a web environment has no {@code HttpSecurity} to build it.
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {

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
        .oauth2ResourceServer(resourceServer -> resourceServer.jwt(Customizer.withDefaults()))
        .build();
  }
}
