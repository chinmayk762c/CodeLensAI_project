package com.codelens.backend.controller;

import com.codelens.backend.analysis.CodeExecutionService;
import com.codelens.backend.dto.ExecuteRequest;
import com.codelens.backend.dto.ExecuteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/execute")
@RequiredArgsConstructor
public class CodeExecutionController {

    private final CodeExecutionService executionService;

    @PostMapping
    public ResponseEntity<ExecuteResponse> execute(@RequestBody ExecuteRequest request) {
        try {
            CodeExecutionService.ExecutionResult result = executionService.execute(request.code());
            return ResponseEntity.ok(new ExecuteResponse(result.output(), result.timedOut(), result.compileFailed(), result.exitCode()));
        } catch (Exception e) {
            return ResponseEntity.ok(new ExecuteResponse("Execution failed: " + e.getMessage(), false, false, null));
        }
    }
}