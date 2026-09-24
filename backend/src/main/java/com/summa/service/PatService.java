package com.summa.service;

import com.summa.repository.PatRepository;
import com.summa.model.Pat;
import com.summa.exception.EntityNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PatService {
    private final PatRepository patRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public PatService(PatRepository patRepository, AuditService auditService,
                      PasswordEncoder passwordEncoder) {
        this.patRepository = patRepository;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PatWithToken create(String memberId, String name, List<String> scopes, int expiryDays) {
        if (expiryDays <= 0) {
            throw new IllegalArgumentException("expiryDays must be positive");
        }
        String rawToken = generateToken();
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

        return new PatWithToken(saved, rawToken);
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

    @Transactional
    public Pat revoke(String id, String actor) {
        Pat pat = patRepository.findByIdAndRevokedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("PAT not found: " + id));

        pat.setRevokedAt(Instant.now());
        Pat saved = patRepository.save(pat);
        auditService.log(actor, "REVOKE_PAT", "pat", id, null);
        return saved;
    }

    @Transactional
    public void touchLastUsed(String id) {
        patRepository.findById(id).ifPresent(pat -> {
            pat.setLastUsedAt(Instant.now());
            patRepository.save(pat);
        });
    }

    private String generateToken() {
        return "summa_pat_" + UUID.randomUUID().toString().replace("-", "") +
               "_" + UUID.randomUUID().toString().replace("-", "");
    }

    private String hashToken(String token) {
        return passwordEncoder.encode(token);
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

    public record PatWithToken(Pat pat, String token) {}
}
