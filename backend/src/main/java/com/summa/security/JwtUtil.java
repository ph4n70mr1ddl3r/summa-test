package com.summa.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

public class JwtUtil {

    private static final String ALGORITHM = "HmacSHA256";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JwtUtil() {}

    public static String generateToken(String subject, String secret, long expirationMillis) {
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + expirationMillis;

        Map<String, Object> payload = Map.of(
            "sub", subject,
            "iat", nowMillis / 1000,
            "exp", expMillis / 1000
        );

        String header = base64UrlEncode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payloadJson;
        try {
            payloadJson = MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize JWT payload", e);
        }
        String encodedPayload = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));

        String signatureInput = header + "." + encodedPayload;
        String signature = base64UrlEncode(hmacSha256(signatureInput, secret));

        return signatureInput + "." + signature;
    }

    public static Map<String, Object> parseToken(String token, String secret) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }

        String signatureInput = parts[0] + "." + parts[1];
        String expectedSignature = base64UrlEncode(hmacSha256(signatureInput, secret));
        if (!expectedSignature.equals(parts[2])) {
            return null;
        }

        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Map<String, Object> payload = MAPPER.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
            Long exp = ((Number) payload.get("exp")).longValue();
            if (exp * 1000L < System.currentTimeMillis()) {
                return null;
            }
            return payload;
        } catch (Exception e) {
            return null;
        }
    }

    private static byte[] hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC-SHA256", e);
        }
    }

    private static String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

