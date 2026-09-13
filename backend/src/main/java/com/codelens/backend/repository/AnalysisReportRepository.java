package com.codelens.backend.repository;

import com.codelens.backend.entity.AnalysisReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, Long> {
    Optional<AnalysisReport> findTopBySubmission_IdOrderByCreatedAtDesc(Long submissionId);
}