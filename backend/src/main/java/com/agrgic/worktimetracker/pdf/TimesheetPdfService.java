package com.agrgic.worktimetracker.pdf;

import com.agrgic.worktimetracker.model.TimesheetRequest;
import com.agrgic.worktimetracker.model.WorkEntry;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class TimesheetPdfService {
    private static final Locale DE = Locale.GERMANY;
    private static final PDFont REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont OBLIQUE = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final float LEFT = 76;
    private static final float TABLE_WIDTH = 443;
    private static final float ORANGE_R = 1.0f;
    private static final float ORANGE_G = 0.45f;
    private static final float ORANGE_B = 0.0f;
    private static final float WEEKEND_GREY = 0.72f;
    private static final float WORK_GREY = 0.84f;

    public byte[] createPdf(TimesheetRequest request) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                drawDocument(cs, request);
            }

            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not create timesheet PDF", e);
        }
    }

    private void drawDocument(PDPageContentStream cs, TimesheetRequest request) throws IOException {
        YearMonth month = YearMonth.of(request.year(), request.month());
        Map<LocalDate, WorkEntry> entries = entriesByDate(request.entries());

        text(cs, "DB Intern / DB internal", LEFT + 5, y(32), 10, REGULAR, 1, 0, 0);

        float top = y(63);
        drawMainHeader(cs, request, month, top);
        float dayHeaderBottom = top - 127;
        float rowHeight = 13;
        BigDecimal total = drawDayRows(cs, request, month, entries, dayHeaderBottom - rowHeight);
        float sumTop = dayHeaderBottom - (month.lengthOfMonth() * rowHeight);
        drawSumRow(cs, sumTop, total);
        drawSignatureBlocks(cs, request, sumTop - 27);
    }

    private void drawMainHeader(PDPageContentStream cs, TimesheetRequest request, YearMonth month, float top) throws IOException {
        strokeRect(cs, LEFT, top - 34, TABLE_WIDTH, 34, 1.2f);
        textCentered(cs, "Berater/Consultant Abrechnung", LEFT, top - 25, TABLE_WIDTH, 12, BOLD, ORANGE_R, ORANGE_G, ORANGE_B);

        float rowY = top - 54;
        float[] widths = {54, 94, 128, 167};
        drawRowGrid(cs, LEFT, rowY, widths, 20, 1.0f);
        text(cs, "Monat", LEFT + 2, rowY + 7, 9, BOLD, ORANGE_R, ORANGE_G, ORANGE_B);
        text(cs, month.format(DateTimeFormatter.ofPattern("MMM yy", DE)), LEFT + widths[0] + 3, rowY + 7, 9, BOLD, ORANGE_R, ORANGE_G, ORANGE_B);
        text(cs, "Firma", LEFT + widths[0] + widths[1] + 3, rowY + 7, 9, BOLD, ORANGE_R, ORANGE_G, ORANGE_B);
        text(cs, request.company(), LEFT + widths[0] + widths[1] + widths[2] + 3, rowY + 7, 9, BOLD, ORANGE_R, ORANGE_G, ORANGE_B);

        rowY -= 14;
        drawRowGrid(cs, LEFT, rowY, new float[]{148, 295}, 14, 0.8f);
        text(cs, "Vorname", LEFT + 2, rowY + 5, 8.5f, BOLD, 0.25f, 0.25f, 0.25f);
        text(cs, "Nachname", LEFT + 151, rowY + 5, 8.5f, BOLD, 0.25f, 0.25f, 0.25f);

        rowY -= 20;
        drawRowGrid(cs, LEFT, rowY, new float[]{148, 295}, 20, 1.0f);
        text(cs, request.consultantFirstName().toUpperCase(DE), LEFT + 2, rowY + 8, 9.5f, BOLD, 0.25f, 0.25f, 0.25f);
        text(cs, request.consultantLastName().toUpperCase(DE), LEFT + 151, rowY + 8, 9.5f, BOLD, 0.25f, 0.25f, 0.25f);

        rowY -= 39;
        drawRowGrid(cs, LEFT, rowY, new float[]{54, 94, 128, 167}, 39, 1.0f);
        text(cs, "Datum", LEFT + 2, rowY + 17, 9, BOLD, ORANGE_R, ORANGE_G, ORANGE_B);
        textCentered(cs, "Tag", LEFT + 54, rowY + 17, 94, 9, BOLD, ORANGE_R, ORANGE_G, ORANGE_B);
        textCentered(cs, "Stunden", LEFT + 148, rowY + 17, 128, 9, BOLD, ORANGE_R, ORANGE_G, ORANGE_B);
        text(cs, "Projekt / Auftraggeber", LEFT + 279, rowY + 17, 9, BOLD, ORANGE_R, ORANGE_G, ORANGE_B);
    }

    private BigDecimal drawDayRows(PDPageContentStream cs, TimesheetRequest request, YearMonth month, Map<LocalDate, WorkEntry> entries, float startY) throws IOException {
        BigDecimal total = BigDecimal.ZERO;
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
        float rowHeight = 13;
        float[] widths = {54, 94, 128, 167};

        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            WorkEntry entry = entries.get(date);
            BigDecimal hours = entry == null || entry.hours() == null ? BigDecimal.ZERO : entry.hours();
            String project = entry == null || blank(entry.project()) ? request.defaultProject() : entry.project();
            boolean weekend = date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY;
            boolean worked = hours.compareTo(BigDecimal.ZERO) > 0;
            float rowY = startY - ((day - 1) * rowHeight);

            if (weekend) {
                fillRect(cs, LEFT + widths[0], rowY, widths[1], rowHeight, WEEKEND_GREY);
            }
            if (worked) {
                fillRect(cs, LEFT + widths[0] + widths[1], rowY, widths[2], rowHeight, WORK_GREY);
                fillRect(cs, LEFT + widths[0] + widths[1] + widths[2], rowY, widths[3], rowHeight, WORK_GREY);
                total = total.add(hours);
            }

            drawRowGrid(cs, LEFT, rowY, widths, rowHeight, 0.7f);
            text(cs, date.format(dateFormatter), LEFT + 5, rowY + 4, 8.5f, REGULAR, 0, 0, 0);
            textCentered(cs, dayLabel(date.getDayOfWeek()), LEFT + widths[0], rowY + 4, widths[1], 8.5f, REGULAR, 0, 0, 0);
            if (worked) {
                textCentered(cs, formatHours(hours), LEFT + widths[0] + widths[1], rowY + 4, widths[2], 8.5f, REGULAR, 0, 0, 0);
                text(cs, project, LEFT + widths[0] + widths[1] + widths[2] + 2, rowY + 4, 8.5f, REGULAR, 0, 0, 0);
            }
        }

        return total;
    }

    private void drawSumRow(PDPageContentStream cs, float y, BigDecimal total) throws IOException {
        strokeRect(cs, LEFT, y - 27, TABLE_WIDTH, 27, 1.2f);
        line(cs, LEFT + 148, y, LEFT + 148, y - 27, 1.2f);
        textCentered(cs, "Summe", LEFT, y - 18, 148, 9, BOLD, ORANGE_R, ORANGE_G, ORANGE_B);
        textCentered(cs, formatHours(total), LEFT + 148, y - 18, 295, 12, BOLD, ORANGE_R, ORANGE_G, ORANGE_B);
    }

    private void drawSignatureBlocks(PDPageContentStream cs, TimesheetRequest request, float sumBottom) throws IOException {
        float blockWidth = TABLE_WIDTH;
        float blockHeight = 70;
        float firstY = sumBottom - 16 - blockHeight;
        float secondY = firstY - 26 - blockHeight;

        drawSignatureBlock(cs, firstY, blockWidth, blockHeight, "Berater", request.consultantFirstName(), request.consultantLastName(), false);
        drawSignatureBlock(cs, secondY, blockWidth, blockHeight, "Genehmiger", safe(request.approverFirstName()), safe(request.approverLastName()), true);
    }

    private void drawSignatureBlock(PDPageContentStream cs, float bottom, float width, float height, String role, String firstName, String lastName, boolean roleOrange) throws IOException {
        strokeRect(cs, LEFT, bottom, width, height, 1.2f);
        line(cs, LEFT + 54, bottom, LEFT + 54, bottom + height, 1.0f);
        line(cs, LEFT + 148, bottom, LEFT + 148, bottom + height, 1.0f);
        line(cs, LEFT + 276, bottom + 50, LEFT + width, bottom + 50, 1.0f);
        line(cs, LEFT + 276, bottom + 38, LEFT + width, bottom + 38, 1.0f);
        line(cs, LEFT + 276, bottom + 50, LEFT + 276, bottom + height, 1.0f);

        textCentered(cs, LocalDate.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), LEFT, bottom + 35, 54, 8.5f, REGULAR, 0, 0, 0);
        float r = roleOrange ? ORANGE_R : 0;
        float g = roleOrange ? ORANGE_G : 0;
        float b = roleOrange ? ORANGE_B : 0;
        textCentered(cs, role, LEFT + 54, bottom + 45, 94, 9, OBLIQUE, r, g, b);
        textCentered(cs, "Unterschrift", LEFT + 54, bottom + 31, 94, 9, OBLIQUE, r, g, b);

        text(cs, "Vorname", LEFT + 150, bottom + 58, 8.5f, REGULAR, 0, 0, 0);
        text(cs, "Nachname", LEFT + 279, bottom + 58, 8.5f, REGULAR, 0, 0, 0);
        text(cs, firstName, LEFT + 150, bottom + 39, 8.5f, REGULAR, 0, 0, 0);
        text(cs, lastName, LEFT + 279, bottom + 39, 8.5f, REGULAR, 0, 0, 0);
        // Lower signature area intentionally stays blank.
    }

    private Map<LocalDate, WorkEntry> entriesByDate(List<WorkEntry> entries) {
        Map<LocalDate, WorkEntry> result = new HashMap<>();
        entries.stream()
                .sorted(Comparator.comparing(WorkEntry::date))
                .forEach(entry -> result.put(entry.date(), entry));
        return result;
    }

    private void drawRowGrid(PDPageContentStream cs, float x, float y, float[] widths, float height, float lineWidth) throws IOException {
        float total = 0;
        for (float width : widths) {
            total += width;
        }
        strokeRect(cs, x, y, total, height, lineWidth);
        float current = x;
        for (int i = 0; i < widths.length - 1; i++) {
            current += widths[i];
            line(cs, current, y, current, y + height, lineWidth);
        }
    }

    private void text(PDPageContentStream cs, String value, float x, float y, float size, PDFont font, float r, float g, float b) throws IOException {
        cs.beginText();
        cs.setNonStrokingColor(r, g, b);
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(safe(value));
        cs.endText();
        cs.setNonStrokingColor(0, 0, 0);
    }

    private void textCentered(PDPageContentStream cs, String value, float x, float y, float width, float size, PDFont font, float r, float g, float b) throws IOException {
        String safeValue = safe(value);
        float textWidth = font.getStringWidth(safeValue) / 1000 * size;
        text(cs, safeValue, x + ((width - textWidth) / 2), y, size, font, r, g, b);
    }

    private void strokeRect(PDPageContentStream cs, float x, float y, float w, float h, float lineWidth) throws IOException {
        cs.setLineWidth(lineWidth);
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    private void fillRect(PDPageContentStream cs, float x, float y, float w, float h, float grey) throws IOException {
        cs.setNonStrokingColor(grey, grey, grey);
        cs.addRect(x, y, w, h);
        cs.fill();
        cs.setNonStrokingColor(0, 0, 0);
    }

    private void line(PDPageContentStream cs, float x1, float y1, float x2, float y2, float lineWidth) throws IOException {
        cs.setLineWidth(lineWidth);
        cs.moveTo(x1, y1);
        cs.lineTo(x2, y2);
        cs.stroke();
    }

    private String formatHours(BigDecimal hours) {
        return hours.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private String dayLabel(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "Mo";
            case TUESDAY -> "Di";
            case WEDNESDAY -> "Mi";
            case THURSDAY -> "Do";
            case FRIDAY -> "Fr";
            case SATURDAY -> "Sa";
            case SUNDAY -> "So";
        };
    }

    private float y(float fromTop) {
        return PAGE_HEIGHT - fromTop;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
