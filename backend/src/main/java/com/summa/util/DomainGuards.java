package com.summa.util;

import com.summa.exception.EntityNotFoundException;
import com.summa.model.DnaDomain;

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
}
