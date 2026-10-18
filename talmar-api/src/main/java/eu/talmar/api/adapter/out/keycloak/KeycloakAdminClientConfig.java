package eu.talmar.api.adapter.out.keycloak;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.RestClientClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.core.http.converter.OAuth2AccessTokenResponseHttpMessageConverter;
import org.springframework.web.client.RestClient;

/**
 * Wires {@link KeycloakIdentityProvider} with a {@link RestClient} for the realm's Admin REST API
 * that carries the {@code talmar-backend} service-account token (client credentials grant).
 *
 * <p>The token is fetched on the first call and reused until it expires: one token for the whole
 * backend, held in Spring Boot's {@link OAuth2AuthorizedClientService} under a fixed principal. The
 * default would hold it under the logged-in user of the request, so every user would get their own
 * copy of the service-account token. When Keycloak rejects a token, it is dropped and the next call
 * fetches a new one.
 *
 * <p>Both clients, the Admin API one and the one that fetches the token, are built from Spring
 * Boot's builder, so they get the {@code spring.http.clients} timeouts. Spring Security's own token
 * client has none and would wait forever for a Keycloak that hangs.
 */
@Configuration
@EnableConfigurationProperties(KeycloakProperties.class)
class KeycloakAdminClientConfig {
  static final String REGISTRATION_ID = "keycloak-admin";

  private static final Authentication SERVICE_ACCOUNT =
      new AnonymousAuthenticationToken(
          REGISTRATION_ID, REGISTRATION_ID, AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

  @Bean
  KeycloakIdentityProvider keycloakIdentityProvider(
      RestClient.Builder restClientBuilder,
      KeycloakProperties properties,
      ClientRegistrationRepository clientRegistrations,
      OAuth2AuthorizedClientService tokens) {
    var restClient =
        restClientBuilder
            .clone()
            .baseUrl(properties.adminApiUrl())
            .requestInterceptor(serviceAccountToken(restClientBuilder, clientRegistrations, tokens))
            .build();
    return new KeycloakIdentityProvider(restClient);
  }

  private static OAuth2ClientHttpRequestInterceptor serviceAccountToken(
      RestClient.Builder restClientBuilder,
      ClientRegistrationRepository clientRegistrations,
      OAuth2AuthorizedClientService tokens) {
    var tokenProvider = new ClientCredentialsOAuth2AuthorizedClientProvider();
    tokenProvider.setAccessTokenResponseClient(tokenResponseClient(restClientBuilder));
    var tokenManager =
        new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrations, tokens);
    tokenManager.setAuthorizedClientProvider(tokenProvider);

    var interceptor = new OAuth2ClientHttpRequestInterceptor(tokenManager);
    interceptor.setClientRegistrationIdResolver(request -> REGISTRATION_ID);
    interceptor.setPrincipalResolver(request -> SERVICE_ACCOUNT);
    interceptor.setAuthorizationFailureHandler(
        OAuth2ClientHttpRequestInterceptor.authorizationFailureHandler(tokens));
    return interceptor;
  }

  /** Spring Security's token client, with the converters and error handler it uses by default. */
  private static RestClientClientCredentialsTokenResponseClient tokenResponseClient(
      RestClient.Builder restClientBuilder) {
    var tokenClient = new RestClientClientCredentialsTokenResponseClient();
    tokenClient.setRestClient(
        restClientBuilder
            .clone()
            .configureMessageConverters(
                converters -> {
                  converters.addCustomConverter(new FormHttpMessageConverter());
                  converters.addCustomConverter(
                      new OAuth2AccessTokenResponseHttpMessageConverter());
                })
            .defaultStatusHandler(new OAuth2ErrorResponseErrorHandler())
            .build());
    return tokenClient;
  }
}
