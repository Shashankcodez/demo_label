package com.labelcheck.compliance;

/**
 * Status outcomes for individual statutory compliance checks and overall evaluation
 * in the Legal Metrology Compliance Evaluation Engine (LM-PCR-2026.01).
 */
public enum RuleStatus {
    PASS,
    WARNING,
    VIOLATION,
    FAIL,
    NOT_DETECTED,
    NOT_APPLICABLE,
    REQUIRES_MANUAL_VERIFICATION;

    /**
     * True if the status represents a confirmed statutory non-compliance (FAIL or VIOLATION).
     */
    public boolean isNonCompliant() {
        return this == VIOLATION || this == FAIL;
    }

    /**
     * True if the status indicates an ambiguous, low-confidence, or physically unverified check.
     */
    public boolean requiresHumanReview() {
        return this == REQUIRES_MANUAL_VERIFICATION || this == WARNING;
    }
}

