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

import com.summa.util.JsonHelpers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Component
public class NodeAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(NodeAuthFilter.class);

    private static final List<String> NODE_AUTH_PATHS = List.of(
        "/api/nodes/",
        "/api/nodes"
    );

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
        if (path.equals("/api/nodes/enroll") || path.startsWith("/api/nodes/enroll?")) {
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
        // Path format: /api/nodes/<uuid>/... or /api/nodes
        String[] parts = path.split("/");
        // /api/nodes/<id>/... => parts[3] is the id
        if (parts.length >= 4 && parts[0].isEmpty() && "api".equals(parts[1]) && "nodes".equals(parts[2])) {
            String candidate = parts[3];
            // Validate UUID format
            if (candidate.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")) {
                return candidate;
            }
        }
        return null;
    }

    private String computeSignature(String method, String path, String body, String pubkey) {
        try {
            String keyBytes;
            // If pubkey looks like base64 (contains + or / or ends with =), decode it;
            // otherwise use raw UTF-8 bytes as the HMAC key.
            if (pubkey.contains("+") || pubkey.contains("/") || pubkey.endsWith("=")) {
                keyBytes = new String(Base64.getDecoder().decode(pubkey), StandardCharsets.UTF_8);
            } else {
                keyBytes = pubkey;
            }
            String payload = method + ":" + path + ":" + body;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyBytes.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("[SUMMA] signature computation failed: {}", e.getMessage());
            return "";
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
