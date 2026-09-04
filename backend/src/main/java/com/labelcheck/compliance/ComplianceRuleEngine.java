package com.labelcheck.compliance;

import com.labelcheck.compliance.model.ApplicabilityProfile;
import com.labelcheck.compliance.model.NormalizedLabel;
import com.labelcheck.compliance.model.ScoreContribution;
import com.labelcheck.compliance.rules.LegalMetrologyRuleCatalog;
import com.labelcheck.dto.StructuredLabelData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deterministic Legal Metrology Compliance Evaluation Engine (LM-PCR-2026.01).
 * Executes statutory packaging rules against extracted label data, computes
 * an applicability profile, performs safe isolated evaluations, and provides
 * a fully auditable compliance outcome with transparent score contributions.
 */
@Service
public class ComplianceRuleEngine {

    public static final String ENGINE_VERSION = LegalMetrologyRuleCatalog.ENGINE_VERSION;

    private static final Logger log = LoggerFactory.getLogger(ComplianceRuleEngine.class);

    private final List<ComplianceRule> rules;

    public ComplianceRuleEngine(List<ComplianceRule> rules) {
        this.rules = rules;
    }

    /**
     * Evaluates all registered statutory compliance rules against the extracted structured label data.
     *
     * @param labelData the structured label information extracted from OCR/Vision AI
     * @return aggregated ComplianceResult containing version, partitioned lists, score, and summary
     */
    public ComplianceResult evaluate(StructuredLabelData labelData) {
        return evaluate(labelData, Map.of(), Map.of());
    }

    /**
     * Evaluates all registered statutory compliance rules with evidence and confidence maps.
     */
    public ComplianceResult evaluate(StructuredLabelData labelData, Map<String, String> evidence, Map<String, Double> confidence) {
        NormalizedLabel normalized = NormalizedLabel.fromStructuredData(labelData, evidence, confidence);
        ApplicabilityProfile profile = buildApplicabilityProfile(normalized);

        List<ComplianceCheck> checks = new ArrayList<>();
        List<ScoreContribution> scoreBreakdown = new ArrayList<>();

        int score = 100;
        int violationCount = 0;
        int warningCount = 0;
        int passCount = 0;
        int actionableReviewCount = 0;

        for (ComplianceRule rule : rules) {
            ComplianceCheck check = executeRuleSafely(rule, normalized, profile, labelData);
            checks.add(check);

            int deduction = 0;
            switch (check.status()) {
                case VIOLATION, FAIL -> {
                    violationCount++;
                    deduction = 25;
                    score -= deduction;
                }
                case WARNING -> {
                    warningCount++;
                    deduction = switch (check.severity()) {
                        case CRITICAL -> 20;
                        case HIGH -> 15;
                        case MEDIUM -> 8;
                        case LOW -> 4;
                        case NONE, INFO, MANUAL_REVIEW -> 0;
                    };
                    score -= deduction;
                }
                case REQUIRES_MANUAL_VERIFICATION -> {
                    if (check.severity() != RuleSeverity.INFO && check.severity() != RuleSeverity.NONE) {
                        actionableReviewCount++;
                    }
                }
                case PASS -> passCount++;
                case NOT_DETECTED, NOT_APPLICABLE -> {
                    // Informational outcomes do not deduct score
                }
            }

            scoreBreakdown.add(new ScoreContribution(check.id(), check.status(), deduction, check.severity()));
        }

        // Clamp score between 0 and 100
        score = Math.max(0, Math.min(100, score));

        RuleStatus overallStatus;
        if (violationCount > 0) {
            overallStatus = RuleStatus.VIOLATION;
        } else if (warningCount > 0) {
            overallStatus = RuleStatus.WARNING;
        } else if (actionableReviewCount > 0) {
            overallStatus = RuleStatus.REQUIRES_MANUAL_VERIFICATION;
        } else {
            overallStatus = RuleStatus.PASS;
        }

        String summary = generateSummary(passCount, warningCount, violationCount, actionableReviewCount, overallStatus);

        log.info("Legal Metrology evaluation ({}) complete. Overall: [{}], Score: [{}/100], Pass: [{}], Warn: [{}], Viol: [{}], Review: [{}]",
                ENGINE_VERSION, overallStatus, score, passCount, warningCount, violationCount, actionableReviewCount);

        List<ComplianceCheck> violations = checks.stream()
                .filter(c -> c.status() == RuleStatus.VIOLATION || c.status() == RuleStatus.FAIL)
                .toList();
        List<ComplianceCheck> warnings = checks.stream()
                .filter(c -> c.status() == RuleStatus.WARNING)
                .toList();
        List<ComplianceCheck> manualReviewItems = checks.stream()
                .filter(c -> c.status() == RuleStatus.REQUIRES_MANUAL_VERIFICATION)
                .toList();
        List<ComplianceCheck> passedChecks = checks.stream()
                .filter(c -> c.status() == RuleStatus.PASS)
                .toList();
        List<ComplianceCheck> notDetectedItems = checks.stream()
                .filter(c -> c.status() == RuleStatus.NOT_DETECTED)
                .toList();
        List<ComplianceCheck> notApplicableItems = checks.stream()
                .filter(c -> c.status() == RuleStatus.NOT_APPLICABLE)
                .toList();

        return new ComplianceResult(
                overallStatus,
                score,
                checks,
                summary,
                ENGINE_VERSION,
                violations,
                warnings,
                manualReviewItems,
                passedChecks,
                notDetectedItems,
                notApplicableItems,
                scoreBreakdown,
                profile
        );
    }

    private ComplianceCheck executeRuleSafely(ComplianceRule rule, NormalizedLabel normalized, ApplicabilityProfile profile, StructuredLabelData fallbackData) {
        try {
            ComplianceCheck check = rule.evaluateNormalized(normalized, profile);
            if (check != null) {
                return check;
            }
            return rule.evaluate(fallbackData);
        } catch (Exception ex) {
            log.error("Compliance rule [{}] encountered an error during evaluation: {}", rule.getRuleId(), ex.getMessage(), ex);
            return new ComplianceCheck(
                    rule.getRuleId(),
                    rule.getMetadata() != null ? rule.getMetadata().legalReference() : "Legal Metrology Act, 2009",
                    rule.getMetadata() != null ? rule.getMetadata().title() : rule.getRuleId(),
                    RuleStatus.REQUIRES_MANUAL_VERIFICATION,
                    "Rule evaluation error",
                    "Automated rule evaluation could not complete deterministically due to unexpected input structure. Per screening safety rules, no legal violation is declared.",
                    "Inspect the physical commodity to verify this statutory requirement manually.",
                    RuleSeverity.MANUAL_REVIEW
            );
        }
    }

    private ApplicabilityProfile buildApplicabilityProfile(NormalizedLabel label) {
        boolean imported = label.countryOfOrigin() != null && !label.countryOfOrigin().matches("(?i).*(india|bharat).*");
        if (label.importerName() != null && !label.importerName().isBlank() && label.countryOfOrigin() == null) {
            imported = true;
        }

        boolean food = label.fssaiLicenseNumber() != null
                || (label.productName() != null && label.productName().matches("(?i).*(oil|biscuit|cookie|wafer|juice|atta|flour|rice|tea|coffee|masala|spice|noodle|milk|butter|ghee|paneer|chips|snack).*"));

        ApplicabilityProfile.MeasurementType mType = ApplicabilityProfile.MeasurementType.UNKNOWN;
        if (label.netQuantityUnit() != null) {
            String u = label.netQuantityUnit().toLowerCase();
            if (u.contains("g") || u.contains("kg")) {
                mType = ApplicabilityProfile.MeasurementType.MASS;
            } else if (u.contains("l") || u.contains("ml")) {
                mType = ApplicabilityProfile.MeasurementType.VOLUME;
            } else if (u.contains("m") || u.contains("cm")) {
                mType = ApplicabilityProfile.MeasurementType.LENGTH;
            } else if (u.contains("n") || u.contains("u") || u.contains("pc")) {
                mType = ApplicabilityProfile.MeasurementType.NUMBER;
            }
        }

        return new ApplicabilityProfile(
                true,
                false,
                false,
                imported,
                food,
                false,
                false,
                mType,
                ApplicabilityProfile.PackageGeometry.RECTANGULAR,
                null,
                label.netQuantityNumeric(),
                label.netQuantityUnit(),
                0.90,
                "Standard retail pre-packaged commodity"
        );
    }

    private String generateSummary(int passCount, int warningCount, int violationCount, int actionableReviewCount, RuleStatus overallStatus) {
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
        } else if (actionableReviewCount > 0) {
            return String.format(
                    "Automated screening complete. %d declaration%s require physical verification on package.",
                    actionableReviewCount, actionableReviewCount == 1 ? "s" : "s"
            );
        } else {
            return String.format(
                    "All %d statutory packaging declarations verified in accordance with Legal Metrology & FSSAI standards.",
                    passCount
            );
        }
    }
}

