package app.jobzy.api.shared.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Binds the registration settings, see {@link IdentityProperties}. */
@Configuration
@EnableConfigurationProperties(IdentityProperties.class)
public class IdentityConfig {}
