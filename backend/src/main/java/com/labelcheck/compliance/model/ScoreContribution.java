package com.labelcheck.compliance.model;

import com.labelcheck.compliance.RuleSeverity;
import com.labelcheck.compliance.RuleStatus;

/**
 * Detailed breakdown of a single rule's contribution to the overall compliance screening score.
 */
public record ScoreContribution(
        String ruleId,
        int weight,
        RuleStatus status,
        int pointsDeducted,
        String rationale
) {
    public ScoreContribution(String ruleId, RuleStatus status, int pointsDeducted, RuleSeverity severity) {
        this(
                ruleId,
                25,
                status,
                pointsDeducted,
                pointsDeducted > 0 ? "Deducted " + pointsDeducted + " points (" + severity + " severity)" : "No deduction (" + status + ")"
        );
    }
}

