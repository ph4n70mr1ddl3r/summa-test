package com.summa.service;

import com.summa.repository.NodeRepository;
import com.summa.repository.AskRepository;
import com.summa.repository.RunRepository;
import com.summa.repository.SpendLedgerRepository;
import com.summa.model.Node;
import com.summa.model.Workspace;
import com.summa.model.Run;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NodeServiceTest {

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private AskRepository askRepository;

    @Mock
    private RunRepository runRepository;

    @Mock
    private SpendLedgerRepository spendLedgerRepository;

    private NodeService nodeService;

    @BeforeEach
    void setUp() {
        nodeService = new NodeService(nodeRepository, auditService, workspaceService, askRepository,
            runRepository, spendLedgerRepository, new ObjectMapper());
    }

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
        when(workspaceService.findByNode("node-1")).thenReturn(List.of());

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

    @Test
    void claimWorkspace_bumpsEpoch() {
        Node node = new Node();
        node.setId("node-1");
        node.setStatus("trusted");

        Workspace ws = new Workspace();
        ws.setId("ws-1");
        ws.setClaimEpoch(0);
        ws.setDomainIds("[\"dom-1\"]");

        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(workspaceService.findById("ws-1")).thenReturn(Optional.of(ws));
        when(nodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        doReturn(ws).when(workspaceService).updateWorkspace(any());

        Node result = nodeService.claimWorkspace("node-1", "ws-1", 0);

        assertEquals(1, ws.getClaimEpoch());
        assertNotNull(ws.getLeaseExpiresAt());
        assertNotNull(result.getClaim());
    }

    @Test
    void claimWorkspace_refusesStaleEpoch() {
        Node node = new Node();
        node.setId("node-1");
        node.setStatus("trusted");

        Workspace ws = new Workspace();
        ws.setId("ws-1");
        ws.setClaimEpoch(5);

        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(workspaceService.findById("ws-1")).thenReturn(Optional.of(ws));

        assertThrows(IllegalStateException.class, () -> {
            nodeService.claimWorkspace("node-1", "ws-1", 3);
        });
    }

    @Test
    void claimWorkspace_refusesRevokedNode() {
        Node node = new Node();
        node.setId("node-1");
        node.setStatus("revoked");

        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));

        assertThrows(IllegalStateException.class, () -> {
            nodeService.claimWorkspace("node-1", "ws-1", 0);
        });
    }

    @Test
    void pullWork_returnsQueuedRuns() {
        Node node = new Node();
        node.setId("node-1");
        node.setStatus("trusted");

        Workspace ws = new Workspace();
        ws.setId("ws-1");
        ws.setNodeId("node-1");
        ws.setLeaseExpiresAt(java.time.Instant.now().plusSeconds(60));

        Run run = new Run();
        run.setId("run-1");
        run.setWorkspaceId("ws-1");
        run.setStatus("queued");

        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(workspaceService.findByNode("node-1")).thenReturn(List.of(ws));
        when(runRepository.findByWorkspaceId("ws-1")).thenReturn(List.of(run));

        List<Run> result = nodeService.pullWork("node-1");

        assertEquals(1, result.size());
        assertEquals("run-1", result.get(0).getId());
    }

    @Test
    void pullWork_refusesRevokedNode() {
        Node node = new Node();
        node.setId("node-1");
        node.setStatus("revoked");

        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));

        assertThrows(IllegalStateException.class, () -> {
            nodeService.pullWork("node-1");
        });
    }

    @Test
    void reportRun_completesRunAndLogsSpend() {
        Node node = new Node();
        node.setId("node-1");
        node.setStatus("trusted");

        Run run = new Run();
        run.setId("run-1");
        run.setStatus("running");

        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(runRepository.findById("run-1")).thenReturn(Optional.of(run));
        when(runRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(spendLedgerRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Run result = nodeService.reportRun("node-1", "run-1", "done", "[\"art1\"]", 100L, 0.5, "agent-1");

        assertEquals("completed", result.getStatus());
        assertEquals("done", result.getResult());
        assertEquals(0.5, result.getCostUsd());
        verify(spendLedgerRepository).save(any());
    }

    @Test
    void reportRun_skipsSpendWhenZeroCost() {
        Node node = new Node();
        node.setId("node-1");
        node.setStatus("trusted");

        Run run = new Run();
        run.setId("run-1");
        run.setStatus("running");

        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(runRepository.findById("run-1")).thenReturn(Optional.of(run));
        when(runRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        nodeService.reportRun("node-1", "run-1", null, null, 0L, 0.0, "agent-1");

        verify(spendLedgerRepository, never()).save(any());
    }
}
