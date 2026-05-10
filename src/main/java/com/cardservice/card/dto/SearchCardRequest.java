package com.cardservice.card.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "Request to look up whether a card number is registered")
public record SearchCardRequest(
        @Schema(description = "Credit card number to look up — 13 to 19 digits, no spaces or dashes",
                example = "4532015112830366")
        @NotBlank
        @Pattern(regexp = "\\d{13,19}", message = "must contain 13 to 19 digits")
        String cardNumber
) {}
