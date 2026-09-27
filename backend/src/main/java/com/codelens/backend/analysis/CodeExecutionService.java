package com.codelens.backend.analysis;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Component
public class CodeExecutionService {

    private static final int TIMEOUT_SECONDS = 10;
    private static final int MAX_OUTPUT_CHARS = 20_000;

    public record ExecutionResult(String output, boolean timedOut, boolean compileFailed, Integer exitCode) {}

    private final CodeFileWriter codeFileWriter;
    private final JavaCompilerService compilerService;

    public CodeExecutionService(CodeFileWriter codeFileWriter, JavaCompilerService compilerService) {
        this.codeFileWriter = codeFileWriter;
        this.compilerService = compilerService;
    }

    public ExecutionResult execute(String code) throws Exception {
        CodeFileWriter.WrittenFile written = codeFileWriter.write(code);
        try {
            JavaCompilerService.CompileResult compileResult = compilerService.compileWithClasspath(
                    written.javaFile(),
                    written.directory().resolve("classes"),
                    System.getProperty("java.class.path")
            );

            if (!compileResult.success()) {
                return new ExecutionResult(compileResult.errorOutput(), false, true, null);
            }

            return runCompiled(compileResult.classOutputDir(), written.className());
        } finally {
            codeFileWriter.cleanup(written.directory());
        }
    }

    private ExecutionResult runCompiled(Path classDir, String className) throws Exception {
        String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        if (System.getProperty("os.name").toLowerCase().contains("win")) javaBin += ".exe";

        ProcessBuilder pb = new ProcessBuilder(javaBin, "-cp", classDir.toString(), className);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        Thread reader = new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                int c;
                while ((c = br.read()) != -1) {
                    synchronized (output) {
                        if (output.length() < MAX_OUTPUT_CHARS) {
                            output.append((char) c);
                        } else {
                            process.destroyForcibly();
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        });
        reader.start();

        boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            reader.join(2000);
            return new ExecutionResult(
                    output + "\n\n[Execution timed out after " + TIMEOUT_SECONDS + "s — possible infinite loop]",
                    true, false, null
            );
        }

        reader.join(2000);
        String finalOutput = output.toString();
        if (finalOutput.length() >= MAX_OUTPUT_CHARS) {
            finalOutput += "\n\n[Output truncated — exceeded " + MAX_OUTPUT_CHARS + " characters]";
        }
        return new ExecutionResult(finalOutput, false, false, process.exitValue());
    }
}