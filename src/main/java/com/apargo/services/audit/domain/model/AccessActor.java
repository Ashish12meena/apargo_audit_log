package com.apargo.services.audit.domain.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Actor of an access event: the contract's {@code AccessActorDto} ({@code type, id, name}) plus the
 * optional schema-doc fields {@code email} (masked) and {@code impersonatorId}. {@code type} is a
 * string so values such as {@code API_KEY} or {@code UNKNOWN} are accepted.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AccessActor(String type, String id, String name, String email, String impersonatorId) {
}
