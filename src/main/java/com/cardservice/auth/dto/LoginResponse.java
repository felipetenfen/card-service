package com.cardservice.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Successful authentication response")
public record LoginResponse(
        @Schema(description = "Signed JWT token to use as Bearer in subsequent requests",
                example = "eyJhbGciOiJIUzI1NiJ9...")
        String token,

        @Schema(description = "Token validity in seconds", example = "3600")
        long expiresIn
) {}
