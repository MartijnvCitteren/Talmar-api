package app.jobzy.api.shared.config;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings for registration ({@code jobzy.identity.*}).
 *
 * @param confirmationUrl the frontend page the verification mail links to; the token is appended as
 *     the {@code token} query parameter
 * @param unconfirmedRetention how long a verification link stays valid, and so how long an
 *     unconfirmed registration is kept before the purge deletes it
 */
@ConfigurationProperties(prefix = "jobzy.identity")
public record IdentityProperties(
    URI confirmationUrl, @DefaultValue("PT48H") Duration unconfirmedRetention) {

  public IdentityProperties {
    Objects.requireNonNull(confirmationUrl, "jobzy.identity.confirmation-url is required");
    Objects.requireNonNull(
        unconfirmedRetention, "jobzy.identity.unconfirmed-retention is required");
  }
}
