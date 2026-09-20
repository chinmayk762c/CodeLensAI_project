package com.codelens.backend.repository;

import com.codelens.backend.entity.RepoAnalysisFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepoAnalysisFileRepository extends JpaRepository<RepoAnalysisFile, Long> {
    List<RepoAnalysisFile> findByJob_Id(Long jobId);
}