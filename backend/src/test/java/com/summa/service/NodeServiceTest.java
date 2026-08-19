package com.summa.service;

import com.summa.repository.NodeRepository;
import com.summa.model.Node;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NodeServiceTest {

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private NodeService nodeService;

    @Test
    void enroll_defaultsKindToRemote() {
        Node node = new Node();
        node.setId("node-1");
        node.setKind("local");
        when(nodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Node result = nodeService.enroll("Dev Box", "local", "pubkey-1");

        assertNotNull(result);
        assertEquals("local", result.getKind());
        assertNotNull(result.getEnrolledAt());
    }

    @Test
    void enroll_defaultsKindWhenNull() {
        when(nodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Node result = nodeService.enroll("Remote Box", null, "pubkey-2");

        assertEquals("remote", result.getKind());
    }

    @Test
    void heartbeat_updatesNode() {
        Node node = new Node();
        node.setId("node-1");
        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(nodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Node result = nodeService.heartbeat("node-1", "{\"cpu\":4}");

        assertEquals("{\"cpu\":4}", result.getCapabilities());
        assertNotNull(result.getLastHeartbeat());
    }

    @Test
    void heartbeat_skipsNullCapabilities() {
        Node node = new Node();
        node.setId("node-1");
        node.setCapabilities("{}");
        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(nodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Node result = nodeService.heartbeat("node-1", null);

        assertEquals("{}", result.getCapabilities());
    }

    @Test
    void heartbeat_throwsWhenNotFound() {
        when(nodeRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            nodeService.heartbeat("missing", "{}");
        });
    }

    @Test
    void revoke_node() {
        Node node = new Node();
        node.setId("node-1");
        node.setStatus("trusted");
        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(nodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Node result = nodeService.revoke("node-1", "admin");

        assertEquals("revoked", result.getStatus());
        assertNotNull(result.getRevokedAt());
    }

    @Test
    void updateMetadata_setsFields() {
        Node node = new Node();
        node.setId("node-1");
        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(nodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Node result = nodeService.updateMetadata("node-1", "New Name", "us-east", "admin");

        assertEquals("New Name", result.getName());
        assertEquals("us-east", result.getRegion());
    }

    @Test
    void updateMetadata_skipsNullFields() {
        Node node = new Node();
        node.setId("node-1");
        node.setName("Old Name");
        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(nodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Node result = nodeService.updateMetadata("node-1", null, null, "admin");

        assertEquals("Old Name", result.getName());
    }

    @Test
    void isRevoked_true() {
        Node node = new Node();
        node.setStatus("revoked");
        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));

        assertTrue(nodeService.isRevoked("node-1"));
    }

    @Test
    void isRevoked_false() {
        when(nodeRepository.findById("node-1")).thenReturn(Optional.empty());

        assertFalse(nodeService.isRevoked("node-1"));
    }
}
