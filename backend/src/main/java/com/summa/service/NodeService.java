package com.summa.service;

import com.summa.repository.NodeRepository;
import com.summa.repository.AskRepository;
import com.summa.repository.RunRepository;
import com.summa.repository.SpendLedgerRepository;
import com.summa.model.Node;
import com.summa.model.Ask;
import com.summa.model.Workspace;
import com.summa.model.Run;
import com.summa.model.SpendLedger;
import com.summa.service.WorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.security.SecureRandom;
import java.util.Base64;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
public class NodeService {
    private static final long ENROLLMENT_TOKEN_TTL_SECONDS = 3600L; // 1 hour
    private static final long DEFAULT_LEASE_INTERVAL_SECONDS = 300L; // 5 minutes

    private final NodeRepository nodeRepository;
    private final AuditService auditService;
    private final WorkspaceService workspaceService;
    private final AskRepository askRepository;
    private final RunRepository runRepository;
    private final SpendLedgerRepository spendLedgerRepository;
    private final ObjectMapper objectMapper;

    public NodeService(NodeRepository nodeRepository, AuditService auditService,
                        WorkspaceService workspaceService, AskRepository askRepository,
                        RunRepository runRepository,
                        SpendLedgerRepository spendLedgerRepository,
                        ObjectMapper objectMapper) {
        this.nodeRepository = nodeRepository;
        this.auditService = auditService;
        this.workspaceService = workspaceService;
        this.askRepository = askRepository;
        this.runRepository = runRepository;
        this.spendLedgerRepository = spendLedgerRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Node enroll(String name, String kind, String pubkey) {
        Node node = new Node();
        node.setId(UUID.randomUUID().toString());
        node.setName(name);
        node.setKind(kind != null ? kind : "remote");
        node.setPubkey(pubkey);
        node.setCapabilities("{}");
        node.setEnrolledAt(Instant.now());

        Node saved = nodeRepository.save(node);
        auditService.log("system", "ENROLL", "node", node.getId(),
            String.format("{\"name\":\"%s\",\"kind\":\"%s\",\"token_expires_at\":%d}",
                name, kind, Instant.now().getEpochSecond() + ENROLLMENT_TOKEN_TTL_SECONDS));
        return saved;
    }

    public long getEnrollmentTokenTtlSeconds() {
        return ENROLLMENT_TOKEN_TTL_SECONDS;
    }

    public Optional<Node> findById(String id) {
        return nodeRepository.findById(id);
    }

    public Optional<Node> findByPubkey(String pubkey) {
        return nodeRepository.findByPubkey(pubkey);
    }

    public String generateEnrollmentToken(String nodeId) {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public List<Node> findAll() {
        return nodeRepository.findAll();
    }

    @Transactional
    public Node heartbeat(String id, String capabilities) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Node not found: " + id));
        
        node.setLastHeartbeat(Instant.now());
        if (capabilities != null) {
            node.setCapabilities(capabilities);
        }
        
        Node saved = nodeRepository.save(node);
        return saved;
    }

    @Transactional
    public Node revoke(String id, String actor) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Node not found: " + id));

        node.setRevokedAt(Instant.now());
        node.setStatus("revoked");

        // ARC-016(b): surface rebind asks for every workspace still bound to the revoked node
        for (Workspace ws : workspaceService.findByNode(id)) {
            Ask rebindAsk = new Ask();
            rebindAsk.setId(UUID.randomUUID().toString());
            rebindAsk.setKind("question");
            rebindAsk.setFrom("system");
            rebindAsk.setTo(OffboardingWalkService.ADMIN_BROADCAST);
            rebindAsk.setPayload(String.format("{\"nodeId\":\"%s\",\"workspaceId\":\"%s\",\"reason\":\"node_revoked\"}", id, ws.getId()));
            rebindAsk.setSlaTier("bulk");
            rebindAsk.setExpiryBehavior("escalate");
            rebindAsk.setQuorumRequired(1);
            rebindAsk.setDeadline(Instant.now().plusSeconds(7L * 86400));
            rebindAsk.setWorkspaceId(ws.getId());
            askRepository.save(rebindAsk);
            auditService.logSystem("REBIND_ASK", "workspace", ws.getId(),
                String.format("{\"nodeId\":\"%s\",\"reason\":\"node_revoked\"}", id));
        }

        Node saved = nodeRepository.save(node);
        auditService.log(actor, "REVOKE", "node", id, null);
        return saved;
    }

    @Transactional
    public Node updateMetadata(String id, String name, String region, String actor) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Node not found: " + id));
        
        if (name != null) {
            node.setName(name);
        }
        if (region != null) {
            node.setRegion(region);
        }
        
        Node saved = nodeRepository.save(node);
        auditService.log(actor, "UPDATE", "node", id, null);
        return saved;
    }

    public boolean isRevoked(String id) {
        return nodeRepository.findById(id)
                .map(Node::isRevoked)
                .orElse(false);
    }

    // ARC-020: Acquire or renew a workspace claim as an epoch-fenced lease.
    // A stale epoch is refused at this mediated boundary (ARC-024).
    @Transactional
    public Node claimWorkspace(String nodeId, String workspaceId, int currentEpoch) {
        Node node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Node not found: " + nodeId));
        if (node.isRevoked()) {
            throw new IllegalStateException("Node is revoked");
        }

        Workspace ws = workspaceService.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found: " + workspaceId));
        if (ws.isArchived()) {
            throw new IllegalStateException("Workspace is archived");
        }

        // ARC-024: refuse stale epoch
        if (ws.getClaimEpoch() != null && ws.getClaimEpoch() > currentEpoch) {
            throw new IllegalStateException(
                String.format("Stale epoch: workspace epoch=%d, claimed=%d", ws.getClaimEpoch(), currentEpoch));
        }

        // Bump epoch and set lease expiry
        int newEpoch = ws.getClaimEpoch() != null ? ws.getClaimEpoch() + 1 : 1;
        ws.setClaimEpoch(newEpoch);
        ws.setLeaseExpiresAt(Instant.now().plusSeconds(DEFAULT_LEASE_INTERVAL_SECONDS));
        workspaceService.updateWorkspace(ws);

        // Store claim on node
        try {
            String claimJson = objectMapper.writeValueAsString(
                java.util.Map.of("workspaceId", workspaceId, "epoch", newEpoch,
                    "expiresAt", Instant.now().getEpochSecond()));
            node.setClaim(claimJson);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize claim: " + e.getMessage());
        }

        Node saved = nodeRepository.save(node);
        auditService.logWithNode("system", "CLAIM", "workspace", workspaceId, nodeId,
            String.format("{\"epoch\":%d}", newEpoch));
        return saved;
    }

    // API-060: Fetch queued runs for workspaces the node holds a live claim on.
    @Transactional(readOnly = true)
    public List<Run> pullWork(String nodeId) {
        Node node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Node not found: " + nodeId));
        if (node.isRevoked()) {
            throw new IllegalStateException("Node is revoked");
        }

        // Find workspaces bound to this node with live claims
        List<Workspace> claimedWorkspaces = workspaceService.findByNode(nodeId).stream()
                .filter(ws -> ws.getLeaseExpiresAt() != null && ws.getLeaseExpiresAt().isAfter(Instant.now()))
                .toList();

        List<String> workspaceIds = claimedWorkspaces.stream()
                .map(Workspace::getId)
                .toList();

        if (workspaceIds.isEmpty()) {
            return List.of();
        }

        // Return queued runs for those workspaces
        List<Run> result = new java.util.ArrayList<>();
        for (String wsId : workspaceIds) {
            result.addAll(runRepository.findByWorkspaceId(wsId).stream()
                    .filter(r -> "queued".equals(r.getStatus()))
                    .toList());
        }
        return result;
    }

    // API-060: Land run results, artifacts, and spend ledger lines.
    @Transactional
    public Run reportRun(String nodeId, String runId, String result, String artifacts,
                          long costTokens, double costUsd, String memberId) {
        Node node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Node not found: " + nodeId));
        if (node.isRevoked()) {
            throw new IllegalStateException("Node is revoked");
        }

        Run run = runRepository.findById(runId)
                .orElseThrow(() -> new IllegalArgumentException("Run not found: " + runId));

        run.setResult(result);
        run.setArtifacts(artifacts != null ? artifacts : "[]");
        run.setStatus("completed");
        run.setCompletedAt(Instant.now());
        run.setCostTokens(costTokens);
        run.setCostUsd(costUsd);
        Run saved = runRepository.save(run);

        // Land spend ledger line (DAT-100)
        if (memberId != null && !memberId.isBlank() && costUsd > 0) {
            SpendLedger ledger = new SpendLedger();
            ledger.setId(UUID.randomUUID().toString());
            ledger.setMemberId(memberId);
            ledger.setRunId(runId);
            ledger.setKind("settle");
            ledger.setCost(costUsd);
            ledger.setTokensOut((double) costTokens);
            ledgerRepositorySave(ledger);
        }

        auditService.logWithNode("system", "REPORT_RUN", "run", runId, nodeId,
            String.format("{\"costUsd\":%.4f,\"costTokens\":%d}", costUsd, costTokens));
        return saved;
    }

    private void ledgerRepositorySave(SpendLedger ledger) {
        spendLedgerRepository.save(ledger);
    }
}
