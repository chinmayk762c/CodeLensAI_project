package com.codelens.backend.controller;

import com.codelens.backend.ai.DocstringService;
import com.codelens.backend.dto.DocstringResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class DocstringController {

    private final DocstringService docstringService;

    @PostMapping("/{id}/docstrings")
    public ResponseEntity<?> generateDocstrings(@PathVariable Long id, Authentication authentication) {
        try {
            DocstringResponse response = docstringService.generate(authentication.getName(), id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Docstring generation failed: " + e.getMessage()));
        }
    }
}