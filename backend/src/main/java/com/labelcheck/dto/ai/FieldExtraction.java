package com.labelcheck.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Collections;
import java.util.List;

/**
 * Represents a single extracted packaging declaration with its value,
 * extraction confidence, visual evidence, and spatial bounding box coordinates.
 *
 * @param value       the exact visible declaration text or normalized statutory value
 * @param confidence  confidence level between 0.0 (uncertain) and 1.0 (certain)
 * @param evidence    brief description or verbatim quote of where it appears on the label
 * @param boundingBox normalized [ymin, xmin, ymax, xmax] coordinates (0-1000 scale)
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FieldExtraction(
        String value,
        Double confidence,
        String evidence,
        List<Integer> boundingBox
) {
    /**
     * Backward-compatible 3-argument constructor.
     */
    public FieldExtraction(String value, Double confidence, String evidence) {
        this(value, confidence, evidence, null);
    }

    public static FieldExtraction of(String value, Double confidence, String evidence) {
        return of(value, confidence, evidence, null);
    }

    public static FieldExtraction of(String value, Double confidence, String evidence, List<Integer> boundingBox) {
        String cleanValue = (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim()) || "not detected".equalsIgnoreCase(value.trim()))
                ? null
                : value.trim();
        List<Integer> cleanBox = (boundingBox != null && boundingBox.size() == 4)
                ? Collections.unmodifiableList(boundingBox)
                : null;
        return new FieldExtraction(cleanValue, confidence, evidence, cleanBox);
    }

    public static FieldExtraction empty() {
        return new FieldExtraction(null, null, null, null);
    }

    public boolean isPresent() {
        return value != null && !value.trim().isEmpty()
                && !value.equalsIgnoreCase("null")
                && !value.equalsIgnoreCase("Not detected")
                && !value.equalsIgnoreCase("N/A");
    }

    public boolean hasBoundingBox() {
        return boundingBox != null && boundingBox.size() == 4;
    }

    public double safeConfidence() {
        return confidence != null ? Math.max(0.0, Math.min(1.0, confidence)) : 0.0;
    }
}
