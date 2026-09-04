package com.labelcheck.compliance.rules;

import com.labelcheck.compliance.ComplianceCheck;
import com.labelcheck.compliance.ComplianceRule;
import com.labelcheck.compliance.RuleSeverity;
import com.labelcheck.compliance.RuleStatus;
import com.labelcheck.dto.StructuredLabelData;
import org.springframework.stereotype.Component;

/**
 * Validates Unit Sale Price (USP) declaration in accordance with
 * Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(11).
 */
@Component
public class UnitSalePriceRule implements ComplianceRule {

    private static final String RULE_ID = "RULE_USP";
    private static final String RULE_REF = "Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(11)";
    private static final String TITLE = "Unit Sale Price (USP) Declaration";

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public ComplianceCheck evaluate(StructuredLabelData labelData) {
        String usp = labelData.unitSalePrice();

        if (usp != null && !usp.isBlank()) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.PASS,
                    usp,
                    "Unit Sale Price (USP) declaration detected on label in accordance with Rule 6(11).",
                    "Ensure the Unit Sale Price is rounded off to the nearest two decimal places and clearly displayed on the principal display panel.",
                    RuleSeverity.NONE
            );
        }

        return new ComplianceCheck(
                RULE_ID,
                RULE_REF,
                TITLE,
                RuleStatus.WARNING,
                "Not detected in image",
                "Unit Sale Price (USP) was not detected in the provided scan. Rule 6(11) mandates declaration of USP (e.g. 'Rs. ... per g / ml / kg / L') unless exempted (e.g. packages with net quantity of 10g/10ml or less, or where retail sale price equals unit sale price).",
                "Verify whether the product qualifies for a statutory exemption under Rule 6(11) or displays the USP on the principal display panel.",
                RuleSeverity.LOW
        );
    }
}
