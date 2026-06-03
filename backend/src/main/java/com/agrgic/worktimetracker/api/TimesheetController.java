package com.agrgic.worktimetracker.api;

import com.agrgic.worktimetracker.model.TimesheetRequest;
import com.agrgic.worktimetracker.pdf.TimesheetPdfService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/timesheets")
@CrossOrigin(origins = "http://localhost:5173")
public class TimesheetController {
    private final TimesheetPdfService pdfService;

    public TimesheetController(TimesheetPdfService pdfService) {
        this.pdfService = pdfService;
    }

    @PostMapping(value = "/pdf", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> createPdf(@Valid @RequestBody TimesheetRequest request) {
        byte[] pdf = pdfService.createPdf(request);
        String filename = "timesheet-%04d-%02d.pdf".formatted(request.year(), request.month());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
                .body(pdf);
    }
}

