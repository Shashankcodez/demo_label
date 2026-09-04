package com.labelcheck.compliance.rules;

import com.labelcheck.compliance.ComplianceCheck;
import com.labelcheck.compliance.ComplianceRule;
import com.labelcheck.compliance.RuleSeverity;
import com.labelcheck.compliance.RuleStatus;
import com.labelcheck.compliance.model.ApplicabilityProfile;
import com.labelcheck.compliance.model.NormalizedLabel;
import com.labelcheck.dto.StructuredLabelData;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Validates prohibition of misleading qualifiers on net quantity declarations under
 * Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 13(5).
 * Strictly prohibits expressions such as "when packed", "approximate", "approx.",
 * "minimum", "not less than" which tend to create a misleading impression.
 */
@Component
public class QuantityQualifierRule implements ComplianceRule {

    private static final String RULE_ID = "RULE_QUANTITY_QUALIFIER";
    private static final String RULE_REF = "Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 13(5)";
    private static final String TITLE = "Prohibition of Misleading Quantity Qualifiers";

    private static final Pattern PROHIBITED_QUALIFIERS = Pattern.compile(
            "(?i)\\b(when\\s+packed|approx(?:imate|imately)?|approx\\.?|min(?:imum)?\\.?|not\\s+less\\s+than|at\\s+least)\\b"
    );

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public ComplianceCheck evaluate(StructuredLabelData labelData) {
        if (labelData == null) {
            return notDetectedCheck();
        }
        String netQty = labelData.netQuantity();
        String rawOcr = labelData.rawOcrText();
        return checkQualifiers(netQty, rawOcr);
    }

    @Override
    public ComplianceCheck evaluateNormalized(NormalizedLabel label, ApplicabilityProfile profile) {
        if (label == null) {
            return notDetectedCheck();
        }
        String netQty = label.netQuantity();
        String rawOcr = label.rawOcrText();
        return checkQualifiers(netQty, rawOcr);
    }

    private ComplianceCheck checkQualifiers(String netQty, String rawOcr) {
        if (netQty == null || netQty.isBlank()) {
            return notDetectedCheck();
        }

        // Check if the net quantity string itself contains a prohibited qualifier
        if (PROHIBITED_QUALIFIERS.matcher(netQty).find()) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.FAIL,
                    netQty,
                    "Net quantity is qualified by a prohibited expression under Rule 13(5). Words such as 'when packed', 'approximate', 'minimum', or 'not less than' are strictly prohibited because they mislead consumers regarding actual commodity quantity.",
                    "Remove all qualifying words from the net quantity declaration and state the definite standard quantity.",
                    RuleSeverity.HIGH
            );
        }

        // Check surrounding OCR context specifically adjacent to net quantity
        if (rawOcr != null && !rawOcr.isBlank()) {
            Pattern adjacentPattern = Pattern.compile(
                    "(?i)(?:net\\s+wt\\.?|net\\s+qty\\.?|net\\s+weight|net\\s+volume|weight|quantity)\\s*[:=-]?\\s*(?:when\\s+packed|approx(?:imate)?|min(?:imum)?|not\\s+less\\s+than)"
            );
            if (adjacentPattern.matcher(rawOcr).find()) {
                return new ComplianceCheck(
                        RULE_ID,
                        RULE_REF,
                        TITLE,
                        RuleStatus.FAIL,
                        "Prohibited qualifier detected near net quantity in label text",
                        "Rule 13(5) prohibits qualifiers like 'when packed', 'approximate', or 'minimum' adjacent to net quantity statements.",
                        "Eliminate qualifying prefixes or suffixes from packaging artwork.",
                        RuleSeverity.HIGH
                );
            }
        }

        return new ComplianceCheck(
                RULE_ID,
                RULE_REF,
                TITLE,
                RuleStatus.PASS,
                "Definite quantity declared (" + netQty + ") without prohibited qualifiers",
                "Net quantity is stated definitely without misleading qualifying words ('approximate', 'when packed', 'minimum') in accordance with Rule 13(5).",
                "Ensure future packaging revisions maintain unqualified net quantity declarations.",
                RuleSeverity.NONE
        );
    }

    private ComplianceCheck notDetectedCheck() {
        return new ComplianceCheck(
                RULE_ID,
                RULE_REF,
                TITLE,
                RuleStatus.NOT_DETECTED,
                "Net quantity not detected",
                "Net quantity was not identified in the image; qualifier compliance cannot be evaluated.",
                "Verify that net quantity is declared without prohibited words on the physical package.",
                RuleSeverity.NONE
        );
    }
}
