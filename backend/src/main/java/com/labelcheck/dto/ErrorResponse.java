package com.labelcheck.dto;

import java.time.Instant;

/**
 * Standardized error response DTO for consistent API error reporting.
 *
 * @param timestamp the time at which the error occurred
 * @param status    the HTTP status code
 * @param error     short error reason / category
 * @param message   human-readable, safe error message
 * @param path      the request URI path that produced the error
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
