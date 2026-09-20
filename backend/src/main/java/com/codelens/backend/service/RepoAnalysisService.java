package com.codelens.backend.service;

import com.codelens.backend.dto.*;
import com.codelens.backend.entity.*;
import com.codelens.backend.repository.RepoAnalysisFileRepository;
import com.codelens.backend.repository.RepoAnalysisJobRepository;
import com.codelens.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RepoAnalysisService {

    private static final Pattern GITHUB_URL_PATTERN =
            Pattern.compile("github\\.com/([^/]+)/([^/]+?)(?:\\.git)?/?$");
    private static final int MAX_FILES = 15;

    private final UserRepository userRepository;
    private final RepoAnalysisJobRepository jobRepository;
    private final RepoAnalysisFileRepository fileRepository;
    private final SubmissionService submissionService;
    private final AnalysisService analysisService;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper mapper = new ObjectMapper();

    public RepoAnalysisResponse analyzeRepo(String userEmail, String repoUrl) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found"));

        Matcher matcher = GITHUB_URL_PATTERN.matcher(repoUrl);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Could not parse a GitHub owner/repo from that URL");
        }
        String owner = matcher.group(1);
        String repo = matcher.group(2);

        RepoAnalysisJob job = new RepoAnalysisJob();
        job.setUser(user);
        job.setRepoUrl(repoUrl);
        job.setStatus(AnalysisStatus.IN_PROGRESS);
        jobRepository.save(job);

        List<RepoAnalysisFile> results = new ArrayList<>();

        try {
            String defaultBranch = fetchDefaultBranch(owner, repo);
            List<String> javaFiles = fetchJavaFilePaths(owner, repo, defaultBranch);

            for (String path : javaFiles.stream().limit(MAX_FILES).toList()) {
                RepoAnalysisFile fileResult = new RepoAnalysisFile();
                fileResult.setJob(job);
                fileResult.setFilePath(path);

                try {
                    String content = fetchRawFile(owner, repo, defaultBranch, path);
                    SubmissionResponse submission = submissionService.createSubmission(
                            userEmail, new SubmissionRequest(Language.JAVA, content, null));
                    fileResult.setSubmissionId(submission.id());
                    analysisService.analyze(userEmail, submission.id());
                } catch (Exception fileException) {
                    fileResult.setErrorMessage(fileException.getMessage());
                }

                results.add(fileRepository.save(fileResult));
            }

            job.setStatus(AnalysisStatus.COMPLETED);
        } catch (Exception e) {
            job.setStatus(AnalysisStatus.FAILED);
            RepoAnalysisFile errorFile = new RepoAnalysisFile();
            errorFile.setJob(job);
            errorFile.setFilePath("(repository)");
            errorFile.setErrorMessage(e.getMessage());
            results.add(fileRepository.save(errorFile));
        }

        jobRepository.save(job);
        return toResponse(job, results);
    }

    private String fetchDefaultBranch(String owner, String repo) throws IOException, InterruptedException {
        String url = "https://api.github.com/repos/" + owner + "/" + repo;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "CodeLensAI")
                .header("Accept", "application/vnd.github+json")
                .GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalArgumentException("Repository not found or not public (status " + response.statusCode() + ")");
        }
        JsonNode json = mapper.readTree(response.body());
        return json.get("default_branch").asText();
    }

    private List<String> fetchJavaFilePaths(String owner, String repo, String branch) throws IOException, InterruptedException {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/git/trees/" + branch + "?recursive=1";
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "CodeLensAI")
                .header("Accept", "application/vnd.github+json")
                .GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalArgumentException("Could not list repository files (status " + response.statusCode() + ")");
        }

        JsonNode json = mapper.readTree(response.body());
        List<String> paths = new ArrayList<>();
        for (JsonNode entry : json.get("tree")) {
            String path = entry.get("path").asText();
            if ("blob".equals(entry.get("type").asText()) && path.endsWith(".java")) {
                paths.add(path);
            }
        }
        return paths;
    }

    private String fetchRawFile(String owner, String repo, String branch, String path) throws IOException, InterruptedException {
        String url = "https://raw.githubusercontent.com/" + owner + "/" + repo + "/" + branch + "/" + path;
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "CodeLensAI")
                .GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Could not fetch file content (status " + response.statusCode() + ")");
        }
        return response.body();
    }

    private RepoAnalysisResponse toResponse(RepoAnalysisJob job, List<RepoAnalysisFile> files) {
        List<RepoAnalysisFileResponse> fileResponses = files.stream()
                .map(f -> new RepoAnalysisFileResponse(f.getFilePath(), f.getSubmissionId(), f.getErrorMessage()))
                .toList();

        return new RepoAnalysisResponse(
                job.getId(), job.getRepoUrl(), job.getStatus().toString(), job.getCreatedAt(), fileResponses
        );
    }
}