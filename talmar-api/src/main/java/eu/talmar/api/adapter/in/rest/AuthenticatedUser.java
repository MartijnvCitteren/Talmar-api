package eu.talmar.api.adapter.in.rest;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;

/** The account behind the current request, as the resource server authenticated it. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AuthenticatedUser {

  /**
   * The {@code sub} claim of the request's access token: the account's id at the identity provider.
   * Only for endpoints that require authentication; {@code SecurityConfig} guarantees a validated
   * token there.
   *
   * @throws IllegalStateException when the request is not authenticated: the endpoint is missing
   *     from {@code SecurityConfig}, which is our bug
   */
  public static String subject() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      throw new IllegalStateException("No authenticated user; the endpoint must require a token");
    }
    return authentication.getName();
  }
}
