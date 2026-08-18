package com.summa.service;

import com.summa.repository.MemoryItemRepository;
import com.summa.model.MemoryItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MemoryService {
    private final MemoryItemRepository memoryItemRepository;
    private final AuditService auditService;

    public MemoryService(MemoryItemRepository memoryItemRepository, AuditService auditService) {
        this.memoryItemRepository = memoryItemRepository;
        this.auditService = auditService;
    }

    @Transactional
    public MemoryItem create(String tier, String memberId, String workspaceId, 
                              String contentMd, String provenance, boolean tainted) {
        MemoryItem item = new MemoryItem();
        item.setId(UUID.randomUUID().toString());
        item.setTier(tier);
        item.setMemberId(memberId);
        item.setWorkspaceId(workspaceId);
        item.setContentMd(contentMd);
        item.setProvenance(provenance != null ? provenance : "{}");
        item.setTainted(tainted);

        MemoryItem saved = memoryItemRepository.save(item);
        auditService.log(memberId != null ? memberId : "system", "CREATE_MEMORY", "memory_item", 
            saved.getId(), String.format("{\"tier\":\"%s\",\"tainted\":%b}", tier, tainted));
        return saved;
    }

    public Optional<MemoryItem> findById(String id) {
        return memoryItemRepository.findById(id);
    }

    public List<MemoryItem> findByMember(String memberId) {
        return memoryItemRepository.findByMemberId(memberId);
    }

    public List<MemoryItem> findByWorkspace(String workspaceId) {
        return memoryItemRepository.findByWorkspaceId(workspaceId);
    }

    public List<MemoryItem> findAll() {
        return memoryItemRepository.findAll();
    }

    public List<MemoryItem> findTainted() {
        return memoryItemRepository.findByTaintedTrue();
    }

    @Transactional
    public MemoryItem review(String id, String reviewerId) {
        MemoryItem item = memoryItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Memory item not found: " + id));

        item.setReviewedBy(reviewerId);
        item.setReviewedAt(Instant.now());
        item.setTainted(false);

        MemoryItem saved = memoryItemRepository.save(item);
        auditService.log(reviewerId, "REVIEW_MEMORY", "memory_item", id, null);
        return saved;
    }

    public long countByTier(String tier) {
        return memoryItemRepository.countByTier(tier);
    }
}
