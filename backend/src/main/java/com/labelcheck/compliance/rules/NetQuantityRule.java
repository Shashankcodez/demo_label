package com.labelcheck.compliance.rules;

import com.labelcheck.compliance.ComplianceCheck;
import com.labelcheck.compliance.ComplianceRule;
import com.labelcheck.compliance.RuleSeverity;
import com.labelcheck.compliance.RuleStatus;
import com.labelcheck.dto.StructuredLabelData;
import org.springframework.stereotype.Component;

/**
 * Validates Net Quantity declaration under Legal Metrology (Packaged Commodities) Rules, 2011 -
 * Rule 6(1)(c) read with Rule 12 & Rule 13, and FSSAI (Labelling and Display) Regulations, 2020 - Regulation 5(2).
 */
@Component
public class NetQuantityRule implements ComplianceRule {

    private static final String RULE_ID = "RULE_NET_QTY";
    private static final String RULE_REF = "Legal Metrology Rules, 2011 - Rule 6(1)(c) & FSSAI Reg. 5(2)";
    private static final String TITLE = "Net Quantity Declaration";

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public ComplianceCheck evaluate(StructuredLabelData labelData) {
        String netQty = labelData.netQuantity();

        if (netQty == null || netQty.isBlank()) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.WARNING,
                    "Not detected in image",
                    "Net quantity declaration was not identified in the scanned photograph. Legal Metrology Rule 6(1)(c) mandates net quantity in terms of standard units of weight, measure, or number. Note: Non-detection in this image does not prove non-compliance on unphotographed panels.",
                    "Verify that net quantity in standard metric units (e.g. g, kg, ml, L) is clearly declared on the principal display panel.",
                    RuleSeverity.MEDIUM
            );
        }

        // Check if standard metric unit symbol is used
        boolean hasStandardUnit = netQty.matches("(?i).*(?:\\b|\\d)(?:g|kg|ml|l|ltr|gm|grams)\\b.*");

        if (hasStandardUnit) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.PASS,
                    netQty,
                    "Net quantity declaration using recognizable standard metric units detected on package. Note: Minimum statutory font height and principal display panel area compliance cannot be evaluated from a 2D photograph and requires physical measurement under Schedule II.",
                    "Confirm that the numeral and unit font size complies with Schedule II requirements corresponding to the package's principal display panel area.",
                    RuleSeverity.NONE
            );
        } else {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.WARNING,
                    netQty,
                    "Net quantity was detected (" + netQty + ") but may use non-standard unit symbols. Legal Metrology Rule 13 prescribes standard metric symbols (g, kg, ml, l, L).",
                    "Ensure standardized metric unit symbols are utilized in the declaration.",
                    RuleSeverity.LOW
            );
        }
    }
}
