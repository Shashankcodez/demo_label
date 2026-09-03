package com.labelcheck.compliance;

/**
 * Evaluated outcome of a single statutory packaging rule.
 *
 * @param id             unique identifier of the check (e.g. "RULE_MRP")
 * @param ruleReference  statutory act, regulation, and rule citation
 * @param title          concise human-readable title of the check
 * @param status         outcome status: PASS, WARNING, or VIOLATION
 * @param detected       value or state detected in the scan (e.g. "₹50.00" or "Not detected in image")
 * @param legalReason    explanation of the statutory requirement and compliance rationale
 * @param recommendation actionable guidance for compliance or manual package verification
 * @param severity       severity level of any discrepancy
 */
public record ComplianceCheck(
        String id,
        String ruleReference,
        String title,
        RuleStatus status,
        String detected,
        String legalReason,
        String recommendation,
        RuleSeverity severity
) {
}
