package com.codelens.backend.analysis;

import org.springframework.stereotype.Component;

import javax.tools.*;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class JavaCompilerService {

    public record CompileResult(boolean success, Path classOutputDir, String errorOutput) {}

    public CompileResult compile(Path javaFile) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("No system Java compiler available — is a JDK (not just a JRE) installed?");
        }

        Path outputDir = Files.createTempDirectory("codelens-classes-");
        StringWriter errorWriter = new StringWriter();

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(outputDir.toFile()));

            Iterable<? extends JavaFileObject> compilationUnits =
                    fileManager.getJavaFileObjectsFromPaths(List.of(javaFile));

            JavaCompiler.CompilationTask task = compiler.getTask(
                    errorWriter, fileManager, null, null, null, compilationUnits
            );

            boolean success = task.call();
            return new CompileResult(success, outputDir, errorWriter.toString());
        }
    }
}