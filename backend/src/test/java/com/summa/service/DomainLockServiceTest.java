package com.summa.service;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class DomainLockServiceTest {

    private final DomainLockService lockService = new DomainLockService();

    @Test
    void withLock_executesSupplierUnderLock() {
        String result = lockService.withLock("dom-1", () -> "success");
        assertEquals("success", result);
    }

    @Test
    void withLock_nullOrBlankDomain_executesWithoutLock() {
        assertEquals("null-ok", lockService.withLock(null, () -> "null-ok"));
        assertEquals("blank-ok", lockService.withLock("   ", () -> "blank-ok"));
    }

    @Test
    void withLocks_executesUnderLocksInDeterministicOrder() {
        List<String> domains = List.of("dom-c", "dom-a", "dom-b", "dom-a");
        List<String> executed = lockService.withLocks(domains, () -> {
            return List.of("locked");
        });
        assertEquals(List.of("locked"), executed);
    }

    @Test
    void withLocks_emptyOrNullList_executesDirectly() {
        assertEquals("empty-ok", lockService.withLocks(List.of(), () -> "empty-ok"));
        assertEquals("null-ok", lockService.withLocks(null, () -> "null-ok"));
    }

    @Test
    void withLocks_deadlockFreeOrderingUnderConcurrency() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startSignal = new CountDownLatch(1);
        AtomicBoolean success1 = new AtomicBoolean(false);
        AtomicBoolean success2 = new AtomicBoolean(false);

        Future<?> f1 = executor.submit(() -> {
            try {
                startSignal.await();
                for (int i = 0; i < 50; i++) {
                    lockService.withLocks(List.of("dom-a", "dom-b"), () -> {
                        Thread.yield();
                        return null;
                    });
                }
                success1.set(true);
            } catch (Exception e) {
                fail(e);
            }
        });

        Future<?> f2 = executor.submit(() -> {
            try {
                startSignal.await();
                for (int i = 0; i < 50; i++) {
                    lockService.withLocks(List.of("dom-b", "dom-a"), () -> {
                        Thread.yield();
                        return null;
                    });
                }
                success2.set(true);
            } catch (Exception e) {
                fail(e);
            }
        });

        startSignal.countDown();
        f1.get(5, TimeUnit.SECONDS);
        f2.get(5, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(success1.get());
        assertTrue(success2.get());
    }
}
