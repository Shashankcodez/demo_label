package com.labelcheck.exception;

/**
 * Base exception thrown when an error occurs during OCR image text extraction.
 */
public class OcrException extends RuntimeException {

    public OcrException(String message) {
        super(message);
    }

    public OcrException(String message, Throwable cause) {
        super(message, cause);
    }
}
