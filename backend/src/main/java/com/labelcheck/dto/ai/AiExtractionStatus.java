package com.labelcheck.dto.ai;

/**
 * Explicit operational state of the packaging declaration extraction pipeline.
 */
public enum AiExtractionStatus {
    /**
     * Vision AI successfully extracted comprehensive statutory declarations.
     */
    AI_SUCCESS,

    /**
     * Vision AI executed successfully, but only a subset of packaging declarations were visibly present.
     */
    AI_PARTIAL,

    /**
     * Vision AI was unavailable (network error, rate limit, timeout, or invalid key);
     * local Tesseract OCR fallback was utilized to extract evidence.
     */
    AI_FAILED_TESSERACT_FALLBACK,

    /**
     * Local Tesseract OCR was executed (AI disabled by config) and provided partial extraction.
     */
    OCR_AVAILABLE_EXTRACTION_LIMITED,

    /**
     * Image legibility is too low (motion blur, specular glare, low contrast) to extract readable text.
     */
    IMAGE_QUALITY_LOW,

    /**
     * Neither Vision AI nor local OCR could decode readable declarations from the image.
     */
    TOTAL_EXTRACTION_FAILURE
}
