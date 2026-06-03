package com.agrgic.worktimetracker.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record TimesheetRequest(
        @Min(2000) int year,
        @Min(1) @Max(12) int month,
        @NotBlank String company,
        @NotBlank String consultantFirstName,
        @NotBlank String consultantLastName,
        String defaultProject,
        String approverFirstName,
        String approverLastName,
        @NotEmpty List<@Valid WorkEntry> entries
) {
}

