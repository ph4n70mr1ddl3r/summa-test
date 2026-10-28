package com.summa.util;

import com.summa.exception.EntityNotFoundException;
import com.summa.model.DnaDomain;
import com.summa.service.MemberService;

/**
 * DGV-041: the archived domain row is read-only history — no injection, routing, or
 * new bindings. A write that lands inside an archived compartment is refused at the
 * door rather than written into frozen history.
 */
public final class DomainGuards {

    private DomainGuards() {
    }

    /**
     * Require that the named domain exists and is writable.
     *
     * @param domain  the resolved domain row (null when the domain does not exist)
     * @param domainId the id the caller asked for, for the refusal message
     */
    public static void requireWritable(DnaDomain domain, String domainId) {
        if (domain == null) {
            throw new EntityNotFoundException("Domain not found: " + domainId);
        }
        // The domain's status vocabulary is active | archived; a not-yet-persisted
        // row carries no status and defaults to active on write.
        if ("archived".equals(domain.getStatus())) {
            throw new IllegalStateException(
                    "Domain is archived — read-only history (DGV-041): " + domainId);
        }
    }

    /**
     * DWP-060 / SEC-020: Item-level direct writes require that the actor is the domain
     * owner or an admin. For org-scoped items (domain is null/blank), only an admin may write.
     * Also checks separation of duties (DWP-050): if domain has sod == 'reviewer-distinct',
     * direct write by the owner is refused (must go through proposal & separate reviewer).
     */
    public static void requireOwnerOrAdmin(com.summa.service.MemberService memberService, DnaDomain domain, String domainId, String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalStateException("Authentication required for DNA write");
        }
        String actorClean = JsonHelpers.stripIdPrefix(actor);
        boolean isAdmin = memberService != null && memberService.isAdmin(actorClean);
        if (isAdmin) {
            return;
        }
        if (domainId == null || domainId.isBlank() || domain == null) {
            throw new IllegalStateException("Admin access required for org-wide DNA write");
        }
        boolean isOwner = domain.getOwnerHumanId() != null
                && domain.getOwnerHumanId().equals(actorClean);
        if (!isOwner) {
            throw new IllegalStateException("Only the domain owner or an admin may write to this domain: " + domainId);
        }
        if ("reviewer-distinct".equals(domain.getSod())) {
            throw new IllegalStateException(
                "Separation of duties: direct write in a domain with reviewer-distinct SoD requires proposal review (DWP-060)");
        }
    }
}
