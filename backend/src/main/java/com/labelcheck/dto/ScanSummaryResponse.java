package com.labelcheck.dto;

import com.labelcheck.compliance.RuleStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Lightweight scan history summary record for paginated history listing.
 * Excludes heavy OCR text and detailed rule check sets to optimize payload size.
 *
 * @param scanId           unique scan identifier
 * @param filename         sanitized server filename
 * @param productName      product name if detected (or "Unknown / Product not detected")
 * @param brand            brand name if detected
 * @param status           pipeline analysis status
 * @param overallStatus    overall statutory compliance status (PASS, WARNING, VIOLATION)
 * @param overallScore     deterministic compliance score (0-100)
 * @param summary          high-level statutory screening summary
 * @param createdAt        timestamp when scan analysis was completed
 * @param extractionStatus extraction state (e.g. AI_SUCCESS, AI_PARTIAL, OCR_AVAILABLE_EXTRACTION_LIMITED)
 * @param extractionSource source (VISION_AI or TESSERACT_FALLBACK)
 */
public record ScanSummaryResponse(
        UUID scanId,
        String filename,
        String productName,
        String brand,
        String status,
        RuleStatus overallStatus,
        int overallScore,
        String summary,
        Instant createdAt,
        String extractionStatus,
        String extractionSource
) {
    /**
     * Backward-compatible 9-parameter constructor.
     */
    public ScanSummaryResponse(
            UUID scanId,
            String filename,
            String productName,
            String brand,
            String status,
            RuleStatus overallStatus,
            int overallScore,
            String summary,
            Instant createdAt
    ) {
        this(scanId, filename, productName, brand, status, overallStatus, overallScore, summary, createdAt, "AI_SUCCESS", "VISION_AI");
    }
}
