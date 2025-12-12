package com.openforum.application.exception;

/**
 * Exception thrown when a user attempts an action they are not authorized to
 * perform.
 * Results in HTTP 403 Forbidden response.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
