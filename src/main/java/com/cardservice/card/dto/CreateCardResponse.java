package com.cardservice.card.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Confirmation that a card was registered")
public record CreateCardResponse(
        @Schema(description = "Generated UUID for the registered card",
                example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Timestamp when the card was stored (UTC)",
                example = "2026-05-09T10:00:00")
        LocalDateTime createdAt
) {}
