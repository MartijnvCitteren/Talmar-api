package eu.talmar.api.adapter.out.persistence;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing so {@code @CreatedDate}/{@code @LastModifiedDate} fields on
 * {@link BaseJpaEntity} are populated automatically.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}
