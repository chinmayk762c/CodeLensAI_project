package com.codelens.backend.analysis;

import com.codelens.backend.entity.IssueCategory;
import com.codelens.backend.entity.IssueSeverity;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class SpotBugsRunner {

    public List<RawIssue> run(Path classOutputDir) throws Exception {
        List<RawIssue> issues = new ArrayList<>();

        Path xmlOutput = Files.createTempFile("spotbugs-output-", ".xml");

        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            javaBin += ".exe";
        }
        String classpath = System.getProperty("java.class.path");

        ProcessBuilder pb = new ProcessBuilder(
                javaBin,
                "-cp", classpath,
                "edu.umd.cs.findbugs.LaunchAppropriateUI",
                "-textui",
                "-xml:withMessages",
                "-effort:max",
                classOutputDir.toString()
        );
        pb.redirectOutput(xmlOutput.toFile());
        pb.redirectErrorStream(false);

        Process process = pb.start();
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);

        if (!finished) {
            process.destroyForcibly();
            System.out.println("[DEBUG] SpotBugs subprocess timed out");
            return issues;
        }

        System.out.println("[DEBUG] SpotBugs subprocess exit code: " + process.exitValue());

        issues.addAll(parseXml(xmlOutput));
        Files.deleteIfExists(xmlOutput);

        return issues;
    }

    private List<RawIssue> parseXml(Path xmlFile) throws Exception {
        List<RawIssue> issues = new ArrayList<>();

        if (Files.size(xmlFile) == 0) {
            System.out.println("[DEBUG] SpotBugs XML output was empty");
            return issues;
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile.toFile());

        NodeList bugInstances = doc.getElementsByTagName("BugInstance");
        System.out.println("[DEBUG] SpotBugs XML BugInstance count: " + bugInstances.getLength());

        for (int i = 0; i < bugInstances.getLength(); i++) {
            Element bug = (Element) bugInstances.item(i);

            int priority = parseIntSafe(bug.getAttribute("priority"), 3);
            String category = bug.getAttribute("category");

            String description = getFirstChildText(bug, "LongMessage");
            Integer lineNumber = null;

            NodeList sourceLines = bug.getElementsByTagName("SourceLine");
            if (sourceLines.getLength() > 0) {
                Element sourceLine = (Element) sourceLines.item(0);
                lineNumber = parseIntSafe(sourceLine.getAttribute("start"), 0);
                if (lineNumber == 0) lineNumber = null;
            }

            issues.add(new RawIssue(
                    mapSeverity(priority),
                    mapCategory(category),
                    description != null ? description : "SpotBugs finding: " + bug.getAttribute("type"),
                    lineNumber,
                    null
            ));
        }

        return issues;
    }

    private String getFirstChildText(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() == 0) return null;
        return nodes.item(0).getTextContent();
    }

    private int parseIntSafe(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private IssueSeverity mapSeverity(int priority) {
        return switch (priority) {
            case 1 -> IssueSeverity.CRITICAL;
            case 2 -> IssueSeverity.HIGH;
            case 3 -> IssueSeverity.MEDIUM;
            default -> IssueSeverity.LOW;
        };
    }

    private IssueCategory mapCategory(String spotbugsCategory) {
        if (spotbugsCategory == null) return IssueCategory.BUG;
        return switch (spotbugsCategory) {
            case "SECURITY" -> IssueCategory.SECURITY;
            case "PERFORMANCE" -> IssueCategory.PERFORMANCE;
            case "STYLE" -> IssueCategory.STYLE;
            default -> IssueCategory.BUG;
        };
    }
}