package eu.talmar.api.application.port.in.command;

import eu.talmar.api.domain.identity.valueobject.JobRole;

/**
 * Onboards the logged-in account.
 *
 * @param subject the logged-in account's id at the identity provider, from its access token
 * @param jobRole the role the user picked; a profile field, not a right
 */
public record OnboardUserCommand(String subject, JobRole jobRole) {}
