package com.codelens.backend.analysis;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CodeFileWriter {

    private static final Pattern PUBLIC_CLASS_PATTERN =
            Pattern.compile("public\\s+class\\s+(\\w+)");

    public record WrittenFile(Path directory, Path javaFile, String className) {}

    public WrittenFile write(String code) throws IOException {
        String className = extractClassName(code).orElse("Submission");

        Path tempDir = Files.createTempDirectory("codelens-analysis-");
        Path javaFile = tempDir.resolve(className + ".java");
        Files.writeString(javaFile, code);

        return new WrittenFile(tempDir, javaFile, className);
    }

    public void cleanup(Path directory) {
        try {
            Files.walk(directory)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private java.util.Optional<String> extractClassName(String code) {
        Matcher matcher = PUBLIC_CLASS_PATTERN.matcher(code);
        return matcher.find() ? java.util.Optional.of(matcher.group(1)) : java.util.Optional.empty();
    }
}