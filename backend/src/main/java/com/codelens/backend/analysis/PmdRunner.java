package com.codelens.backend.analysis;

import com.codelens.backend.entity.IssueCategory;
import com.codelens.backend.entity.IssueSeverity;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class PmdRunner {

    public List<RawIssue> run(Path javaFile) {
        List<RawIssue> issues = new ArrayList<>();

        PMDConfiguration config = new PMDConfiguration();
        config.addInputPath(javaFile);
        config.addRuleSet("category/java/bestpractices.xml");
        config.addRuleSet("category/java/errorprone.xml");
        config.addRuleSet("category/java/design.xml");
        config.addRuleSet("category/java/codestyle.xml");

        try (PmdAnalysis pmd = PmdAnalysis.create(config)) {
            Report report = pmd.performAnalysisAndCollectReport();
            for (RuleViolation violation : report.getViolations()) {
                issues.add(new RawIssue(
                        mapSeverity(violation.getRule().getPriority().getPriority()),
                        mapCategory(violation.getRule().getRuleSetName()),
                        violation.getDescription(),
                        violation.getBeginLine(),
                        null
                ));
            }
        }

        return issues;
    }

    private IssueSeverity mapSeverity(int priority) {
        return switch (priority) {
            case 1, 2 -> IssueSeverity.HIGH;
            case 3 -> IssueSeverity.MEDIUM;
            default -> IssueSeverity.LOW;
        };
    }

    private IssueCategory mapCategory(String ruleSetName) {
        if (ruleSetName == null) return IssueCategory.CODE_SMELL;
        String name = ruleSetName.toLowerCase();
        if (name.contains("error")) return IssueCategory.BUG;
        if (name.contains("style")) return IssueCategory.STYLE;
        return IssueCategory.CODE_SMELL;
    }
}