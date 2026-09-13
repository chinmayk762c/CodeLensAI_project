package com.codelens.backend.repository;

import com.codelens.backend.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findByReport_Id(Long reportId);
}