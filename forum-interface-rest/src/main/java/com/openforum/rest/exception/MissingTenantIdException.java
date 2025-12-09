package com.openforum.rest.exception;

public class MissingTenantIdException extends RuntimeException {
    public MissingTenantIdException(String message) {
        super(message);
    }
}
