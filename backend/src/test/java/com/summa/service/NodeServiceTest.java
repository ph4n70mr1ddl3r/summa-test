package com.summa.service;

import com.summa.repository.NodeRepository;
import com.summa.repository.RunRepository;
import com.summa.repository.SpendLedgerRepository;
import com.summa.repository.WorkspaceRepository;
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
import com.summa.exception.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class NodeServiceTest {

    @Mock
    private NodeRepository nodeRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private WorkspaceService workspaceService;

    @Mock
    private AskService askService;

    @Mock
    private RunRepository runRepository;

    @Mock
    private SpendLedgerRepository spendLedgerRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    private NodeService nodeService;

    @BeforeEach
    void setUp() throws Exception {
        nodeService = new NodeService(nodeRepository, auditService, workspaceService, askService,
            runRepository, spendLedgerRepository, workspaceRepository, new ObjectMapper());
        java.lang.reflect.Field f = NodeService.class.getDeclaredField("leaseIntervalSeconds");
        f.setAccessible(true);
        f.setLong(nodeService, 30L);
    }

    @Test
    void enroll_defaultsKindToRemote() {
        Node node = new Node();
        node.setId("node-1");
        node.setKind("local");
        when(nodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Node result = nodeService.enroll("Dev Box", "local", "pubkey-1", "admin");

        assertNotNull(result);
        assertEquals("local", result.getKind());
        assertNotNull(result.getEnrolledAt());
    }

    @Test
    void enroll_defaultsKindWhenNull() {
        when(nodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Node result = nodeService.enroll("Remote Box", null, "pubkey-2", "admin");

        assertEquals("remote", result.getKind());
    }

    @Test
    void heartbeat_updatesNode() {
        Node node = new Node();
        node.setId("node-1");
        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(nodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Node result = nodeService.heartbeat("node-1", "node-1", "{\"cpu\":4}");

        assertEquals("{\"cpu\":4}", result.getCapabilities());
        assertNotNull(result.getLastHeartbeat());
    }

    @Test
    void heartbeat_auditDetailEmbedsCapabilitiesAsJsonValue() throws Exception {
        Node node = new Node();
        node.setId("node-1");
        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(nodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        nodeService.heartbeat("node-1", "node-1", "{\"cpu\":4,\"note\":\"quoted \\\"x\\\"\"}");

        org.mockito.ArgumentCaptor<String> detail = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(auditService).logWithNode(eq("node-1"), eq("HEARTBEAT"), eq("node"), eq("node-1"), eq("node-1"), detail.capture());
        // The detail must parse as JSON with capabilities as an embedded object,
        // even when the capabilities payload itself contains quotes.
        com.fasterxml.jackson.databind.JsonNode parsed = new ObjectMapper().readTree(detail.getValue());
        assertTrue(parsed.has("capabilities"));
        assertTrue(parsed.get("capabilities").isObject());
        assertEquals(4, parsed.get("capabilities").get("cpu").asInt());
    }

    @Test
    void heartbeat_skipsNullCapabilities() {
        Node node = new Node();
        node.setId("node-1");
        node.setCapabilities("{}");
        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(nodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Node result = nodeService.heartbeat("node-1", "node-1", null);

        assertEquals("{}", result.getCapabilities());
    }

    @Test
    void heartbeat_throwsWhenNotFound() {
        when(nodeRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> {
            nodeService.heartbeat("missing", "missing", "{}");
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
        when(workspaceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Node result = nodeService.claimWorkspace("node-1", "ws-1", 1);

        assertEquals(1, ws.getClaimEpoch());
        assertNotNull(ws.getLeaseExpiresAt());
        assertNotNull(result.getClaim());
    }

    @Test
    void claimWorkspace_mergesIntoExistingClaimArray() throws Exception {
        // ARC-020: a node may hold live claims on several workspaces; claiming a
        // second workspace must not drop the first claim from the node's array.
        Node node = new Node();
        node.setId("node-1");
        node.setStatus("trusted");
        node.setClaim("[{\"workspaceId\":\"ws-1\",\"epoch\":1,\"expiresAt\":100}]");

        Workspace ws2 = new Workspace();
        ws2.setId("ws-2");
        ws2.setClaimEpoch(0);

        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(workspaceService.findById("ws-2")).thenReturn(Optional.of(ws2));
        when(nodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(workspaceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Node result = nodeService.claimWorkspace("node-1", "ws-2", 1);

        com.fasterxml.jackson.databind.JsonNode claim = new ObjectMapper().readTree(result.getClaim());
        assertTrue(claim.isArray());
        assertEquals(2, claim.size());
        assertEquals("ws-1", claim.get(0).get("workspaceId").asText());
        assertEquals("ws-2", claim.get(1).get("workspaceId").asText());
        assertEquals(1, claim.get(1).get("epoch").asInt());
        // The stored entry must carry the actual lease expiry, not the claim instant.
        assertEquals(ws2.getLeaseExpiresAt().getEpochSecond(), claim.get(1).get("expiresAt").asLong());
    }

    @Test
    void claimWorkspace_renewalReplacesPriorEntryForSameWorkspace() throws Exception {
        Node node = new Node();
        node.setId("node-1");
        node.setStatus("trusted");
        node.setClaim("[{\"workspaceId\":\"ws-1\",\"epoch\":1,\"expiresAt\":100},{\"workspaceId\":\"ws-2\",\"epoch\":4,\"expiresAt\":100}]");

        Workspace ws1 = new Workspace();
        ws1.setId("ws-1");
        ws1.setClaimEpoch(1);

        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(workspaceService.findById("ws-1")).thenReturn(Optional.of(ws1));
        when(nodeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(workspaceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Node result = nodeService.claimWorkspace("node-1", "ws-1", 2);

        com.fasterxml.jackson.databind.JsonNode claim = new ObjectMapper().readTree(result.getClaim());
        assertEquals(2, claim.size());
        // ws-2 entry preserved, ws-1 entry renewed at the new epoch
        java.util.List<String> ids = new java.util.ArrayList<>();
        claim.forEach(c -> ids.add(c.get("workspaceId").asText()));
        assertTrue(ids.contains("ws-2"));
        assertEquals(2, claim.get(ids.indexOf("ws-1")).get("epoch").asInt());
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
        run.setStatus("running");
        run.setWorkspaceId("ws-1");
        run.setAgentId("agent-1");
        run.setStatus("queued");

        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(workspaceService.findByNode("node-1")).thenReturn(List.of(ws));
        when(runRepository.findByWorkspaceIdAndStatus("ws-1", "queued")).thenReturn(List.of(run));

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
        run.setWorkspaceId("ws-1");
        run.setAgentId("agent-1");

        Workspace ws = new Workspace();
        ws.setId("ws-1");
        ws.setLeaseExpiresAt(java.time.Instant.now().plusSeconds(30));

        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(runRepository.findById("run-1")).thenReturn(Optional.of(run));
        when(workspaceService.findByNode("node-1")).thenReturn(List.of(ws));
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
        run.setWorkspaceId("ws-1");

        Workspace ws = new Workspace();
        ws.setId("ws-1");
        ws.setLeaseExpiresAt(java.time.Instant.now().plusSeconds(30));

        when(nodeRepository.findById("node-1")).thenReturn(Optional.of(node));
        when(runRepository.findById("run-1")).thenReturn(Optional.of(run));
        when(workspaceService.findByNode("node-1")).thenReturn(List.of(ws));
        when(runRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        nodeService.reportRun("node-1", "run-1", null, null, 0L, 0.0, "agent-1");

        verify(spendLedgerRepository, never()).save(any());
    }
}
