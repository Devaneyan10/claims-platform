package com.example.claims.service;

import com.example.claims.domain.ClaimType;
import com.example.claims.domain.Market;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Command to report a new incident. */
public record SubmitClaim(
        @NotBlank @Size(max = 200) String claimantName,
        @NotNull Market market,
        @NotNull ClaimType type,
        @NotBlank @Size(max = 2000) String description,
        @NotNull @PastOrPresent LocalDate incidentDate,
        @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal estimatedAmount) { }
