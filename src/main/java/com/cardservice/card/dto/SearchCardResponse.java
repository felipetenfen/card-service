package com.cardservice.card.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Result of a card lookup")
public record SearchCardResponse(
        @Schema(description = "`true` if the card is registered, `false` otherwise", example = "true")
        boolean found,

        @Schema(description = "UUID of the registered card — present only when `found` is true",
                example = "550e8400-e29b-41d4-a716-446655440000",
                nullable = true)
        UUID id
) {}
