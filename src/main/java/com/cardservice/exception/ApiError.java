package com.cardservice.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Standardized error response — never exposes stack traces or internal details")
public record ApiError(
        @Schema(description = "Machine-readable error code", example = "INVALID_CARD_NUMBER")
        String error,

        @Schema(description = "Human-readable error message", example = "Invalid card number")
        String message
) {}
