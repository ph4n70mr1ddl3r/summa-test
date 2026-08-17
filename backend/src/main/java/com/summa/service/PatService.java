package com.summa.service;

import com.summa.repository.PatRepository;
import com.summa.model.Pat;
import org.springframework.stereotype.Service;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PatService {
    private final PatRepository patRepository;
    private final AuditService auditService;

    public PatService(PatRepository patRepository, AuditService auditService) {
        this.patRepository = patRepository;
        this.auditService = auditService;
    }

    public Pat create(String memberId, String name, List<String> scopes, int expiryDays) {
        String rawToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        String tokenHash = hashToken(rawToken);

        Pat pat = new Pat();
        pat.setId(UUID.randomUUID().toString());
        pat.setMemberId(memberId);
        pat.setName(name);
        pat.setTokenHash(tokenHash);
        pat.setScopes(scopes != null ? serializeScopes(scopes) : "[]");
        pat.setExpiresAt(Instant.now().plusSeconds(expiryDays * 86400L));

        Pat saved = patRepository.save(pat);
        auditService.log(memberId, "CREATE_PAT", "pat", saved.getId(),
            String.format("{\"name\":\"%s\",\"expiryDays\":%d}", name, expiryDays));

        // Return with plaintext token (shown once)
        saved = new Pat();
        saved.setId(saved.getId());
        saved.setMemberId(saved.getMemberId());
        saved.setName(saved.getName());
        saved.setScopes(saved.getScopes());
        saved.setExpiresAt(saved.getExpiresAt());
        return saved;
    }

    public Optional<Pat> findById(String id) {
        return patRepository.findById(id);
    }

    public Optional<Pat> findByHash(String tokenHash) {
        return patRepository.findByTokenHash(tokenHash);
    }

    public List<Pat> findByMember(String memberId) {
        return patRepository.findByMemberId(memberId);
    }

    public Pat revoke(String id, String actor) {
        Pat pat = patRepository.findByIdAndRevokedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("PAT not found: " + id));

        pat.setRevokedAt(Instant.now());
        Pat saved = patRepository.save(pat);
        auditService.log(actor, "REVOKE_PAT", "pat", id, null);
        return saved;
    }

    public void touchLastUsed(String id) {
        patRepository.findById(id).ifPresent(pat -> {
            pat.setLastUsedAt(Instant.now());
            patRepository.save(pat);
        });
    }

    private String hashToken(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes());
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    private String serializeScopes(List<String> scopes) {
        if (scopes == null || scopes.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < scopes.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(scopes.get(i)).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }
}
