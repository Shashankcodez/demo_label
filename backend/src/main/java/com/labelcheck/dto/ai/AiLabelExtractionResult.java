package com.labelcheck.dto.ai;

/**
 * Encapsulates the complete result of an attempt to extract packaging information via Vision AI.
 *
 * @param status             the extraction status category
 * @param overallConfidence  confidence score (0.0 to 1.0)
 * @param extractionSource   source label (e.g. "VISION_AI", "TESSERACT_FALLBACK")
 * @param modelName          the AI model identifier used
 * @param label              strongly-typed extracted declarations
 * @param rawResponse        raw text or JSON received from the provider (for inspection)
 * @param errorMessage       sanitized error message if failed
 */
public record AiLabelExtractionResult(
        AiExtractionStatus status,
        Double overallConfidence,
        String extractionSource,
        String modelName,
        StructuredAiLabel label,
        String rawResponse,
        String errorMessage
) {
    public static AiLabelExtractionResult success(Double confidence, String modelName, StructuredAiLabel label) {
        return success(confidence, "VISION_AI", modelName, label);
    }

    public static AiLabelExtractionResult success(Double confidence, String extractionSource, String modelName, StructuredAiLabel label) {
        return new AiLabelExtractionResult(
                AiExtractionStatus.AI_SUCCESS,
                confidence != null ? confidence : 0.9,
                extractionSource != null ? extractionSource : "VISION_AI",
                modelName,
                label,
                null,
                null
        );
    }

    public static AiLabelExtractionResult partial(Double confidence, String modelName, StructuredAiLabel label) {
        return partial(confidence, "VISION_AI", modelName, label);
    }

    public static AiLabelExtractionResult partial(Double confidence, String extractionSource, String modelName, StructuredAiLabel label) {
        return new AiLabelExtractionResult(
                AiExtractionStatus.AI_PARTIAL,
                confidence != null ? confidence : 0.6,
                extractionSource != null ? extractionSource : "VISION_AI",
                modelName,
                label,
                null,
                null
        );
    }

    public static AiLabelExtractionResult failed(AiExtractionStatus status, String modelName, String errorMessage) {
        return new AiLabelExtractionResult(
                status,
                0.0,
                "FALLBACK",
                modelName,
                null,
                null,
                errorMessage
        );
    }
}
