package com.labelcheck.dto;

import com.labelcheck.compliance.ComplianceResult;

import java.time.Instant;
import java.util.UUID;

/**
 * Unified Scan Response DTO representing the end-to-end outcome of image upload,
 * validation, storage, OCR text extraction, entity extraction, statutory compliance evaluation,
 * and persistent storage.
 *
 * @param scanId         unique identifier generated for this scan session
 * @param filename       sanitized server-side filename (does not expose physical server directory)
 * @param contentType    validated MIME type of the uploaded image
 * @param sizeBytes      file size in bytes
 * @param status         overall pipeline status: "ANALYSIS_COMPLETE" or "OCR_NO_TEXT"
 * @param text           raw text extracted by the local OCR engine (backward-compatible alias)
 * @param ocrText        raw text extracted by the local OCR engine
 * @param language       the language code used for OCR (e.g. "eng")
 * @param message        human-readable informational message
 * @param extractedLabel structured statutory declarations parsed from OCR text
 * @param compliance     statutory compliance evaluation result with individual rule checks
 * @param createdAt      timestamp when the scan record was persisted
 * @param detectedFieldsCount number of mandatory packaging fields detected (0 to 12)
 * @param labelQualityTier quality tier: GOOD_LABEL, AVERAGE_LABEL, POOR_LABEL, VERY_POOR_IMAGE
 * @param complianceOutcome workflow outcome: Compliance, Compliance + Needs Review, Partial extraction + Needs Review, Retake image
 * @param qualityMessage human-readable quality summary message
 */
public record ScanResponse(
        UUID scanId,
        String filename,
        String contentType,
        long sizeBytes,
        String status,
        String text,
        String ocrText,
        String language,
        String message,
        StructuredLabelData extractedLabel,
        ComplianceResult compliance,
        Instant createdAt,
        int detectedFieldsCount,
        String labelQualityTier,
        String complianceOutcome,
        String qualityMessage
) {
    /**
     * Backward-compatible 12-parameter constructor calculating quality fields automatically.
     */
    public ScanResponse(
            UUID scanId,
            String filename,
            String contentType,
            long sizeBytes,
            String status,
            String text,
            String ocrText,
            String language,
            String message,
            StructuredLabelData extractedLabel,
            ComplianceResult compliance,
            Instant createdAt
    ) {
        this(
                scanId,
                filename,
                contentType,
                sizeBytes,
                status,
                text,
                ocrText,
                language,
                message,
                extractedLabel,
                compliance,
                createdAt,
                extractedLabel != null ? extractedLabel.countDetectedFields() : 0,
                extractedLabel != null ? extractedLabel.getQualityTier() : "VERY_POOR_IMAGE",
                extractedLabel != null ? extractedLabel.getComplianceOutcome() : "Retake image",
                message
        );
    }

    /**
     * Backward-compatible 11-parameter constructor defaulting createdAt to Instant.now().
     */
    public ScanResponse(
            UUID scanId,
            String filename,
            String contentType,
            long sizeBytes,
            String status,
            String text,
            String ocrText,
            String language,
            String message,
            StructuredLabelData extractedLabel,
            ComplianceResult compliance
    ) {
        this(scanId, filename, contentType, sizeBytes, status, text, ocrText, language, message, extractedLabel, compliance, Instant.now());
    }
}
