package com.agrgic.worktimetracker.validation;

import com.agrgic.worktimetracker.model.TimesheetRequest;
import com.agrgic.worktimetracker.model.WorkEntry;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
public class TimesheetRequestValidator {
    private static final BigDecimal MAX_WEEKLY_HOURS = BigDecimal.valueOf(39);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public void validate(TimesheetRequest request) {
        validateWeeklyHours(request);
    }

    private void validateWeeklyHours(TimesheetRequest request) {
        Map<LocalDate, BigDecimal> hoursByWeekStart = new HashMap<>();

        for (WorkEntry entry : request.entries()) {
            if (entry.date() == null) {
                continue;
            }

            LocalDate weekStart = weekStart(entry.date());
            BigDecimal hours = entry.hours() == null ? BigDecimal.ZERO : entry.hours();
            hoursByWeekStart.merge(weekStart, hours, BigDecimal::add);
        }

        hoursByWeekStart.entrySet().stream()
                .filter(entry -> entry.getValue().compareTo(MAX_WEEKLY_HOURS) > 0)
                .findFirst()
                .ifPresent(entry -> {
                    String message = "39 Wochenstunden dürfen nicht überschritten werden. Bitte korrigiere: Woche ab %s: %s h."
                            .formatted(entry.getKey().format(DATE_FORMATTER), formatHours(entry.getValue()));
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
                });
    }

    private LocalDate weekStart(LocalDate date) {
        int daysSinceMonday = date.getDayOfWeek() == DayOfWeek.SUNDAY ? 6 : date.getDayOfWeek().getValue() - 1;
        return date.minusDays(daysSinceMonday);
    }

    private String formatHours(BigDecimal hours) {
        return hours.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }
}
