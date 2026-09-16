package com.summa.exception;

/**
 * Explicit exception type for resource-level conflicts (409).
 * Using this instead of substring heuristics on IllegalStateException
 * ensures correct status code classification regardless of message wording.
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
