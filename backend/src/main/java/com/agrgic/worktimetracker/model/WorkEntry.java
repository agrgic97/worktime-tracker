package com.agrgic.worktimetracker.model;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WorkEntry(
        @NotNull LocalDate date,
        @DecimalMin(value = "0.0") BigDecimal hours,
        String project
) {
}

