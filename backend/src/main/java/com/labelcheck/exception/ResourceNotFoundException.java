package com.labelcheck.exception;

/**
 * Thrown when a requested resource (e.g. scan analysis record) does not exist in persistence.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
