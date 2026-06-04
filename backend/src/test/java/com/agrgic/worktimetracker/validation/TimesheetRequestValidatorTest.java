package com.agrgic.worktimetracker.validation;

import com.agrgic.worktimetracker.model.TimesheetRequest;
import com.agrgic.worktimetracker.model.WorkEntry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimesheetRequestValidatorTest {
    private final TimesheetRequestValidator validator = new TimesheetRequestValidator();

    @Test
    void allowsExactlyThirtyNineHoursPerWeek() {
        TimesheetRequest request = request(List.of(
                entry("2026-06-01", "8.00"),
                entry("2026-06-02", "8.00"),
                entry("2026-06-03", "8.00"),
                entry("2026-06-04", "8.00"),
                entry("2026-06-05", "7.00")
        ));

        assertThatCode(() -> validator.validate(request)).doesNotThrowAnyException();
    }

    @Test
    void rejectsMoreThanThirtyNineHoursPerWeek() {
        TimesheetRequest request = request(List.of(
                entry("2026-06-01", "8.00"),
                entry("2026-06-02", "8.00"),
                entry("2026-06-03", "8.00"),
                entry("2026-06-04", "8.00"),
                entry("2026-06-05", "7.01")
        ));

        assertThatThrownBy(() -> validator.validate(request))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> {
                    ResponseStatusException exception = (ResponseStatusException) error;
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).contains("39 Wochenstunden");
                    assertThat(exception.getReason()).contains("Woche ab 01.06.2026: 39,01 h");
                });
    }

    private TimesheetRequest request(List<WorkEntry> entries) {
        return new TimesheetRequest(
                2026,
                6,
                "OG Consultancy Services GmbH",
                "ANTONIO",
                "GRGIC",
                "MOVAS",
                "",
                "",
                entries
        );
    }

    private WorkEntry entry(String date, String hours) {
        return new WorkEntry(LocalDate.parse(date), new BigDecimal(hours), "MOVAS");
    }
}
