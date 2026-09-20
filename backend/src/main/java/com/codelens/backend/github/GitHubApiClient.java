package com.codelens.backend.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

@Component
public class GitHubApiClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public record ChangedFile(String path, String rawUrl) {}

    public List<ChangedFile> getChangedJavaFiles(String token, String owner, String repo, int prNumber) throws Exception {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/pulls/" + prNumber + "/files";
        HttpRequest request = baseRequest(url, token).GET().build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Failed to list PR files: " + response.statusCode());
        }

        List<ChangedFile> files = new ArrayList<>();
        for (JsonNode entry : mapper.readTree(response.body())) {
            String filename = entry.get("filename").asText();
            String status = entry.get("status").asText();
            if (filename.endsWith(".java") && !"removed".equals(status)) {
                files.add(new ChangedFile(filename, entry.get("raw_url").asText()));
            }
        }
        return files;
    }

    public String fetchRawContent(String token, String rawUrl) throws Exception {
        HttpRequest request = baseRequest(rawUrl, token).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Failed to fetch file: " + response.statusCode());
        }
        return response.body();
    }

    public long createCheckRun(String token, String owner, String repo, String headSha) throws Exception {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/check-runs";
        String body = """
                {"name":"CodeLens AI Review","head_sha":"%s","status":"in_progress"}
                """.formatted(headSha);

        HttpRequest request = baseRequest(url, token)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new IllegalStateException("Failed to create check run: " + response.statusCode() + " " + response.body());
        }
        return mapper.readTree(response.body()).get("id").asLong();
    }

    public void completeCheckRun(String token, String owner, String repo, long checkRunId,
                                  boolean passed, String summary) throws Exception {
        String url = "https://api.github.com/repos/" + owner + "/" + repo + "/check-runs/" + checkRunId;
        String conclusion = passed ? "success" : "failure";
        String escapedSummary = summary.replace("\"", "\\\"").replace("\n", "\\n");

        String body = """
                {"status":"completed","conclusion":"%s","output":{"title":"CodeLens AI Review","summary":"%s"}}
                """.formatted(conclusion, escapedSummary);

        HttpRequest request = baseRequest(url, token)
                .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                .build();

        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder baseRequest(String url, String token) {
        return HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "CodeLensAI");
    }
}