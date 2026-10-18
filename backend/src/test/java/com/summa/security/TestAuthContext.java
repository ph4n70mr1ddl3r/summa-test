package com.summa.security;

/**
 * Test-only bridge: lets controller tests in other packages drive the
 * RbacAuthorizationFilter thread-locals (actor identity + write surface),
 * which the package-private test setters otherwise restrict to this package.
 */
public final class TestAuthContext {

    private TestAuthContext() {
    }

    /** Install an authenticated, write-capable actor for the current thread. */
    public static void allowWrites(String actor) {
        RbacAuthorizationFilter.setActorForTest(actor);
        RbacAuthorizationFilter.setWriteAllowedForTest(true);
    }

    /** Clear all auth thread-locals for the current thread. */
    public static void clear() {
        RbacAuthorizationFilter.clearThreadLocals();
    }
}
