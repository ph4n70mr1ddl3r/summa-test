package com.summa.security;

import com.summa.model.AuditEvent;
import com.summa.model.Node;
import com.summa.repository.NodeRepository;
import com.summa.service.AuditService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class NodeAuthFilterTest {

    private static final String NODE_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String RUN_ID = "6ba7b810-9dad-11d1-80b4-00c04fd430c8";
    // Raw (non base64) pubkey so computeSignature uses the UTF-8 branch.
    private static final String PUBKEY = "raw-node-pubkey-abcdefgh";

    private NodeRepository nodeRepository;
    private AuditService auditService;
    private NodeAuthFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;
    private StringWriter responseBody;

    @BeforeEach
    void setUp() throws Exception {
        nodeRepository = mock(NodeRepository.class);
        auditService = mock(AuditService.class);
        AuditEvent auditEvent = new AuditEvent();
        auditEvent.setId("audit-123");
        when(auditService.log(any(), any(), any(), any(), any())).thenReturn(auditEvent);
        filter = new NodeAuthFilter(nodeRepository, auditService);

        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        responseBody = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseBody, true));
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        // Mockito mocks don't store attributes by default — back them with a map so the
        // wrapper-delegated setAttribute/getAttribute round-trip works.
        Map<String, Object> attributes = new HashMap<>();
        doAnswer(invocation -> {
            attributes.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(request).setAttribute(anyString(), any());
        when(request.getAttribute(anyString())).thenAnswer(invocation -> attributes.get(invocation.getArgument(0)));

        Node node = new Node();
        node.setId(NODE_ID);
        node.setPubkey(PUBKEY);
        when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(node));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void isNodeCallbackPath_api060Callbacks_returnTrue() {
        assertTrue(NodeAuthFilter.isNodeCallbackPath("/api/nodes/" + NODE_ID + "/heartbeat"));
        assertTrue(NodeAuthFilter.isNodeCallbackPath("/api/nodes/" + NODE_ID + "/claims"));
        assertTrue(NodeAuthFilter.isNodeCallbackPath("/api/nodes/" + NODE_ID + "/work/pull"));
        assertTrue(NodeAuthFilter.isNodeCallbackPath("/api/nodes/" + NODE_ID + "/runs/" + RUN_ID + "/report"));
        assertTrue(NodeAuthFilter.isNodeCallbackPath("/api/nodes/" + NODE_ID + "/heartbeat/"));
    }

    @Test
    void isNodeCallbackPath_consoleAndJunkPaths_returnFalse() {
        assertFalse(NodeAuthFilter.isNodeCallbackPath("/api/nodes"));
        assertFalse(NodeAuthFilter.isNodeCallbackPath("/api/nodes/"));
        assertFalse(NodeAuthFilter.isNodeCallbackPath("/api/nodes/" + NODE_ID));
        assertFalse(NodeAuthFilter.isNodeCallbackPath("/api/nodes/" + NODE_ID + "/revoke"));
        assertFalse(NodeAuthFilter.isNodeCallbackPath("/api/nodes/enroll"));
        assertFalse(NodeAuthFilter.isNodeCallbackPath("/api/nodes/not-a-uuid/heartbeat"));
        assertFalse(NodeAuthFilter.isNodeCallbackPath("/api/org/humans"));
        assertFalse(NodeAuthFilter.isNodeCallbackPath(null));
    }

    @Test
    void callback_withoutSignature_refusesWith401() throws Exception {
        stubCallbackPath("POST", null);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseBody.toString().contains("Node signature required"));
    }

    @Test
    void callback_withInvalidSignature_refusesWith401() throws Exception {
        stubCallbackPath("POST", "{\"capabilities\":\"shell\"}");
        when(request.getHeader("X-Node-Signature")).thenReturn("deadbeef");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertTrue(responseBody.toString().contains("Invalid node signature"));
    }

    @Test
    void callback_withValidSignature_authenticatesNodeAndHandsReReadableBody() throws Exception {
        String body = "{\"capabilities\":\"shell\"}";
        stubCallbackPath("POST", body);
        String signature = sign("POST", "/api/nodes/" + NODE_ID + "/heartbeat", body);
        when(request.getHeader("X-Node-Signature")).thenReturn(signature);

        final HttpServletRequest[] captured = new HttpServletRequest[1];
        final Authentication[] capturedAuth = new Authentication[1];
        doAnswer(invocation -> {
            captured[0] = invocation.getArgument(0);
            capturedAuth[0] = SecurityContextHolder.getContext().getAuthentication();
            // Downstream must be able to read the body the filter consumed for HMAC
            byte[] read = readAll(captured[0].getInputStream());
            assertEquals(body, new String(read, StandardCharsets.UTF_8));
            return null;
        }).when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(request, response, filterChain);

        assertNotNull(captured[0]);
        assertNotSame(request, captured[0]);
        assertEquals(NODE_ID, captured[0].getAttribute("actor"));
        assertEquals(Boolean.TRUE, captured[0].getAttribute("nodeAuth"));
        assertNotNull(capturedAuth[0]);
        assertEquals(NODE_ID, capturedAuth[0].getPrincipal());
        // Context is cleared once the chain unwinds
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void revokedNode_refusesWith403() throws Exception {
        Node revoked = new Node();
        revoked.setId(NODE_ID);
        revoked.setPubkey(PUBKEY);
        revoked.setStatus("revoked");
        revoked.setRevokedAt(Instant.now());
        when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(revoked));
        stubCallbackPath("POST", "{}");
        when(request.getHeader("X-Node-Signature")).thenReturn("anything");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void consolePath_withoutSignature_passesThroughToJwtFilter() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/nodes");
        when(request.getMethod()).thenReturn("GET");

        filter.doFilterInternal(request, response, filterChain);

        // Same request instance handed on — no signature demanded, no node context set
        verify(filterChain).doFilter(same(request), same(response));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void consolePathById_withoutSignature_passesThroughToJwtFilter() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/nodes/" + NODE_ID);
        when(request.getMethod()).thenReturn("GET");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(same(request), same(response));
    }

    @Test
    void enrollPath_isPublicAndPassesThrough() throws Exception {
        when(request.getRequestURI()).thenReturn("/api/nodes/enroll");
        when(request.getMethod()).thenReturn("POST");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(same(request), same(response));
    }

    private void stubCallbackPath(String method, String body) throws IOException {
        String path = "/api/nodes/" + NODE_ID + "/heartbeat";
        when(request.getRequestURI()).thenReturn(path);
        when(request.getMethod()).thenReturn(method);
        byte[] bytes = body != null ? body.getBytes(StandardCharsets.UTF_8) : new byte[0];
        when(request.getInputStream()).thenReturn(streamOf(bytes));
    }

    private static String sign(String method, String path, String body) throws Exception {
        MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha256.digest(PUBKEY.getBytes(StandardCharsets.UTF_8));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));
        byte[] hash = mac.doFinal((method + ":" + path + ":" + body).getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static byte[] readAll(ServletInputStream in) throws IOException {
        return in.readAllBytes();
    }

    private static ServletInputStream streamOf(byte[] bytes) {
        ByteArrayInputStream delegate = new ByteArrayInputStream(bytes);
        return new ServletInputStream() {
            @Override public boolean isFinished() { return delegate.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener readListener) { throw new UnsupportedOperationException(); }
            @Override public int read() { return delegate.read(); }
        };
    }
}
