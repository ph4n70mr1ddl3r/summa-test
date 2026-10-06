package com.summa.exception;

/**
 * Explicit exception type for authorization refusals (403).
 * Using this instead of substring heuristics on IllegalStateException
 * ensures correct status code classification regardless of message wording.
 * Thrown by WriteGate, controllers, and services when an actor lacks
 * the required permissions for an operation.
 */
public class AuthorizationDeniedException extends IllegalStateException {
    public AuthorizationDeniedException(String message) {
        super(message);
    }
}
