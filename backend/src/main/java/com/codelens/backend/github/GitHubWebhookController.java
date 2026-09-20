package com.codelens.backend.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Set;

@RestController
@RequiredArgsConstructor
public class GitHubWebhookController {

    @Value("${github.webhook.secret}")
    private String webhookSecret;

    private final PullRequestAnalysisService prAnalysisService;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Set<String> TRIGGER_ACTIONS = Set.of("opened", "synchronize", "reopened");

    @PostMapping("/api/github/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestHeader(value = "X-GitHub-Event", required = false) String eventType,
            @RequestBody String payload
    ) {
        if (signature == null || !isValidSignature(payload, signature)) {
            return ResponseEntity.status(401).body("Invalid signature");
        }

        System.out.println("[GITHUB] Received event: " + eventType);

        if ("pull_request".equals(eventType)) {
            try {
                JsonNode json = mapper.readTree(payload);
                String action = json.get("action").asText();

                if (TRIGGER_ACTIONS.contains(action)) {
                    long installationId = json.get("installation").get("id").asLong();
                    String owner = json.get("repository").get("owner").get("login").asText();
                    String repo = json.get("repository").get("name").asText();
                    int prNumber = json.get("pull_request").get("number").asInt();
                    String headSha = json.get("pull_request").get("head").get("sha").asText();

                    System.out.println("[GITHUB] PR #" + prNumber + " " + action + " on " + owner + "/" + repo);
                    prAnalysisService.analyzeAndReport(installationId, owner, repo, prNumber, headSha);
                }
            } catch (Exception e) {
                System.err.println("[GITHUB] Error processing PR event: " + e.getMessage());
            }
        }

        return ResponseEntity.ok("received");
    }

    private boolean isValidSignature(String payload, String signatureHeader) {
        try {
            String expected = "sha256=" + hmacSha256(payload, webhookSecret);
            return java.security.MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    signatureHeader.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }

    private String hmacSha256(String data, String key) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : hash) hex.append(String.format("%02x", b));
        return hex.toString();
    }
}