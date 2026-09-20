package com.codelens.backend.controller;

import com.codelens.backend.dto.RepoAnalysisRequest;
import com.codelens.backend.dto.RepoAnalysisResponse;
import com.codelens.backend.service.RepoAnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/repo-analysis")
@RequiredArgsConstructor
public class RepoAnalysisController {

    private final RepoAnalysisService repoAnalysisService;

    @PostMapping
    public ResponseEntity<?> analyze(@Valid @RequestBody RepoAnalysisRequest request, Authentication authentication) {
        try {
            RepoAnalysisResponse response = repoAnalysisService.analyzeRepo(authentication.getName(), request.repoUrl());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}