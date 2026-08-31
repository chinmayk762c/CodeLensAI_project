package com.codelens.backend.repository;

import com.codelens.backend.entity.CodeSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodeSubmissionRepository extends JpaRepository<CodeSubmission, Long> {
}