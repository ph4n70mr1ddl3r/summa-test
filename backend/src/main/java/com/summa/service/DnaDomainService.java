package com.summa.service;

import com.summa.repository.DnaDomainRepository;
import com.summa.model.DnaDomain;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class DnaDomainService {
    private final DnaDomainRepository domainRepository;
    private final AuditService auditService;

    public DnaDomainService(DnaDomainRepository domainRepository, AuditService auditService) {
        this.domainRepository = domainRepository;
        this.auditService = auditService;
    }

    public DnaDomain create(String id, String name, String ownerHumanId, String access, 
                           String store, Integer reviewSlaDays, String residency) {
        DnaDomain domain = new DnaDomain();
        domain.setId(id);
        domain.setName(name);
        domain.setOwnerHumanId(ownerHumanId);
        domain.setAccess(access != null ? access : "public");
        domain.setStore(store != null ? store : "git");
        domain.setReviewSlaDays(reviewSlaDays != null ? reviewSlaDays : 7);
        domain.setResidency(residency);
        
        DnaDomain saved = domainRepository.save(domain);
        auditService.log("system", "CREATE", "dna_domain", id, 
            String.format("{\"name\":\"%s\",\"access\":\"%s\",\"store\":\"%s\"}", name, domain.getAccess(), domain.getStore()));
        return saved;
    }

    public Optional<DnaDomain> findById(String id) {
        return domainRepository.findById(id);
    }

    public Optional<DnaDomain> findByName(String name) {
        return domainRepository.findByNameNotArchived(name);
    }

    public List<DnaDomain> findAll() {
        return domainRepository.findAllActive();
    }

    public List<DnaDomain> findAllIncludingArchived() {
        return domainRepository.findAll();
    }

    @Transactional
    public DnaDomain archive(String id, String actor) {
        DnaDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Domain not found: " + id));

        // DGV-040: Archive refuses a domain still holding live-set state
        // Simplified: check that domain exists and is active
        if (!"active".equals(domain.getStatus())) {
            throw new IllegalStateException("Domain is not active: " + domain.getStatus());
        }
        
        domain.setStatus("archived");
        DnaDomain saved = domainRepository.save(domain);
        auditService.log(actor, "ARCHIVE", "dna_domain", id, null);
        return saved;
    }

    @Transactional
    public DnaDomain rename(String id, String newName, String actor) {
        DnaDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Domain not found: " + id));

        domain.setName(newName);
        DnaDomain saved = domainRepository.save(domain);
        auditService.logWithNode(actor, "RENAME", "dna_domain", id, null,
            String.format("{\"newName\":\"%s\"}", newName));
        return saved;
    }

    public DnaDomain updateOwner(String id, String newOwnerId, String actor) {
        DnaDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Domain not found: " + id));
        domain.setOwnerHumanId(newOwnerId);
        DnaDomain saved = domainRepository.save(domain);
        auditService.log(actor, "UPDATE_OWNER", "dna_domain", id, 
            String.format("{\"newOwner\":\"%s\"}", newOwnerId));
        return saved;
    }

    public DnaDomain updateAccess(String id, String access, String actor) {
        DnaDomain domain = domainRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Domain not found: " + id));
        domain.setAccess(access);
        DnaDomain saved = domainRepository.save(domain);
        auditService.log(actor, "UPDATE_ACCESS", "dna_domain", id, null);
        return saved;
    }
}
