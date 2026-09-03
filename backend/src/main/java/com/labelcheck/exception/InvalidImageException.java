package com.labelcheck.exception;

/**
 * Exception thrown when an uploaded image fails validation (empty file, unsupported MIME type,
 * or corrupted/invalid image binary data).
 */
public class InvalidImageException extends RuntimeException {

    public InvalidImageException(String message) {
        super(message);
    }
}
