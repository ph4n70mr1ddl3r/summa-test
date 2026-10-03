package com.summa.security;

import com.summa.model.Node;
import com.summa.repository.NodeRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.summa.constants.Defaults;
import com.summa.util.JsonHelpers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Component
public class NodeAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(NodeAuthFilter.class);

    private static final List<String> NODE_AUTH_PATHS = List.of(
        "/nodes/",
        "/nodes"
    );

    private static final Pattern PUBKEY_PATTERN = Pattern.compile(Defaults.PUBKEY_REGEX);

    private final NodeRepository nodeRepository;

    public NodeAuthFilter(NodeRepository nodeRepository) {
        this.nodeRepository = nodeRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                      FilterChain filterChain) throws ServletException, IOException {
        // Wrap request to buffer body so downstream readers (JSON deserializers) can also read it
        HttpServletRequest wrappedRequest = new ContentCachingRequestWrapper(request);

        String path = wrappedRequest.getRequestURI();
        if (!matchesNodePath(path)) {
            filterChain.doFilter(wrappedRequest, response);
            return;
        }

        // Enroll is a public operation — skip signature verification
        if (path.equals("/nodes/enroll")) {
            filterChain.doFilter(wrappedRequest, response);
            return;
        }

        String signature = wrappedRequest.getHeader("X-Node-Signature");
        if (signature == null || signature.isBlank()) {
            log.warn("[SUMMA] node request without signature: {} from {}", path, wrappedRequest.getRemoteAddr());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Node signature required");
            return;
        }

        // Extract node ID from path: /api/nodes/<id>/...
        String nodeId = extractNodeId(path);
        if (nodeId == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid node path");
            return;
        }

        Optional<Node> nodeOpt = nodeRepository.findById(nodeId);
        if (nodeOpt.isEmpty()) {
            log.warn("[SUMMA] unknown node ID: {} path={}", nodeId, path);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unknown node");
            return;
        }

        Node node = nodeOpt.get();
        if (node.isRevoked()) {
            log.warn("[SUMMA] revoked node attempted request: {} path={}", nodeId, path);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Node revoked");
            return;
        }

        // Verify HMAC-SHA256 signature using node pubkey as key
        String body = readRequestBody(wrappedRequest);
        String expectedSig = computeSignature(wrappedRequest.getMethod(), path, body, node.getPubkey());
        if (!constantTimeEquals(expectedSig, signature)) {
            log.warn("[SUMMA] node signature mismatch: node={} path={}", nodeId, path);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid node signature");
            return;
        }

        // Authenticated as node — set actor to node ID
        wrappedRequest.setAttribute("actor", nodeId);
        wrappedRequest.setAttribute("nodeAuth", true);
        filterChain.doFilter(wrappedRequest, response);
    }

    private boolean matchesNodePath(String path) {
        if (path == null) return false;
        for (String prefix : NODE_AUTH_PATHS) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    private String extractNodeId(String path) {
        // Path format: /nodes/<uuid>/... or /nodes
        String[] parts = path.split("/");
        // /nodes/<id>/... => parts[1] is the id
        if (parts.length >= 3 && "nodes".equals(parts[1])) {
            String candidate = parts[2];
            // Validate UUID format
            if (candidate.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
                return candidate;
            }
        }
        return null;
    }

    private String computeSignature(String method, String path, String body, String pubkey) {
        try {
            // Always hash the pubkey through SHA-256 before using it as the HMAC key.
            // This ensures uniform key length regardless of whether the pubkey is
            // a raw ASCII string or a base64-encoded ECDSA public key.
            java.security.MessageDigest sha256 = java.security.MessageDigest.getInstance("SHA-256");
            byte[] keyBytes;
            boolean isPubkeyFormat = PUBKEY_PATTERN.matcher(pubkey).matches();
            if (isPubkeyFormat) {
                keyBytes = sha256.digest(Base64.getDecoder().decode(pubkey));
            } else {
                keyBytes = sha256.digest(pubkey.getBytes(StandardCharsets.UTF_8));
            }
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
            String payload = method + ":" + path + ":" + body;
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("[SUMMA] signature computation failed: {}", e.getMessage());
            throw new IllegalStateException("Node signature verification failed", e);
        }
    }

    private String readRequestBody(HttpServletRequest request) {
        try {
            byte[] bytes = ((ContentCachingRequestWrapper) request).getContentAsByteArray();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static boolean constantTimeEquals(String a, String b) {
        return JsonHelpers.constantTimeEquals(a, b);
    }
}
