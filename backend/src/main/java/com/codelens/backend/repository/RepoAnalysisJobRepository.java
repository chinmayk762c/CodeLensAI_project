package com.codelens.backend.repository;

import com.codelens.backend.entity.RepoAnalysisJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepoAnalysisJobRepository extends JpaRepository<RepoAnalysisJob, Long> {
}