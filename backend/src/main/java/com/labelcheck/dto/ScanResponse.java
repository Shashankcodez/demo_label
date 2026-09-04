package com.labelcheck.dto;

import com.labelcheck.compliance.ComplianceResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Unified Scan Response DTO representing the end-to-end outcome of image upload,
 * validation, storage, Vision AI extraction with deterministic OCR fallback,
 * entity extraction, statutory compliance evaluation, and persistent storage.
 *
 * @param scanId                      unique identifier generated for this scan session
 * @param filename                    sanitized server-side filename
 * @param contentType                 validated MIME type of the uploaded image
 * @param sizeBytes                   file size in bytes
 * @param status                      overall pipeline status
 * @param text                        raw text extracted (backward-compatible alias)
 * @param ocrText                     raw text extracted by local OCR
 * @param language                    the language code used for OCR (e.g. "eng")
 * @param message                     human-readable informational message
 * @param extractedLabel              structured statutory declarations
 * @param compliance                  statutory compliance evaluation result with individual rule checks
 * @param createdAt                   timestamp when the scan record was persisted
 * @param detectedFieldsCount         number of mandatory packaging fields detected (0 to 12)
 * @param labelQualityTier            quality tier: GOOD_LABEL, AVERAGE_LABEL, POOR_LABEL, VERY_POOR_IMAGE
 * @param complianceOutcome           workflow outcome
 * @param qualityMessage              human-readable quality summary message
 * @param extractionSource            primary source: "VISION_AI", "TESSERACT_FALLBACK", "HYBRID"
 * @param extractionStatus            pipeline status: "AI_SUCCESS", "AI_PARTIAL", "AI_FAILED_TESSERACT_FALLBACK", "OCR_AVAILABLE_EXTRACTION_LIMITED", "IMAGE_QUALITY_LOW", "TOTAL_EXTRACTION_FAILURE"
 * @param overallExtractionConfidence confidence score between 0.0 and 1.0
 * @param aiEnabled                   true if Vision AI is enabled on backend
 * @param aiModel                     model identifier used (e.g. "gpt-4o-mini")
 * @param fieldEvidence               map of field name to visual evidence snippet
 * @param fieldConfidence             map of field name to individual confidence score
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
        String qualityMessage,
        String extractionSource,
        String extractionStatus,
        Double overallExtractionConfidence,
        boolean aiEnabled,
        String aiModel,
        Map<String, String> fieldEvidence,
        Map<String, Double> fieldConfidence,
        Map<String, List<Integer>> fieldBoundingBoxes
) {
    public ScanResponse {
        if (fieldEvidence == null) fieldEvidence = Map.of();
        if (fieldConfidence == null) fieldConfidence = Map.of();
        if (fieldBoundingBoxes == null) fieldBoundingBoxes = Map.of();
    }

    /**
     * Backward-compatible 23-parameter constructor.
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
            Instant createdAt,
            int detectedFieldsCount,
            String labelQualityTier,
            String complianceOutcome,
            String qualityMessage,
            String extractionSource,
            String extractionStatus,
            Double overallExtractionConfidence,
            boolean aiEnabled,
            String aiModel,
            Map<String, String> fieldEvidence,
            Map<String, Double> fieldConfidence
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
                detectedFieldsCount,
                labelQualityTier,
                complianceOutcome,
                qualityMessage,
                extractionSource,
                extractionStatus,
                overallExtractionConfidence,
                aiEnabled,
                aiModel,
                fieldEvidence,
                fieldConfidence,
                Map.of()
        );
    }
    /**
     * Backward-compatible 16-parameter constructor.
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
            Instant createdAt,
            int detectedFieldsCount,
            String labelQualityTier,
            String complianceOutcome,
            String qualityMessage
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
                detectedFieldsCount,
                labelQualityTier,
                complianceOutcome,
                qualityMessage,
                "TESSERACT_FALLBACK",
                "OCR_AVAILABLE_EXTRACTION_LIMITED",
                0.70,
                false,
                null,
                Map.of(),
                Map.of()
        );
    }

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
