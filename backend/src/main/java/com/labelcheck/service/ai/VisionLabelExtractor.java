package com.labelcheck.service.ai;

import com.labelcheck.dto.ai.AiLabelExtractionResult;
import java.nio.file.Path;

/**
 * Provider-agnostic abstraction for extracting packaging declarations from product label images.
 * Implementations wrap vendor-specific HTTP vision endpoints (e.g. OpenAI, Azure, Anthropic, or local vLLM).
 */
public interface VisionLabelExtractor {

    /**
     * Checks if this extractor is enabled and has valid configuration credentials.
     *
     * @return true if enabled and ready to process images
     */
    boolean isEnabled();

    /**
     * Extracts visibly supported packaging declarations from a local image file.
     *
     * @param imagePath   path to the original uploaded label image file
     * @param contentType MIME type of the image (e.g. "image/jpeg", "image/png", "image/webp")
     * @return AiLabelExtractionResult containing extracted structured data or safe fallback status
     */
    AiLabelExtractionResult extract(Path imagePath, String contentType);
}
