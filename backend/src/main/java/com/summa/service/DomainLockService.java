package com.summa.service;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * DGV-050 / DGV-051 / DGV-052 / DWP-031 / DWP-040 / DWP-060:
 * Domain-level write locks. Every domain has one writer door serialized behind
 * its write lock. Multi-domain writers acquire all affected locks up front in
 * domain-id order to guarantee deadlock-free execution.
 */
@Service
public class DomainLockService {

    public static final String ORG_WIDE_LOCK_KEY = "__ORG_WIDE__";

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    private String canonicalKey(String domainId) {
        if (domainId == null || domainId.isBlank()) {
            return ORG_WIDE_LOCK_KEY;
        }
        return domainId.trim();
    }

    private ReentrantLock getLock(String key) {
        return locks.computeIfAbsent(key, k -> new ReentrantLock());
    }

    /**
     * Run an action inside the write lock for a single domain (DGV-050).
     */
    public <T> T withLock(String domainId, Supplier<T> action) {
        String key = canonicalKey(domainId);
        ReentrantLock lock = getLock(key);
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Run a void action inside the write lock for a single domain (DGV-050).
     */
    public void withLock(String domainId, Runnable action) {
        withLock(domainId, () -> {
            action.run();
            return null;
        });
    }

    /**
     * Run an action inside the write locks for multiple domains, acquiring them
     * in deterministic lexicographic domain-id order (DGV-051 / DGV-052).
     */
    public <T> T withLocks(Collection<String> domainIds, Supplier<T> action) {
        if (domainIds == null || domainIds.isEmpty()) {
            return withLock(null, action);
        }
        List<String> sortedKeys = domainIds.stream()
                .map(this::canonicalKey)
                .distinct()
                .sorted()
                .toList();

        List<ReentrantLock> acquired = new ArrayList<>(sortedKeys.size());
        try {
            for (String key : sortedKeys) {
                ReentrantLock lock = getLock(key);
                lock.lock();
                acquired.add(lock);
            }
            return action.get();
        } finally {
            for (int i = acquired.size() - 1; i >= 0; i--) {
                acquired.get(i).unlock();
            }
        }
    }

    /**
     * Run a void action inside multiple domain write locks (DGV-051 / DGV-052).
     */
    public void withLocks(Collection<String> domainIds, Runnable action) {
        withLocks(domainIds, () -> {
            action.run();
            return null;
        });
    }
}
