package com.codelens.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "repo_analysis_files")
@Getter
@Setter
@NoArgsConstructor
public class RepoAnalysisFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private RepoAnalysisJob job;

    @Column(nullable = false)
    private String filePath;

    private Long submissionId;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;
}