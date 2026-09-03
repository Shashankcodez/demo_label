package com.labelcheck.compliance;

import java.util.List;

/**
 * Aggregated compliance evaluation outcome produced by the Compliance Rule Engine.
 *
 * @param overallStatus overall compliance outcome (PASS, WARNING, or VIOLATION)
 * @param overallScore  deterministic compliance score (0 to 100)
 * @param checks        list of individual statutory rule evaluations
 * @param summary       concise summary of findings
 */
public record ComplianceResult(
        RuleStatus overallStatus,
        int overallScore,
        List<ComplianceCheck> checks,
        String summary
) {
}
