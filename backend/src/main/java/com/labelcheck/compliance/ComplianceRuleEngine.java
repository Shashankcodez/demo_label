package com.labelcheck.compliance;

import com.labelcheck.dto.StructuredLabelData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic compliance rule engine that executes all registered packaging rules
 * against extracted label data and calculates an aggregate screening score.
 */
@Service
public class ComplianceRuleEngine {

    private static final Logger log = LoggerFactory.getLogger(ComplianceRuleEngine.class);

    private final List<ComplianceRule> rules;

    public ComplianceRuleEngine(List<ComplianceRule> rules) {
        this.rules = rules;
    }

    /**
     * Evaluates all statutory compliance rules against the extracted structured label data.
     *
     * @param labelData the structured label information extracted from OCR
     * @return aggregated ComplianceResult containing overallStatus, score, checks, and summary
     */
    public ComplianceResult evaluate(StructuredLabelData labelData) {
        List<ComplianceCheck> checks = new ArrayList<>();
        int score = 100;
        int violationCount = 0;
        int warningCount = 0;
        int passCount = 0;

        for (ComplianceRule rule : rules) {
            ComplianceCheck check = rule.evaluate(labelData);
            checks.add(check);

            switch (check.status()) {
                case VIOLATION -> {
                    violationCount++;
                    score -= 25;
                }
                case WARNING -> {
                    warningCount++;
                    score -= switch (check.severity()) {
                        case HIGH -> 15;
                        case MEDIUM -> 8;
                        case LOW -> 4;
                        case NONE -> 0;
                    };
                }
                case PASS -> passCount++;
            }
        }

        // Clamp score between 0 and 100
        score = Math.max(0, Math.min(100, score));

        RuleStatus overallStatus;
        if (violationCount > 0) {
            overallStatus = RuleStatus.VIOLATION;
        } else if (warningCount > 0) {
            overallStatus = RuleStatus.WARNING;
        } else {
            overallStatus = RuleStatus.PASS;
        }

        String summary = generateSummary(passCount, warningCount, violationCount, overallStatus);

        log.info("Compliance evaluation complete. Overall: [{}], Score: [{}/100], Pass: [{}], Warn: [{}], Viol: [{}]",
                overallStatus, score, passCount, warningCount, violationCount);

        return new ComplianceResult(overallStatus, score, checks, summary);
    }

    private String generateSummary(int passCount, int warningCount, int violationCount, RuleStatus overallStatus) {
        if (violationCount > 0) {
            return String.format(
                    "Identified %d confirmed statutory non-compliance issue%s. Review recommended corrective actions.",
                    violationCount, violationCount == 1 ? "" : "s"
            );
        } else if (warningCount > 0) {
            return String.format(
                    "%d declaration%s not conclusively identified in this scan. Automated screening suggests physical package verification.",
                    warningCount, warningCount == 1 ? " was" : "s were"
            );
        } else {
            return String.format(
                    "All %d statutory packaging declarations verified in accordance with Legal Metrology & FSSAI standards.",
                    passCount
            );
        }
    }
}
