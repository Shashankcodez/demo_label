package com.labelcheck.compliance.rules;

import com.labelcheck.compliance.ComplianceCheck;
import com.labelcheck.compliance.ComplianceRule;
import com.labelcheck.compliance.RuleSeverity;
import com.labelcheck.compliance.RuleStatus;
import com.labelcheck.compliance.model.ApplicabilityProfile;
import com.labelcheck.compliance.model.NormalizedLabel;
import com.labelcheck.dto.StructuredLabelData;
import org.springframework.stereotype.Component;

/**
 * Evaluates font height compliance under Legal Metrology (Packaged Commodities) Rules, 2011 -
 * Rule 7 & Schedule II (Minimum Height of Numerals and Letters).
 *
 * Statutory Principle:
 * A 2D photograph lacks an optical millimeter calibration target or physical package dimensions.
 * Therefore, automated screening CANNOT deterministically declare a statutory violation under
 * Schedule II. It must declare REQUIRES_MANUAL_VERIFICATION with the applicable millimeter thresholds.
 */
@Component
public class FontSizeRule implements ComplianceRule {

    private static final String RULE_ID = "RULE_FONT_SIZE";
    private static final String RULE_REF = "Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 7 & Schedule II";
    private static final String TITLE = "Minimum Font Height Requirements (Schedule II)";

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public ComplianceCheck evaluate(StructuredLabelData labelData) {
        if (labelData == null) {
            return uncalibratedCheck(null, null);
        }
        return uncalibratedCheck(labelData.netQuantity(), null);
    }

    @Override
    public ComplianceCheck evaluateNormalized(NormalizedLabel label, ApplicabilityProfile profile) {
        if (label == null) {
            return uncalibratedCheck(null, null);
        }
        return uncalibratedCheck(label.netQuantity(), label.netQuantityNumeric());
    }

    private ComplianceCheck uncalibratedCheck(String netQtyString, Double netQtyNumeric) {
        String thresholdInfo;
        if (netQtyNumeric != null) {
            if (netQtyNumeric <= 50.0) {
                thresholdInfo = "For declared quantity <= 50g/ml, Schedule II mandates minimum 1.0mm numeral/letter height.";
            } else if (netQtyNumeric <= 200.0) {
                thresholdInfo = "For declared quantity 50g–200g/ml, Schedule II mandates minimum 2.0mm numeral/letter height.";
            } else if (netQtyNumeric <= 1000.0) {
                thresholdInfo = "For declared quantity 200g–1kg/L, Schedule II mandates minimum 4.0mm numeral/letter height.";
            } else {
                thresholdInfo = "For declared quantity > 1kg/L, Schedule II mandates minimum 6.0mm numeral/letter height.";
            }
        } else {
            thresholdInfo = "Schedule II prescribes minimum numeral heights ranging from 1.0mm to 6.0mm depending on net quantity and Principal Display Panel area.";
        }

        return new ComplianceCheck(
                RULE_ID,
                RULE_REF,
                TITLE,
                RuleStatus.REQUIRES_MANUAL_VERIFICATION,
                netQtyString != null ? "Net Qty: " + netQtyString + " (Physical scale uncalibrated)" : "Uncalibrated visual capture",
                "Automated screening cannot verify statutory millimeter font heights from an uncalibrated 2D photograph without physical metric reference. " + thresholdInfo + " Per legal screening guidelines, automated vision tools must never fabricate statutory font infractions without physical verification.",
                "Inspect the physical commodity with a calibrated optical scale or metric rule to verify numeral height against Schedule II.",
                RuleSeverity.INFO
        );
    }
}
