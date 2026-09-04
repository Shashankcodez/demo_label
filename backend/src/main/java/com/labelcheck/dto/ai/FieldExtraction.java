package com.labelcheck.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a single extracted packaging declaration with its value,
 * extraction confidence, and visual evidence explaining where/how it appears on the label.
 *
 * @param value      the exact visible declaration text or normalized statutory value
 * @param confidence confidence level between 0.0 (uncertain) and 1.0 (certain)
 * @param evidence   brief description of what visual part of the label supports this extraction
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FieldExtraction(
        String value,
        Double confidence,
        String evidence
) {
    public static FieldExtraction of(String value, Double confidence, String evidence) {
        String cleanValue = (value == null || value.trim().isEmpty() || "null".equalsIgnoreCase(value.trim()) || "not detected".equalsIgnoreCase(value.trim()))
                ? null
                : value.trim();
        return new FieldExtraction(cleanValue, confidence, evidence);
    }

    public static FieldExtraction empty() {
        return new FieldExtraction(null, null, null);
    }

    public boolean isPresent() {
        return value != null && !value.trim().isEmpty()
                && !value.equalsIgnoreCase("null")
                && !value.equalsIgnoreCase("Not detected")
                && !value.equalsIgnoreCase("N/A");
    }

    public double safeConfidence() {
        return confidence != null ? Math.max(0.0, Math.min(1.0, confidence)) : 0.0;
    }
}
