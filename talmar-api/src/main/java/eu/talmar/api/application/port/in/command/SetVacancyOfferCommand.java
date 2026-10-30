package eu.talmar.api.application.port.in.command;

import eu.talmar.api.domain.vacancy.valueobject.ContactPerson;
import eu.talmar.api.domain.vacancy.valueobject.Offer;
import java.util.UUID;
import lombok.Builder;

/** The contact person and offer to set on an existing vacancy, replacing what it had. */
@Builder
public record SetVacancyOfferCommand(UUID vacancyId, ContactPerson contactPerson, Offer offer) {}
