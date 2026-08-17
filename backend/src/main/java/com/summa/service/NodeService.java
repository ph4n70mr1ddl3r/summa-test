package com.summa.service;

import com.summa.repository.NodeRepository;
import com.summa.model.Node;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NodeService {
    private final NodeRepository nodeRepository;
    private final AuditService auditService;

    public NodeService(NodeRepository nodeRepository, AuditService auditService) {
        this.nodeRepository = nodeRepository;
        this.auditService = auditService;
    }

    public Node enroll(String name, String kind, String pubkey) {
        Node node = new Node();
        node.setId(UUID.randomUUID().toString());
        node.setName(name);
        node.setKind(kind != null ? kind : "remote");
        node.setPubkey(pubkey);
        node.setCapabilities("{}");

        Node saved = nodeRepository.save(node);
        auditService.log("system", "ENROLL", "node", node.getId(), 
            String.format("{\"name\":\"%s\",\"kind\":\"%s\"}", name, kind));
        return saved;
    }

    public Optional<Node> findById(String id) {
        return nodeRepository.findById(id);
    }

    public Optional<Node> findByPubkey(String pubkey) {
        return nodeRepository.findByPubkey(pubkey);
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
