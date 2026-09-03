package com.labelcheck.dto;

import java.util.UUID;

/**
 * Response DTO returned upon successfully validating and temporarily storing a product label image.
 *
 * @param scanId      unique identifier generated for this scan session
 * @param filename    sanitized server-side filename (does not expose physical server directory)
 * @param contentType validated MIME type of the uploaded image
 * @param sizeBytes   file size in bytes
 * @param message     informational message confirming image receipt
 */
public record ScanUploadResponse(
        UUID scanId,
        String filename,
        String contentType,
        long sizeBytes,
        String message
) {
}
