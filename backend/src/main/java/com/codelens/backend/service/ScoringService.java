package com.codelens.backend.service;

import com.codelens.backend.entity.Issue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScoringService {

    @Value("${codelens.quality-gate.threshold:70}")
    private int qualityGateThreshold;

    public int calculateScore(List<Issue> issues) {
        double score = 100;
        for (Issue issue : issues) {
            score -= deductionFor(issue);
        }
        return Math.max((int) Math.round(score), 0);
    }

    private double deductionFor(Issue issue) {
        // Style/cosmetic issues barely move the score — correctness and efficiency matter far more.
        if (issue.getCategory() == com.codelens.backend.entity.IssueCategory.STYLE) {
            return 0.5;
        }
        if (issue.getCategory() == com.codelens.backend.entity.IssueCategory.CODE_SMELL) {
            return switch (issue.getSeverity()) {
                case CRITICAL -> 6;
                case HIGH -> 4;
                case MEDIUM -> 2;
                case LOW -> 1;
            };
        }
        if (issue.getCategory() == com.codelens.backend.entity.IssueCategory.PERFORMANCE) {
            return switch (issue.getSeverity()) {
                case CRITICAL -> 15;
                case HIGH -> 10;
                case MEDIUM -> 6;
                case LOW -> 3;
            };
        }
        if (issue.getCategory() == com.codelens.backend.entity.IssueCategory.SECURITY) {
            return switch (issue.getSeverity()) {
                case CRITICAL -> 25;
                case HIGH -> 15;
                case MEDIUM -> 8;
                case LOW -> 4;
            };
        }
        // BUG — the category that matters most: does the code actually work correctly?
        return switch (issue.getSeverity()) {
            case CRITICAL -> 25;
            case HIGH -> 15;
            case MEDIUM -> 7;
            case LOW -> 3;
        };
    }

    public boolean passesQualityGate(int score) {
        return score >= qualityGateThreshold;
    }
}