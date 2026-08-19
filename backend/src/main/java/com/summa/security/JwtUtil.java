package com.summa.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private static final String ALGORITHM = "HmacSHA256";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long SECONDS_TO_MILLIS = 1000L;

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
        if (!constantTimeEquals(expectedSignature, parts[2])) {
            return null;
        }

        try {
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Map<String, Object> payload = MAPPER.readValue(payloadJson, new TypeReference<Map<String, Object>>() {});
            Long exp = ((Number) payload.get("exp")).longValue();
            if (exp * SECONDS_TO_MILLIS < System.currentTimeMillis()) {
                return null;
            }
            Number nbfNum = (Number) payload.get("nbf");
            Long nbf = nbfNum != null ? nbfNum.longValue() : null;
            if (nbf != null && nbf * SECONDS_TO_MILLIS > System.currentTimeMillis()) {
                return null;
            }
            return payload;
        } catch (Exception e) {
            log.warn("JWT parse failure: {}", e.getMessage());
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

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        byte[] ab = a.getBytes(StandardCharsets.UTF_8);
        byte[] bb = b.getBytes(StandardCharsets.UTF_8);
        int result = 0;
        for (int i = 0; i < ab.length; i++) {
            result |= ab[i] ^ bb[i];
        }
        return result == 0;
    }
}
