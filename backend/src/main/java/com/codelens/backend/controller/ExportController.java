package com.codelens.backend.controller;

import com.codelens.backend.dto.AnalysisReportResponse;
import com.codelens.backend.export.ExportService;
import com.codelens.backend.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class ExportController {

    private final AnalysisService analysisService;
    private final ExportService exportService;

    @GetMapping("/{id}/export/csv")
    public ResponseEntity<?> exportCsv(@PathVariable Long id, Authentication authentication) {
        try {
            AnalysisReportResponse report = analysisService.getLatestReport(authentication.getName(), id);
            String csv = exportService.toCsv(report);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename("report-" + id + ".csv").build().toString())
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(csv.getBytes(StandardCharsets.UTF_8));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/export/pdf")
    public ResponseEntity<?> exportPdf(@PathVariable Long id, Authentication authentication) {
        try {
            AnalysisReportResponse report = analysisService.getLatestReport(authentication.getName(), id);
            byte[] pdf = exportService.toPdf(report);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            ContentDisposition.attachment().filename("report-" + id + ".pdf").build().toString())
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdf);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "PDF generation failed: " + e.getMessage()));
        }
    }
}