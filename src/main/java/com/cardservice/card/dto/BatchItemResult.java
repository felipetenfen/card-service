package com.cardservice.card.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.cardservice.card.batch.BatchItemStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Processing result for a single card line within a batch")
public record BatchItemResult(
        @Schema(description = "Sequence number from the batch file", example = "000001")
        String sequence,

        @Schema(description = "`SUCCESS` if the card was inserted, `DUPLICATE` if already registered, `INVALID` if rejected",
                example = "SUCCESS")
        BatchItemStatus status,

        @Schema(description = "UUID assigned to the card — present only on SUCCESS",
                example = "550e8400-e29b-41d4-a716-446655440000", nullable = true)
        UUID id,

        @Schema(description = "Error code explaining why the card was rejected — present only on failure",
                example = "DUPLICATE_CARD", nullable = true)
        String reason
) {}
