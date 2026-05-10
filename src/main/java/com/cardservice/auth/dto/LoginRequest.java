package com.cardservice.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credentials for authentication")
public record LoginRequest(
        @Schema(description = "Account username", example = "admin")
        @NotBlank String username,

        @Schema(description = "Account password", example = "ChangeMe!2026")
        @NotBlank String password
) {}
