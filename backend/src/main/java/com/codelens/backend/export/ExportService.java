package com.codelens.backend.export;

import com.codelens.backend.dto.AnalysisReportResponse;
import com.codelens.backend.dto.IssueResponse;
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
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class ExportService {

    private static final float MARGIN = 50;
    private static final float LINE_HEIGHT = 16;
    private static final float PAGE_WIDTH = PDRectangle.A4.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.A4.getHeight();
    private static final int WRAP_CHARS = 95;

    public String toCsv(AnalysisReportResponse report) {
        StringBuilder sb = new StringBuilder();
        sb.append("Submission ID,Score,Quality Gate,Coverage %,Created At\n");
        sb.append(csvEscape(String.valueOf(report.submissionId()))).append(",");
        sb.append(csvEscape(report.overallScore() == null ? "" : String.valueOf(report.overallScore()))).append(",");
        sb.append(csvEscape(report.qualityGatePassed() ? "PASSED" : "FAILED")).append(",");
        sb.append(csvEscape(report.coveragePercentage() == null ? "" : String.valueOf(report.coveragePercentage()))).append(",");
        sb.append(csvEscape(String.valueOf(report.createdAt()))).append("\n\n");

        sb.append("Severity,Category,Description,Line\n");
        for (IssueResponse issue : report.issues()) {
            sb.append(csvEscape(issue.severity().toString())).append(",");
            sb.append(csvEscape(issue.category().toString())).append(",");
            sb.append(csvEscape(issue.description())).append(",");
            sb.append(csvEscape(issue.lineNumber() == null ? "" : String.valueOf(issue.lineNumber()))).append("\n");
        }
        return sb.toString();
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    public byte[] toPdf(AnalysisReportResponse report) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDFont regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            PdfCursor cursor = new PdfCursor(doc);

            cursor.writeLine("CodeLens AI - Analysis Report", boldFont, 16);
            cursor.newLine(6);
            cursor.writeLine("Submission #" + report.submissionId(), regularFont, 11);
            cursor.writeLine("Generated: " + report.createdAt(), regularFont, 11);
            cursor.newLine(10);

            cursor.writeLine("Score: " + (report.overallScore() == null ? "N/A" : report.overallScore() + "/100"), boldFont, 13);
            cursor.writeLine("Quality Gate: " + (report.qualityGatePassed() ? "PASSED" : "FAILED"), regularFont, 11);
            if (report.coveragePercentage() != null) {
                cursor.writeLine(String.format("Test Coverage: %.1f%%", report.coveragePercentage()), regularFont, 11);
            }
            cursor.newLine(10);

            if (report.aiSummary() != null) {
                cursor.writeLine("AI Summary", boldFont, 13);
                cursor.writeWrapped(report.aiSummary(), regularFont, 10);
                cursor.newLine(10);
            }

            cursor.writeLine("Issues (" + report.issues().size() + ")", boldFont, 13);
            cursor.newLine(4);
            for (IssueResponse issue : report.issues()) {
                String line = "[" + issue.severity() + "/" + issue.category() + "] " + issue.description();
                cursor.writeWrapped(line, regularFont, 9);
            }

            cursor.close();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private static class PdfCursor {
        private final PDDocument doc;
        private PDPageContentStream stream;
        private float y;

        PdfCursor(PDDocument doc) throws IOException {
            this.doc = doc;
            newPage();
        }

        private void newPage() throws IOException {
            if (stream != null) stream.close();
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            stream = new PDPageContentStream(doc, page);
            y = PAGE_HEIGHT - MARGIN;
        }

        private void ensureSpace() throws IOException {
            if (y < MARGIN) newPage();
        }

        void newLine(float extra) {
            y -= extra;
        }

        void writeLine(String text, PDFont font, float size) throws IOException {
            ensureSpace();
            stream.beginText();
            stream.setFont(font, size);
            stream.newLineAtOffset(MARGIN, y);
            stream.showText(sanitize(text));
            stream.endText();
            y -= LINE_HEIGHT;
        }

        void writeWrapped(String text, PDFont font, float size) throws IOException {
            for (String line : wrap(text, WRAP_CHARS)) {
                writeLine(line, font, size);
            }
        }

        void close() throws IOException {
            if (stream != null) stream.close();
        }

        private String sanitize(String text) {
            return text.replaceAll("[^\\x20-\\x7E]", "?");
        }

        private List<String> wrap(String text, int maxChars) {
            List<String> lines = new java.util.ArrayList<>();
            String[] words = text.split("\\s+");
            StringBuilder current = new StringBuilder();
            for (String word : words) {
                if (current.length() + word.length() + 1 > maxChars) {
                    lines.add(current.toString());
                    current = new StringBuilder();
                }
                if (!current.isEmpty()) current.append(" ");
                current.append(word);
            }
            if (!current.isEmpty()) lines.add(current.toString());
            return lines;
        }
    }
}