package com.codelens.backend.analysis;

import com.codelens.backend.entity.IssueCategory;
import com.codelens.backend.entity.IssueSeverity;
import com.puppycrawl.tools.checkstyle.Checker;
import com.puppycrawl.tools.checkstyle.ConfigurationLoader;
import com.puppycrawl.tools.checkstyle.PropertiesExpander;
import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AuditListener;
import com.puppycrawl.tools.checkstyle.api.Configuration;
import com.puppycrawl.tools.checkstyle.api.SeverityLevel;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Component
public class CheckstyleRunner {

    public List<RawIssue> run(Path javaFile) throws Exception {
        List<RawIssue> issues = new ArrayList<>();

        Configuration config = ConfigurationLoader.loadConfiguration(
                "/sun_checks.xml",
                new PropertiesExpander(new Properties())
        );

        Checker checker = new Checker();
        checker.setModuleClassLoader(Checker.class.getClassLoader());
        checker.configure(config);

        checker.addListener(new AuditListener() {
            @Override public void auditStarted(AuditEvent event) {}
            @Override public void auditFinished(AuditEvent event) {}
            @Override public void fileStarted(AuditEvent event) {}
            @Override public void fileFinished(AuditEvent event) {}

            @Override
            public void addError(AuditEvent event) {
                issues.add(new RawIssue(
                        mapSeverity(event.getSeverityLevel()),
                        IssueCategory.STYLE,
                        event.getMessage(),
                        event.getLine(),
                        null
                ));
            }

            @Override
            public void addException(AuditEvent event, Throwable throwable) {}
        });

        checker.process(List.of(javaFile.toFile()));
        checker.destroy();

        return issues;
    }

    private IssueSeverity mapSeverity(SeverityLevel level) {
        if (level == SeverityLevel.ERROR) return IssueSeverity.HIGH;
        if (level == SeverityLevel.WARNING) return IssueSeverity.MEDIUM;
        return IssueSeverity.LOW;
    }
}