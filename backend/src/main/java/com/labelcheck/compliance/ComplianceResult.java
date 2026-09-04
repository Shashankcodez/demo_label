package com.labelcheck.compliance;

import com.labelcheck.compliance.model.ApplicabilityProfile;
import com.labelcheck.compliance.model.ScoreContribution;

import java.util.List;

/**
 * Aggregated compliance evaluation outcome produced by the Legal Metrology Compliance Evaluation Engine.
 *
 * @param overallStatus        overall compliance outcome (PASS, WARNING, VIOLATION, FAIL, or REQUIRES_MANUAL_VERIFICATION)
 * @param overallScore         deterministic compliance score (0 to 100)
 * @param checks               master list of all individual statutory rule evaluations
 * @param summary              concise summary of findings
 * @param ruleEngineVersion    engine version tag (e.g. "LM-PCR-2026.01")
 * @param violations           list of confirmed statutory non-compliance items (FAIL or VIOLATION)
 * @param warnings             list of non-critical discrepancy items
 * @param manualReviewItems    list of items requiring physical package or documentary verification
 * @param passedChecks         list of fully compliant statutory declarations
 * @param notDetectedItems     list of declarations not identified in this visual scan
 * @param notApplicableItems   list of statutory rules excluded by package applicability profile
 * @param scoreBreakdown       transparent score deductions and weights per rule
 * @param applicabilityProfile profile of package attributes used to select active rules
 */
public record ComplianceResult(
        RuleStatus overallStatus,
        int overallScore,
        List<ComplianceCheck> checks,
        String summary,
        String ruleEngineVersion,
        List<ComplianceCheck> violations,
        List<ComplianceCheck> warnings,
        List<ComplianceCheck> manualReviewItems,
        List<ComplianceCheck> passedChecks,
        List<ComplianceCheck> notDetectedItems,
        List<ComplianceCheck> notApplicableItems,
        List<ScoreContribution> scoreBreakdown,
        ApplicabilityProfile applicabilityProfile
) {
    /**
     * Backward-compatible 4-parameter constructor for existing callers and test suites.
     */
    public ComplianceResult(
            RuleStatus overallStatus,
            int overallScore,
            List<ComplianceCheck> checks,
            String summary
    ) {
        this(
                overallStatus,
                overallScore,
                checks != null ? checks : List.of(),
                summary,
                "LM-PCR-2026.01",
                filterByStatus(checks, RuleStatus.VIOLATION, RuleStatus.FAIL),
                filterByStatus(checks, RuleStatus.WARNING),
                filterByStatus(checks, RuleStatus.REQUIRES_MANUAL_VERIFICATION),
                filterByStatus(checks, RuleStatus.PASS),
                filterByStatus(checks, RuleStatus.NOT_DETECTED),
                filterByStatus(checks, RuleStatus.NOT_APPLICABLE),
                List.of(),
                ApplicabilityProfile.defaultRetailFood()
        );
    }

    private static List<ComplianceCheck> filterByStatus(List<ComplianceCheck> checks, RuleStatus... statuses) {
        if (checks == null || checks.isEmpty()) {
            return List.of();
        }
        List<RuleStatus> match = List.of(statuses);
        return checks.stream()
                .filter(c -> match.contains(c.status()))
                .toList();
    }
}

