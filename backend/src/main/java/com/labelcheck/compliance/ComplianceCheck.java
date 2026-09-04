package com.labelcheck.compliance;

import com.labelcheck.compliance.model.RegulationFamily;

/**
 * Evaluated outcome of a single statutory packaging rule under Legal Metrology & allied laws.
 *
 * @param id                   unique identifier of the check (e.g. "RULE_MRP")
 * @param ruleReference        statutory act, regulation, and rule citation
 * @param title                concise human-readable title of the check
 * @param status               outcome status: PASS, WARNING, VIOLATION, FAIL, NOT_DETECTED, NOT_APPLICABLE, or REQUIRES_MANUAL_VERIFICATION
 * @param detected             value or state detected in the scan (e.g. "₹50.00" or "Not detected in image")
 * @param legalReason          explanation of the statutory requirement and compliance rationale
 * @param recommendation       actionable guidance for compliance or manual package verification
 * @param severity             severity level of any discrepancy
 * @param regulationFamily     statutory authority family (LEGAL_METROLOGY, FOOD_LABELING, OTHER_SECTORAL)
 * @param extractedValue       raw normalized value extracted from the package
 * @param evidenceText         snippet of text or visual token supporting the check
 * @param evidenceSource       origin of evidence (e.g. "OCR_TEXT", "VISION_PROMPT", "METADATA")
 * @param extractionConfidence model confidence in the extracted field (0.0 to 1.0)
 * @param validationConfidence engine confidence in the legal classification (0.0 to 1.0)
 * @param manualReviewReason   reasons why human inspection is required (if applicable)
 */
public record ComplianceCheck(
        String id,
        String ruleReference,
        String title,
        RuleStatus status,
        String detected,
        String legalReason,
        String recommendation,
        RuleSeverity severity,
        RegulationFamily regulationFamily,
        String extractedValue,
        String evidenceText,
        String evidenceSource,
        Double extractionConfidence,
        Double validationConfidence,
        String manualReviewReason
) {
    /**
     * Backward-compatible 8-parameter constructor for existing rules and test suites.
     */
    public ComplianceCheck(
            String id,
            String ruleReference,
            String title,
            RuleStatus status,
            String detected,
            String legalReason,
            String recommendation,
            RuleSeverity severity
    ) {
        this(
                id,
                ruleReference,
                title,
                status,
                detected,
                legalReason,
                recommendation,
                severity,
                id != null && id.contains("FSSAI") ? RegulationFamily.FOOD_LABELING : RegulationFamily.LEGAL_METROLOGY,
                detected,
                null,
                "SCAN_DATA",
                1.0,
                1.0,
                status == RuleStatus.REQUIRES_MANUAL_VERIFICATION || status == RuleStatus.WARNING ? recommendation : null
        );
    }
}

