package com.summa.security;

import com.summa.model.Node;
import com.summa.repository.NodeRepository;
import com.summa.service.AuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.summa.constants.Defaults;
import com.summa.util.JsonHelpers;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Node runtime authentication (API-060 / SEC-012).
 *
 * Signature verification is enforced only on the four node callback endpoints —
 * heartbeat, claims, work/pull, and runs/:runId/report — which the enrolled node
 * calls with its HMAC-SHA256 signature over method:path:body. Every other path
 * under /api/nodes (list, view, update, revoke) is console/admin surface and is
 * authenticated by the JWT filter instead.
 *
 * On a verified callback the filter installs the node as the request actor and
 * in the SecurityContext so downstream authorization sees an authenticated
 * principal, and hands downstream readers a re-readable body wrapper so the
 * signature check does not consume the request body.
 */
@Component
public class NodeAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(NodeAuthFilter.class);

    private static final List<String> NODE_AUTH_PATHS = List.of(
        "/api/nodes/",
        "/api/nodes"
    );

    // Reject node auth requests with bodies exceeding this size (1 MB) to prevent memory exhaustion
    // before signature verification.
    private static final int MAX_BODY_BYTES = 1_048_576;

    private static final Pattern PUBKEY_PATTERN = Pattern.compile(Defaults.PUBKEY_REGEX);

    private static final String UUID_SEGMENT =
        "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    // The four API-060 callback endpoints, relative to /api/nodes/<nodeId>.
    private static final List<String> CALLBACK_SUFFIXES = List.of(
        "heartbeat",
        "claims",
        "work/pull"
    );

    private static final Pattern RUN_REPORT_PATTERN =
        Pattern.compile("runs/[^/]+/report");

    private final NodeRepository nodeRepository;
    private final AuditService auditService;

    public NodeAuthFilter(NodeRepository nodeRepository, AuditService auditService) {
        this.nodeRepository = nodeRepository;
        this.auditService = auditService;
    }

    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request,
                                       @org.springframework.lang.NonNull HttpServletResponse response,
                                       @org.springframework.lang.NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (!matchesNodePath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Normalize trailing slash so /api/nodes/enroll/ is treated the same as /api/nodes/enroll
        String normalizedPath = JsonHelpers.normalizeTrailingSlash(path);
        if (normalizedPath != null && normalizedPath.equals("/api/nodes/enroll")) {
            // Enroll is a public operation — skip signature verification
            filterChain.doFilter(request, response);
            return;
        }

        if (!isNodeCallbackPath(normalizedPath)) {
            // Console/admin node management surface: JWT authentication handles it.
            filterChain.doFilter(request, response);
            return;
        }

        String signature = request.getHeader("X-Node-Signature");
        if (signature == null || signature.isBlank()) {
            log.warn("[SUMMA] node request without signature: {} from {}", path, request.getRemoteAddr());
            writeRefusal(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Node signature required");
            return;
        }

        String nodeId = extractNodeId(normalizedPath);
        if (nodeId == null) {
            writeRefusal(request, response, HttpServletResponse.SC_BAD_REQUEST, "Invalid node path");
            return;
        }

        Optional<Node> nodeOpt = nodeRepository.findById(nodeId);
        if (nodeOpt.isEmpty()) {
            log.warn("[SUMMA] unknown node ID: {} path={}", nodeId, path);
            writeRefusal(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Unknown node");
            return;
        }

        Node node = nodeOpt.get();
        if (node.isRevoked()) {
            log.warn("[SUMMA] revoked node attempted request: {} path={}", nodeId, path);
            writeRefusal(request, response, HttpServletResponse.SC_FORBIDDEN, "Node revoked");
            return;
        }

        // Buffer the body once, capped, so the HMAC covers exactly what downstream
        // readers will parse (ContentCachingRequestWrapper alone caches lazily, which
        // made the pre-chain read an always-empty payload).
        byte[] bodyBytes;
        try {
            bodyBytes = readRequestBody(request);
        } catch (BodyTooLargeException e) {
            log.warn("[SUMMA] node request body too large: node={} path={}", nodeId, path);
            writeRefusal(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Node signature verification failed");
            return;
        } catch (IOException e) {
            log.warn("[SUMMA] node request body read error: {}", e.getMessage());
            writeRefusal(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Node signature verification failed");
            return;
        }
        String body = new String(bodyBytes, StandardCharsets.UTF_8);

        // Use normalized path for signature computation to ensure consistency
        String expectedSig;
        try {
            expectedSig = computeSignature(request.getMethod(), normalizedPath, body, node.getPubkey());
        } catch (Exception e) {
            log.warn("[SUMMA] node signature computation error: {}", e.getMessage());
            writeRefusal(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Node signature verification failed");
            return;
        }
        if (!constantTimeEquals(expectedSig, signature)) {
            log.warn("[SUMMA] node signature mismatch: node={} path={}", nodeId, path);
            writeRefusal(request, response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid node signature");
            return;
        }

        // Authenticated as node — set actor to node ID and populate the SecurityContext
        // so .anyRequest().authenticated() admits the callback.
        HttpServletRequest wrappedRequest = new CachedBodyRequestWrapper(request, bodyBytes);
        wrappedRequest.setAttribute("actor", nodeId);
        wrappedRequest.setAttribute("nodeAuth", true);
        var auth = new UsernamePasswordAuthenticationToken(nodeId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            filterChain.doFilter(wrappedRequest, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean matchesNodePath(String path) {
        if (path == null) return false;
        for (String prefix : NODE_AUTH_PATHS) {
            if (path.startsWith(prefix)) return true;
        }
        return false;
    }

    /**
     * True only for the four API-060 node runtime callbacks that must carry the
     * node's signature. Accepts a tolerated trailing slash.
     */
    static boolean isNodeCallbackPath(String normalizedPath) {
        if (normalizedPath == null) return false;
        if (!normalizedPath.startsWith("/api/nodes/")) return false;
        String[] parts = normalizedPath.split("/");
        // /api/nodes/<id>/<suffix...> => parts[1]="api", parts[2]="nodes", parts[3]=id
        if (parts.length < 5) return false;
        if (!"api".equals(parts[1]) || !"nodes".equals(parts[2])) return false;
        if (!parts[3].matches(UUID_SEGMENT)) return false;
        String suffix = String.join("/", java.util.Arrays.copyOfRange(parts, 4, parts.length));
        return CALLBACK_SUFFIXES.contains(suffix) || RUN_REPORT_PATTERN.matcher(suffix).matches();
    }

    private String extractNodeId(String path) {
        // Path format: /api/nodes/<uuid>/... or /api/nodes
        String[] parts = path.split("/");
        // /api/nodes/<id>/... => parts[1]="api", parts[2]="nodes", parts[3]=id
        if (parts.length >= 4 && "api".equals(parts[1]) && "nodes".equals(parts[2])) {
            String candidate = parts[3];
            if (candidate.matches(UUID_SEGMENT)) {
                return candidate;
            }
        }
        return null;
    }

    private String computeSignature(String method, String path, String body, String pubkey) {
        // Always hash the pubkey through SHA-256 before using it as the HMAC key.
        // This ensures uniform key length regardless of whether the pubkey is
        // a raw ASCII string or a base64-encoded ECDSA public key.
        try {
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

    private static byte[] readRequestBody(HttpServletRequest request) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        try (InputStream in = request.getInputStream()) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_BODY_BYTES) {
                    throw new BodyTooLargeException();
                }
                out.write(buffer, 0, read);
            }
        }
        return out.toByteArray();
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

    /**
     * Writes a structured JSON refusal response per API-061, including an audit event.
     */
    private void writeRefusal(HttpServletRequest request, HttpServletResponse response, int status, String message) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String actor = (String) request.getAttribute("actor");
        com.summa.model.AuditEvent auditEvent = auditService.log(actor != null ? actor : Defaults.SYSTEM_ACTOR, "REFUSAL", "node_auth", null, message);
        Map<String, Object> body = Map.of(
            "code", "unauthorized",
            "message", message,
            "audit_event_id", auditEvent.getId()
        );
        response.setStatus(status);
        response.getWriter().write(JsonHelpers.toJson(body, null));
    }

    private static final class BodyTooLargeException extends IOException {
        BodyTooLargeException() {
            super("Request body exceeds maximum size");
        }
    }

    /**
     * Re-readable request wrapper: the filter consumes the body for HMAC
     * verification, downstream readers get the same bytes again.
     */
    private static final class CachedBodyRequestWrapper extends jakarta.servlet.http.HttpServletRequestWrapper {
        private final byte[] body;

        CachedBodyRequestWrapper(HttpServletRequest request, byte[] body) {
            super(request);
            this.body = body;
        }

        @Override
        public ServletInputStream getInputStream() {
            CachedBodyInputStream stream = new CachedBodyInputStream(body);
            return stream;
        }

        @Override
        public BufferedReader getReader() {
            String encoding = getCharacterEncoding() != null ? getCharacterEncoding() : StandardCharsets.UTF_8.name();
            return new BufferedReader(new InputStreamReader(getInputStream(), java.nio.charset.Charset.forName(encoding)));
        }

        private static final class CachedBodyInputStream extends ServletInputStream {
            private final java.io.ByteArrayInputStream delegate;

            CachedBodyInputStream(byte[] body) {
                this.delegate = new java.io.ByteArrayInputStream(body);
            }

            @Override
            public boolean isFinished() {
                return delegate.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
                throw new UnsupportedOperationException("Async reads are not supported");
            }

            @Override
            public int read() {
                return delegate.read();
            }
        }
    }
}
