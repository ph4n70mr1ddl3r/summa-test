package com.summa.security;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "test-secret-that-is-long-enough-for-hs256";

    @Test
    void generateToken_withValidSubject_returnsToken() {
        String token = JwtUtil.generateToken("user-1", SECRET, 3600_000L, 16);
        assertNotNull(token);
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void generateToken_withBlankSubject_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            JwtUtil.generateToken("  ", SECRET, 3600_000L, 16));
    }

    @Test
    void generateToken_withNullSubject_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            JwtUtil.generateToken(null, SECRET, 3600_000L, 16));
    }

    @Test
    void generateToken_withShortSecret_throws() {
        assertThrows(IllegalArgumentException.class, () ->
            JwtUtil.generateToken("user-1", "short", 3600_000L, 16));
    }

    @Test
    void parseToken_withValidToken_returnsPayload() {
        String token = JwtUtil.generateToken("user-1", SECRET, 3600_000L, 16);
        Map<String, Object> payload = JwtUtil.parseToken(token, SECRET);
        assertNotNull(payload);
        assertEquals("user-1", payload.get("sub"));
        assertTrue(payload.containsKey("iat"));
        assertTrue(payload.containsKey("exp"));
        assertTrue(payload.containsKey("sv"));
        assertEquals(0, payload.get("sv"));
    }

    @Test
    void parseToken_withWrongSecret_returnsNull() {
        String token = JwtUtil.generateToken("user-1", SECRET, 3600_000L, 16);
        assertNull(JwtUtil.parseToken(token, "wrong-secret"));
    }

    @Test
    void parseToken_withMalformedToken_returnsNull() {
        assertNull(JwtUtil.parseToken("not.a.valid.token", SECRET));
        assertNull(JwtUtil.parseToken("only-two-parts", SECRET));
        assertNull(JwtUtil.parseToken("", SECRET));
    }

    @Test
    void parseToken_withExpiredToken_returnsNull() {
        String token = JwtUtil.generateToken("user-1", SECRET, -1_000L, 16);
        assertNull(JwtUtil.parseToken(token, SECRET));
    }

    @Test
    void parseToken_withAlgNone_rejects() {
        // Manually craft a token with alg=none
        String header = base64UrlEncode("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes());
        String payload = base64UrlEncode("{\"sub\":\"user-1\",\"iat\":1,\"exp\":9999999999,\"sv\":0}".getBytes());
        String token = header + "." + payload + ".fake";
        assertNull(JwtUtil.parseToken(token, SECRET));
    }

    @Test
    void parseToken_nullInput_returnsNull() {
        assertNull(JwtUtil.parseToken(null, SECRET));
        assertNull(JwtUtil.parseToken("   ", SECRET));
    }

    @Test
    void generateTokenWithSession_includesSessionVersion() {
        String token = JwtUtil.generateTokenWithSession("user-1", SECRET, 3600_000L, 16, 5);
        Map<String, Object> payload = JwtUtil.parseToken(token, SECRET);
        assertNotNull(payload);
        assertEquals(5, payload.get("sv"));
    }

    @Test
    void generateToken_capsExpiryToOneYear() {
        // Request an expiry of 2 years; it should be capped to 1 year
        long twoYearsMillis = 2L * 365 * 24 * 3600 * 1000;
        String token = JwtUtil.generateToken("user-1", SECRET, twoYearsMillis, 16);
        Map<String, Object> payload = JwtUtil.parseToken(token, SECRET);
        assertNotNull(payload);
        long exp = ((Number) payload.get("exp")).longValue();
        long maxExp = System.currentTimeMillis() / 1000 + 365L * 24 * 3600;
        assertTrue(exp <= maxExp, "Expiry should be capped to 1 year from now");
    }

    @Test
    void parseToken_rejectsFarFutureExpiry() {
        // A token with expiry 10 years from now should be rejected
        long farFuture = System.currentTimeMillis() / 1000L + 10L * 365 * 24 * 3600;
        String header = base64UrlEncode("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes());
        String payloadJson = base64UrlEncode(
            ("{\"sub\":\"user-1\",\"iat\":" + (farFuture - 1000) + ",\"exp\":" + farFuture + ",\"sv\":0}").getBytes());
        String sig = base64UrlEncode(computeHmac(header + "." + payloadJson));
        String token = header + "." + payloadJson + "." + sig;
        assertNull(JwtUtil.parseToken(token, SECRET));
    }

    private byte[] computeHmac(String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            mac.init(new javax.crypto.spec.SecretKeySpec(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String base64UrlEncode(byte[] bytes) {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
