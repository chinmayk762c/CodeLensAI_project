package com.codelens.backend.repository;

import com.codelens.backend.entity.CodeSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CodeSubmissionRepository extends JpaRepository<CodeSubmission, Long> {
    List<CodeSubmission> findByUser_IdOrderByCreatedAtDesc(Long userId);
}