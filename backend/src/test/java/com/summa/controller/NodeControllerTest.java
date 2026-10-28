package com.summa.controller;

import com.summa.model.AuditEvent;
import com.summa.model.Node;
import com.summa.security.TestAuthContext;
import com.summa.security.WriteGate;
import com.summa.service.AuditService;
import com.summa.service.MemberService;
import com.summa.service.NodeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * SEC-012: node revocation is a trust-surface act. Every other node door
 * (list, view, enroll, update) is admin-gated — revocation must be too, or any
 * write-capable member can kill a node's keypair.
 */
class NodeControllerTest {

    private NodeService nodeService;
    private AuditService auditService;
    private MemberService memberService;
    private NodeController controller;

    @BeforeEach
    void setUp() {
        nodeService = mock(NodeService.class);
        auditService = mock(AuditService.class);
        memberService = mock(MemberService.class);
        controller = new NodeController(nodeService, auditService, new WriteGate(auditService), memberService);
        when(auditService.log(any(), any(), any(), any(), any())).thenReturn(newAuditEvent());
        when(auditService.logSystem(anyString(), anyString(), any(), any())).thenReturn(newAuditEvent());
    }

    @AfterEach
    void tearDown() {
        TestAuthContext.clear();
    }

    private AuditEvent newAuditEvent() {
        AuditEvent event = new AuditEvent();
        event.setId("audit-1");
        return event;
    }

    private String bodyMessage(org.springframework.http.ResponseEntity<?> res) {
        Object body = res.getBody();
        if (body instanceof Map<?, ?> map) {
            Object message = map.get("message");
            return message == null ? null : message.toString();
        }
        return null;
    }

    @Test
    void revoke_byNonAdmin_refused() {
        TestAuthContext.allowWrites("member-1");
        when(memberService.isAdmin("member-1")).thenReturn(false);

        var res = controller.revoke("node-1");

        assertEquals(403, res.getStatusCodeValue());
        assertTrue(bodyMessage(res).contains("Admin access required"));
        verify(nodeService, never()).revoke(anyString(), anyString());
    }

    @Test
    void revoke_byAdmin_allowed() {
        TestAuthContext.allowWrites("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);
        Node node = new Node();
        node.setId("node-1");
        node.setStatus("revoked");
        when(nodeService.revoke("node-1", "admin-1")).thenReturn(node);

        var res = controller.revoke("node-1");

        assertEquals(200, res.getStatusCodeValue());
        verify(nodeService).revoke("node-1", "admin-1");
    }

    @Test
    void enroll_withAnUnknownKind_isAValidationRefusal() {
        // API-061: validation before constraint — nodes.kind carries a CHECK, so
        // an unknown kind must be a 422, not an integrity abort.
        TestAuthContext.allowWrites("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);

        var res = controller.enroll(Map.of(
                "name", "edge-1",
                "kind", "edge",
                "pubkey", java.util.Base64.getEncoder().encodeToString(new byte[32])));

        assertEquals(422, res.getStatusCodeValue());
        assertTrue(bodyMessage(res).contains("kind"));
        verify(nodeService, never()).enroll(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void heartbeat_missingNode_returns404() {
        TestAuthContext.allowWrites("node-1");
        when(nodeService.heartbeat(eq("node-missing"), anyString(), any()))
            .thenThrow(new com.summa.exception.EntityNotFoundException("Node not found: node-missing"));

        var res = controller.heartbeat("node-missing", Map.of());

        assertEquals(404, res.getStatusCodeValue());
        assertTrue(bodyMessage(res).contains("Node not found: node-missing"));
    }

    @Test
    void revoke_missingNode_returns404() {
        TestAuthContext.allowWrites("admin-1");
        when(memberService.isAdmin("admin-1")).thenReturn(true);
        when(nodeService.revoke("node-missing", "admin-1"))
            .thenThrow(new com.summa.exception.EntityNotFoundException("Node not found: node-missing"));

        var res = controller.revoke("node-missing");

        assertEquals(404, res.getStatusCodeValue());
        assertTrue(bodyMessage(res).contains("Node not found: node-missing"));
    }
}
