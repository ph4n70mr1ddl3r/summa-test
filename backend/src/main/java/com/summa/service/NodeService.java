package com.summa.service;

import com.summa.repository.NodeRepository;
import com.summa.repository.RunRepository;
import com.summa.repository.SpendLedgerRepository;
import com.summa.repository.WorkspaceRepository;
import com.summa.model.Node;
import com.summa.model.Workspace;
import com.summa.model.Run;
import com.summa.model.SpendLedger;
import com.summa.service.WorkspaceService;
import com.summa.constants.Defaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.security.SecureRandom;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.summa.exception.EntityNotFoundException;
import com.summa.util.JsonHelpers;

@Service
public class NodeService {
    @Value("${summa.node.lease-interval-seconds:30}")
    private long leaseIntervalSeconds;

    private final NodeRepository nodeRepository;
    private final AuditService auditService;
    private final WorkspaceService workspaceService;
    private final AskService askService;
    private final RunRepository runRepository;
    private final SpendLedgerRepository spendLedgerRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ObjectMapper objectMapper;

    public NodeService(NodeRepository nodeRepository, AuditService auditService,
                        WorkspaceService workspaceService, AskService askService,
                        RunRepository runRepository,
                        SpendLedgerRepository spendLedgerRepository,
                        WorkspaceRepository workspaceRepository,
                        ObjectMapper objectMapper) {
        this.nodeRepository = nodeRepository;
        this.auditService = auditService;
        this.workspaceService = workspaceService;
        this.askService = askService;
        this.runRepository = runRepository;
        this.spendLedgerRepository = spendLedgerRepository;
        this.workspaceRepository = workspaceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Node enroll(String name, String kind, String pubkey, String actor) {
        if (pubkey == null || pubkey.isBlank()) {
            throw new IllegalArgumentException("pubkey is required");
        }
        // Prevent duplicate enrollment: reject if a non-revoked node already uses this pubkey
        Optional<Node> existing = nodeRepository.findByPubkey(pubkey);
        if (existing.isPresent() && !existing.get().isRevoked()) {
            throw new IllegalStateException("A non-revoked node already exists with this pubkey");
        }

        Node node = new Node();
        node.setId(UUID.randomUUID().toString());
        node.setName(name);
        node.setKind(kind != null ? kind : "remote");
        node.setPubkey(pubkey);
        node.setCapabilities("{}");
        node.setEnrolledAt(Instant.now());

        Node saved = nodeRepository.save(node);
        String auditActor = actor != null ? actor : Defaults.SYSTEM_ACTOR;
        auditService.log(auditActor, "ENROLL", "node", node.getId(),
            String.format("{\"name\":\"%s\",\"kind\":\"%s\",\"token_expires_at\":%d}",
                name, node.getKind(), Instant.now().getEpochSecond() + Defaults.ENROLLMENT_TOKEN_TTL_SECONDS));
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<Node> findById(String id) {
        return nodeRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Node> findByPubkey(String pubkey) {
        return nodeRepository.findByPubkey(pubkey);
    }

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Enrollment token is a single-use, node-scoped secret handed to the node at
     * enrollment time. The node id is part of the token payload so a leaked token
     * cannot be replayed against a different node's enrollment record.
     */
    public String generateEnrollmentToken(String nodeId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return nodeId + "." + nonce;
    }

    @Transactional(readOnly = true)
    public List<Node> findAll() {
        return nodeRepository.findAll();
    }

    @Transactional
    public Node heartbeat(String id, String actor, String capabilities) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Node not found: " + id));
        String actorClean = JsonHelpers.stripIdPrefix(actor);
        if (!id.equals(actorClean)) {
            throw new IllegalStateException("Node can only heartbeat its own record");
        }

        node.setLastHeartbeat(Instant.now());
        if (capabilities != null) {
            // Validate capabilities is parseable JSON before storing
            try {
                objectMapper.readTree(capabilities);
            } catch (Exception e) {
                throw new IllegalArgumentException("capabilities must be valid JSON: " + e.getMessage());
            }
            node.setCapabilities(capabilities);
        }

        Node saved = nodeRepository.save(node);
        auditService.logWithNode(actor, "HEARTBEAT", "node", id, actor,
            // capabilities is validated parseable JSON above — embed it as a JSON
            // value (not a raw-interpolated string) so the audit detail stays valid JSON.
            String.format("{\"capabilities\":%s}", capabilities != null ? capabilities : "{}"));
        return saved;
    }

    @Transactional
    public Node revoke(String id, String actor) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Node not found: " + id));

        node.setRevokedAt(Instant.now());
        node.setStatus("revoked");

        // ARC-016(b): surface rebind asks for every workspace still bound to the revoked node
        for (Workspace ws : workspaceService.findByNode(id)) {
            long deadlineSeconds = askService.deriveDeadlineFromTier("bulk");
            if (deadlineSeconds > Defaults.MAX_DEADLINE_SECONDS) deadlineSeconds = Defaults.MAX_DEADLINE_SECONDS;
            askService.create("question", "system", OffboardingWalkService.ADMIN_BROADCAST,
                String.format("{\"nodeId\":%s,\"workspaceId\":%s,\"reason\":\"node_revoked\"}", JsonHelpers.jsonString(id), JsonHelpers.jsonString(ws.getId())),
                "bulk", "escalate", 1,
                Instant.now().plusSeconds(deadlineSeconds), null, ws.getId());
            auditService.logSystem("REBIND_ASK", "workspace", ws.getId(),
                String.format("{\"nodeId\":%s,\"reason\":\"node_revoked\"}", JsonHelpers.jsonString(id)));
        }

        Node saved = nodeRepository.save(node);
        auditService.log(actor, "REVOKE", "node", id, null);
        return saved;
    }

    @Transactional
    public Node updateMetadata(String id, String name, String region, String actor) {
        Node node = nodeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Node not found: " + id));
        
        if (name != null) {
            node.setName(name);
        }
        if (region != null) {
            node.setRegion(region);
        }
        
        Node saved = nodeRepository.save(node);
        auditService.log(actor, "UPDATE_NODE_METADATA", "node", id, null);
        return saved;
    }

    @Transactional(readOnly = true)
    public boolean isRevoked(String id) {
        return nodeRepository.findById(id)
                .map(Node::isRevoked)
                .orElse(false);
    }

    // ARC-020: Acquire or renew a workspace claim as an epoch-fenced lease.
    // A stale epoch is refused at this mediated boundary (ARC-024).
    @Transactional
    public Node claimWorkspace(String nodeId, String workspaceId, int currentEpoch) {
        if (leaseIntervalSeconds <= 0) {
            throw new IllegalArgumentException("summa.node.lease-interval-seconds must be positive, got: " + leaseIntervalSeconds);
        }
        Node node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("Node not found: " + nodeId));
        if (node.isRevoked()) {
            throw new IllegalStateException("Node is revoked");
        }

        Workspace ws = workspaceService.findById(workspaceId)
                .orElseThrow(() -> new EntityNotFoundException("Workspace not found: " + workspaceId));
        if (ws.isArchived()) {
            throw new IllegalStateException("Workspace is archived");
        }

        // ARC-024: refuse stale or equal epoch (equal epoch = replay attack).
        // Re-read the workspace row immediately before the bump so a concurrent
        // claim that interleaved between the lookup above and the save cannot
        // race past the epoch check (SQLite/H2 cannot hold a row lock here).
        ws = workspaceRepository.findById(workspaceId).orElse(ws);
        // Null epoch on a fresh workspace is treated as epoch 0, so a replay with epoch=0 is also refused.
        int wsEpoch = ws.getClaimEpoch() != null ? ws.getClaimEpoch() : 0;
        if (wsEpoch >= currentEpoch) {
            throw new IllegalStateException(
                String.format("Stale epoch: workspace epoch=%d, claimed=%d", wsEpoch, currentEpoch));
        }

        // Bump epoch and set lease expiry
        int newEpoch = ws.getClaimEpoch() != null ? ws.getClaimEpoch() + 1 : 1;
        Instant leaseExpiry = Instant.now().plusSeconds(leaseIntervalSeconds);
        ws.setClaimEpoch(newEpoch);
        ws.setLeaseExpiresAt(leaseExpiry);
        workspaceRepository.save(ws);

        // Store the claim on the node as a per-workspace array (ARC-020): a node may
        // hold live claims on several workspaces at once, so merge this entry into
        // any existing array rather than replacing the whole claim. Renewing a claim
        // on the same workspace replaces its prior entry. WorkspaceService.archive
        // relies on this shape when it strips a single workspace's entry.
        try {
            ArrayNode claimArray = objectMapper.createArrayNode();
            if (node.getClaim() != null && !node.getClaim().isBlank()) {
                JsonNode existing = objectMapper.readTree(node.getClaim());
                if (existing.isArray()) {
                    for (JsonNode entry : existing) {
                        String entryWsId = entry.isObject() && entry.has("workspaceId")
                            ? entry.get("workspaceId").asText() : null;
                        if (entryWsId != null && !entryWsId.equals(workspaceId)) {
                            claimArray.add(entry);
                        }
                    }
                }
            }
            claimArray.add(objectMapper.createObjectNode()
                .put("workspaceId", workspaceId)
                .put("epoch", newEpoch)
                .put("expiresAt", leaseExpiry.getEpochSecond()));
            node.setClaim(objectMapper.writeValueAsString(claimArray));
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
                .orElseThrow(() -> new EntityNotFoundException("Node not found: " + nodeId));
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
        List<Run> result = new ArrayList<>();
        for (String wsId : workspaceIds) {
            result.addAll(runRepository.findByWorkspaceIdAndStatus(wsId, "queued"));
        }
        return result;
    }

    // API-060: Land run results, artifacts, and spend ledger lines.
    @Transactional
    public Run reportRun(String nodeId, String runId, String result, String artifacts,
                           long costTokens, double costUsd, String memberId) {
        Node node = nodeRepository.findById(nodeId)
                .orElseThrow(() -> new EntityNotFoundException("Node not found: " + nodeId));
        if (node.isRevoked()) {
            throw new IllegalStateException("Node is revoked");
        }

        Run run = runRepository.findById(runId)
                .orElseThrow(() -> new EntityNotFoundException("Run not found: " + runId));

        // Verify the node holds a live claim on the run's workspace
        List<Workspace> claimedWorkspaces = workspaceService.findByNode(nodeId).stream()
                .filter(ws -> ws.getLeaseExpiresAt() != null && ws.getLeaseExpiresAt().isAfter(Instant.now()))
                .toList();
        boolean ownsWorkspace = claimedWorkspaces.stream()
                .anyMatch(ws -> ws.getId().equals(run.getWorkspaceId()));
        if (!ownsWorkspace) {
            throw new IllegalStateException(
                "Node does not hold a live claim on workspace: " + run.getWorkspaceId());
        }

        run.setResult(result);
        run.setArtifacts(artifacts != null ? artifacts : "[]");
        run.setStatus("completed");
        run.setCompletedAt(Instant.now());
        run.setCostTokens(costTokens);
        run.setCostUsd(costUsd);
        Run saved = runRepository.save(run);

        // Land spend ledger line (DAT-100). Strip any keyed-union prefix so the
        // ledger always stores the bare member id, matching the agents(id) FK
        // and the acknowledge/ack paths' strip-comparison.
        String cleanMemberId = JsonHelpers.stripIdPrefix(memberId);
        if (cleanMemberId != null && !cleanMemberId.isBlank() && costUsd > 0) {
            SpendLedger ledger = new SpendLedger();
            ledger.setId(UUID.randomUUID().toString());
            ledger.setMemberId(cleanMemberId);
            ledger.setRunId(runId);
            ledger.setKind("settle");
            ledger.setCost(costUsd);
            ledger.setTokensOut(costTokens);
            spendLedgerRepository.save(ledger);
        }

        auditService.logWithNode("system", "REPORT_RUN", "run", runId, nodeId,
            String.format("{\"costUsd\":%.4f,\"costTokens\":%d}", costUsd, costTokens));
        return saved;
    }
}
