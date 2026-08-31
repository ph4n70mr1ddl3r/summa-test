package com.summa.service;

import com.summa.repository.MemoryItemRepository;
import com.summa.model.MemoryItem;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final DnaDomainService domainService;
    private final MemberService memberService;
    private final WorkspaceService workspaceService;
    private final ObjectMapper objectMapper;

    public MemoryService(MemoryItemRepository memoryItemRepository, AuditService auditService,
                           DnaDomainService domainService, MemberService memberService,
                           WorkspaceService workspaceService, ObjectMapper objectMapper) {
        this.memoryItemRepository = memoryItemRepository;
        this.auditService = auditService;
        this.domainService = domainService;
        this.memberService = memberService;
        this.workspaceService = workspaceService;
        this.objectMapper = objectMapper;
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

        // SUB-041: taint clearance — authority depends on tier
        if ("personal".equals(item.getTier())) {
            // Personal-tier: only the spawner's owner (memberId) can clear taint
            if (item.getMemberId() == null || !item.getMemberId().equals(reviewerId)) {
                throw new IllegalStateException("Only the owner can review personal-tier memory");
            }
        } else if ("project".equals(item.getTier())) {
            // Project-tier: only the domain owner can clear taint
            if (item.getWorkspaceId() == null) {
                throw new IllegalStateException("Project-tier memory requires a workspace for review");
            }
            // Find the workspace and check if reviewer owns any of its domains
            boolean isDomainOwner = false;
            Optional<com.summa.model.Workspace> wsOpt = workspaceService.findById(item.getWorkspaceId());
            if (wsOpt.isPresent()) {
                com.summa.model.Workspace ws = wsOpt.get();
                try {
                    List<String> domainIds = objectMapper.readValue(
                        ws.getDomainIds(),
                        new TypeReference<List<String>>() {});
                    for (String domId : domainIds) {
                        Optional<com.summa.model.DnaDomain> domainOpt = domainService.findById(domId);
                        if (domainOpt.isPresent() && domainOpt.get().getOwnerHumanId().equals(reviewerId)) {
                            isDomainOwner = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    // If domainIds parsing fails, fall through to admin check
                }
            }
            // Also check if reviewer is admin (admins can review any tier)
            boolean isAdmin = memberService.findAdmins().stream()
                    .anyMatch(h -> h.getId().equals(reviewerId));
            if (!isDomainOwner && !isAdmin) {
                throw new IllegalStateException("Only the domain owner or an admin can review project-tier memory");
            }
        }
        // proposal-tier: any reviewer with write access can clear (no additional gate)

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
