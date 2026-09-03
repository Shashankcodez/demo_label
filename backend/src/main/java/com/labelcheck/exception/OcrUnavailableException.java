package com.labelcheck.exception;

/**
 * Exception thrown when OCR processing is disabled or the native engine / language training data
 * is unavailable on the host system.
 */
public class OcrUnavailableException extends OcrException {

    public OcrUnavailableException(String message) {
        super(message);
    }

    public OcrUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
