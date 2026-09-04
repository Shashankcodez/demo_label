package com.labelcheck.compliance.rules;

import com.labelcheck.compliance.ComplianceCheck;
import com.labelcheck.compliance.ComplianceRule;
import com.labelcheck.compliance.RuleSeverity;
import com.labelcheck.compliance.RuleStatus;
import com.labelcheck.dto.StructuredLabelData;
import org.springframework.stereotype.Component;

/**
 * Validates Maximum Retail Price (MRP) declaration in accordance with
 * Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(e).
 */
@Component
public class MrpRule implements ComplianceRule {

    private static final String RULE_ID = "RULE_MRP";
    private static final String RULE_REF = "Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(e)";
    private static final String TITLE = "Maximum Retail Price (MRP) Declaration";

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public ComplianceCheck evaluate(StructuredLabelData labelData) {
        String mrp = labelData.mrp();

        if (mrp == null || mrp.isBlank()) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.WARNING,
                    "Not detected in image",
                    "Maximum Retail Price (MRP) declaration was not identified in the scanned photograph. Rule 6(1)(e) mandates that every pre-packaged retail commodity declare the retail sale price in numerals with rupee symbol or abbreviation 'Rs.'. Note: Non-detection in a single photograph does not constitute legal proof of absence on other package panels.",
                    "Inspect the physical commodity or capture a clearer image of the price panel to confirm the statutory MRP declaration.",
                    RuleSeverity.MEDIUM
            );
        }

        try {
            double value = Double.parseDouble(mrp);
            if (value <= 0) {
                return new ComplianceCheck(
                        RULE_ID,
                        RULE_REF,
                        TITLE,
                        RuleStatus.VIOLATION,
                        "₹" + mrp,
                        "Declared Maximum Retail Price is zero or negative, which constitutes a clear statutory violation for commercial retail packaging under Rule 6(1)(e).",
                        "Correct the packaging price stamp to declare the legitimate retail sale price.",
                        RuleSeverity.HIGH
                );
            }

            boolean hasTaxStatement = Boolean.TRUE.equals(labelData.mrpInclusiveOfTaxes());

            if (hasTaxStatement) {
                return new ComplianceCheck(
                        RULE_ID,
                        RULE_REF,
                        TITLE,
                        RuleStatus.PASS,
                        "₹" + mrp + " (incl. of all taxes)",
                        "Maximum Retail Price (MRP) numerical declaration detected with the mandatory 'inclusive of all taxes' indication under Rule 6(1)(e).",
                        "Ensure the price declaration remains unambiguous and unaltered across distribution channels.",
                        RuleSeverity.NONE
                );
            } else {
                return new ComplianceCheck(
                        RULE_ID,
                        RULE_REF,
                        TITLE,
                        RuleStatus.WARNING,
                        "₹" + mrp + " ('incl. of taxes' not identified)",
                        "Numerical Maximum Retail Price was detected (₹" + mrp + "), but the explicit statutory phrase 'inclusive of all taxes' was not identified in this scan. Rule 6(1)(e) requires the price to indicate that it is inclusive of all taxes.",
                        "Check the physical package to ensure 'incl. of all taxes' or 'inclusive of all taxes' is legibly printed adjacent to the price numerals.",
                        RuleSeverity.LOW
                );
            }
        } catch (NumberFormatException e) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.WARNING,
                    mrp,
                    "MRP price declaration was detected but numerals appear ambiguous or distorted by OCR.",
                    "Verify the price formatting on the physical package.",
                    RuleSeverity.LOW
            );
        }
    }
}
