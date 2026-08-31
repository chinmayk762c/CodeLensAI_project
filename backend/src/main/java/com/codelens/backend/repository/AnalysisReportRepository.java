package com.codelens.backend.repository;

import com.codelens.backend.entity.AnalysisReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisReportRepository extends JpaRepository<AnalysisReport, Long> {
}