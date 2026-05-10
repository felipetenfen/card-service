package com.cardservice.card.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Summary of a batch file processing operation")
public record BatchUploadResponse(
        @Schema(description = "Batch ID extracted from the file header", example = "LOTE0001")
        String batchId,

        @Schema(description = "Number of card records declared in the file header", example = "10")
        int totalDeclared,

        @Schema(description = "Number of cards successfully inserted", example = "9")
        int totalInserted,

        @Schema(description = "Number of cards rejected (duplicate or invalid)", example = "1")
        int totalRejected,

        @Schema(description = "Per-line results — one entry per card line in the file")
        List<BatchItemResult> results
) {}
