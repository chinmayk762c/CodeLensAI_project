package com.codelens.backend.github;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

@Component
public class GitHubAppAuth {

    @Value("${github.app.id}")
    private String appId;

    @Value("${github.app.private-key-path}")
    private String privateKeyPath;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

    public String generateAppJwt() throws Exception {
        PrivateKey privateKey = loadPrivateKey();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + 9 * 60 * 1000); // 9 minutes (max 10)
        Date issuedAt = new Date(now.getTime() - 60 * 1000); // 60s clock drift buffer

        return Jwts.builder()
                .issuer(appId)
                .issuedAt(issuedAt)
                .expiration(expiry)
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    public String getInstallationToken(long installationId) throws Exception {
        String appJwt = generateAppJwt();
        String url = "https://api.github.com/app/installations/" + installationId + "/access_tokens";

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("Authorization", "Bearer " + appJwt)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "CodeLensAI")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            throw new IllegalStateException("Failed to get installation token: " + response.statusCode() + " " + response.body());
        }

        return mapper.readTree(response.body()).get("token").asText();
    }

    private PrivateKey loadPrivateKey() throws Exception {
        String pem = Files.readString(Path.of(privateKeyPath));
        String cleaned = pem
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(cleaned);

        byte[] pkcs8Bytes;
        if (pem.contains("BEGIN RSA PRIVATE KEY")) {
            pkcs8Bytes = pkcs1ToPkcs8(keyBytes);
        } else {
            pkcs8Bytes = keyBytes;
        }

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(pkcs8Bytes));
    }

    private byte[] pkcs1ToPkcs8(byte[] pkcs1Bytes) {
        int pkcs1Length = pkcs1Bytes.length;
        int totalLength = pkcs1Length + 22;
        byte[] header = new byte[] {
                0x30, (byte) 0x82, (byte) ((totalLength >> 8) & 0xff), (byte) (totalLength & 0xff),
                0x2, 0x1, 0x0,
                0x30, 0xD, 0x6, 0x9, 0x2A, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xF7, 0xD, 0x1, 0x1, 0x1, 0x5, 0x0,
                0x4, (byte) 0x82, (byte) ((pkcs1Length >> 8) & 0xff), (byte) (pkcs1Length & 0xff)
        };

        byte[] result = new byte[header.length + pkcs1Bytes.length];
        System.arraycopy(header, 0, result, 0, header.length);
        System.arraycopy(pkcs1Bytes, 0, result, header.length, pkcs1Bytes.length);
        return result;
    }
}