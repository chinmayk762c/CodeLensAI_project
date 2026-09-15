package com.codelens.backend.analysis;


import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.tools.ExecFileLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CoverageRunner {

    private static final Pattern PUBLIC_CLASS_PATTERN = Pattern.compile("public\\s+class\\s+(\\w+)");

    private final JavaCompilerService compilerService;

    public CoverageRunner(JavaCompilerService compilerService) {
        this.compilerService = compilerService;
    }

    public Double run(String sourceCode, String testCode) throws Exception {
        Path workDir = Files.createTempDirectory("codelens-coverage-");
        try {
            String sourceClassName = extractClassName(sourceCode).orElse("Source");
            String testClassName = extractClassName(testCode).orElse("SourceTest");

            Path sourceFile = workDir.resolve(sourceClassName + ".java");
            Path testFile = workDir.resolve(testClassName + ".java");
            Files.writeString(sourceFile, sourceCode);
            Files.writeString(testFile, testCode);

            Path sourceClasses = workDir.resolve("source-classes");
            Path testClasses = workDir.resolve("test-classes");

            String ownClasspath = System.getProperty("java.class.path");

            JavaCompilerService.CompileResult sourceResult =
                    compilerService.compileWithClasspath(sourceFile, sourceClasses, ownClasspath);
            if (!sourceResult.success()) {
                return null;
            }

            String testClasspath = ownClasspath + File.pathSeparator + sourceClasses;
            JavaCompilerService.CompileResult testResult =
                    compilerService.compileWithClasspath(testFile, testClasses, testClasspath);
            if (!testResult.success()) {
                return null;
            }

            return runWithJacoco(sourceClasses, testClasses, ownClasspath);
        } finally {
            deleteRecursive(workDir);
        }
    }

    private Double runWithJacoco(Path sourceClasses, Path testClasses, String ownClasspath) throws Exception {
                Path execFile = Files.createTempFile("jacoco-", ".exec");
        String agentJarPath = findJacocoAgentJar(ownClasspath);
        if (agentJarPath == null) {
            throw new IllegalStateException("Could not locate org.jacoco.agent runtime jar on classpath");
        }

        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            javaBin += ".exe";
        }

        String runClasspath = ownClasspath + File.pathSeparator + sourceClasses + File.pathSeparator + testClasses;

                ProcessBuilder pb = new ProcessBuilder(
                javaBin,
                "-javaagent:" + agentJarPath + "=destfile=" + execFile.toAbsolutePath(),
                "-cp", runClasspath,
                "org.junit.platform.console.ConsoleLauncher",
                "execute",
                "--scan-class-path", testClasses.toString(),
                "--details=none"
        );
        Path launcherOutput = Files.createTempFile("junit-console-", ".log");
        pb.redirectOutput(launcherOutput.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        Files.deleteIfExists(launcherOutput);

        if (!finished) {
            process.destroyForcibly();
            return null;
        }

        if (Files.size(execFile) == 0) {
            return null;
        }

        ExecFileLoader loader = new ExecFileLoader();
        loader.load(execFile.toFile());

        CoverageBuilder coverageBuilder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), coverageBuilder);
        analyzer.analyzeAll(sourceClasses.toFile());

        int totalLines = 0;
        int coveredLines = 0;
        for (IClassCoverage cc : coverageBuilder.getClasses()) {
            totalLines += cc.getLineCounter().getTotalCount();
            coveredLines += cc.getLineCounter().getCoveredCount();
        }

        Files.deleteIfExists(execFile);
        

        if (totalLines == 0) return null;
        return (coveredLines * 100.0) / totalLines;
    }
    private String findJacocoAgentJar(String classpath) {
        for (String entry : classpath.split(File.pathSeparator)) {
            if (entry.contains("org.jacoco.agent") && entry.endsWith(".jar")) {
                return entry;
            }
        }
        return null;
    }

    private java.util.Optional<String> extractClassName(String code) {
        Matcher matcher = PUBLIC_CLASS_PATTERN.matcher(code);
        return matcher.find() ? java.util.Optional.of(matcher.group(1)) : java.util.Optional.empty();
    }

    private void deleteRecursive(Path dir) {
        try {
            Files.walk(dir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception ignored) {
                        }
                    });
        } catch (Exception ignored) {
        }
    }
}