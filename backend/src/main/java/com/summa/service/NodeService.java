package com.summa.service;

import com.summa.repository.NodeRepository;
import com.summa.repository.AskRepository;
import com.summa.model.Node;
import com.summa.model.Ask;
import com.summa.service.WorkspaceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class NodeService {
    private static final long ENROLLMENT_TOKEN_TTL_SECONDS = 3600L; // 1 hour

    private final NodeRepository nodeRepository;
    private final AuditService auditService;
    private final WorkspaceService workspaceService;
    private final AskRepository askRepository;

    public NodeService(NodeRepository nodeRepository, AuditService auditService,
                       WorkspaceService workspaceService, AskRepository askRepository) {
        this.nodeRepository = nodeRepository;
        this.auditService = auditService;
        this.workspaceService = workspaceService;
        this.askRepository = askRepository;
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
        for (com.summa.model.Workspace ws : workspaceService.findByNode(id)) {
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
}
