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
        int score = 100;
        for (Issue issue : issues) {
            score -= switch (issue.getSeverity()) {
                case CRITICAL -> 15;
                case HIGH -> 8;
                case MEDIUM -> 4;
                case LOW -> 1;
            };
        }
        return Math.max(score, 0);
    }

    public boolean passesQualityGate(int score) {
        return score >= qualityGateThreshold;
    }
}