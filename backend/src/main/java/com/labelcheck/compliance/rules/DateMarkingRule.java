package com.labelcheck.compliance.rules;

import com.labelcheck.compliance.ComplianceCheck;
import com.labelcheck.compliance.ComplianceRule;
import com.labelcheck.compliance.RuleSeverity;
import com.labelcheck.compliance.RuleStatus;
import com.labelcheck.dto.StructuredLabelData;
import org.springframework.stereotype.Component;

/**
 * Validates date markings (Date of Manufacture/Packing and Expiry/Best Before) under
 * Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(d) and
 * FSSAI (Labelling and Display) Regulations, 2020 - Regulation 5(6).
 */
@Component
public class DateMarkingRule implements ComplianceRule {

    private static final String RULE_ID = "RULE_DATE_MARKING";
    private static final String RULE_REF = "Legal Metrology Rules, 2011 - Rule 6(1)(d) & FSSAI Reg. 5(6)";
    private static final String TITLE = "Date of Manufacture, Packing, or Expiry";

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public ComplianceCheck evaluate(StructuredLabelData labelData) {
        String mfd = labelData.manufactureOrPackingDate();
        String exp = labelData.bestBeforeOrExpiry();

        if (mfd != null && exp != null) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.PASS,
                    "MFD/PKD: " + mfd + " | Expiry: " + exp,
                    "Both Date of Manufacture/Packing and Expiry/Best Before declarations detected in accordance with Legal Metrology Rule 6(1)(d) and FSSAI Regulation 5(6). Note: Shelf-life validity is not dynamically computed from OCR scans alone.",
                    "Ensure day, month, and year formatting complies with statutory DD/MM/YY or MM/YY standards.",
                    RuleSeverity.NONE
            );
        } else if (mfd != null) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.PASS,
                    "MFD/PKD: " + mfd,
                    "Date of manufacture/packing detected (satisfying Legal Metrology Rule 6(1)(d)). For food products, an additional Expiry or Best Before date is required under FSSAI Regulation 5(6) if not already declared on another panel.",
                    "Verify whether an Expiry or Best Before date is declared on the crimp, seal, or secondary display panel.",
                    RuleSeverity.NONE
            );
        } else if (exp != null) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.PASS,
                    "Expiry/Best Before: " + exp,
                    "Expiry or Best Before declaration detected under FSSAI Regulation 5(6). Date of manufacture/packing was not identified in this scan (required under Legal Metrology Rule 6(1)(d)).",
                    "Ensure date of manufacture or packing is stamped legibly on the package.",
                    RuleSeverity.NONE
            );
        }

        return new ComplianceCheck(
                RULE_ID,
                RULE_REF,
                TITLE,
                RuleStatus.WARNING,
                "Not detected in image",
                "Date of manufacture (MFD), packing (PKD), or Expiry/Best Before date was not identified in the scanned photograph. Rule 6(1)(d) and FSSAI Reg. 5(6) mandate date marking declarations.",
                "Check the crimp seals, top lid, or bottom stamp area where date markings are commonly embossed or ink-jet printed.",
                RuleSeverity.MEDIUM
        );
    }
}
