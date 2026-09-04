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
 * Evaluates general legibility, prominence, and display standards under
 * Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 9.
 *
 * Statutory Principle:
 * Rule 9 requires declarations to be conspicuous, legible, and distinct from background.
 * It does NOT prescribe WCAG digital web accessibility contrast ratios as statutory packaging rules.
 * Low contrast or optical blur generates WARNING/REQUIRES_MANUAL_VERIFICATION, never a statutory FAIL.
 */
@Component
public class LegibilityRule implements ComplianceRule {

    private static final String RULE_ID = "RULE_LEGIBILITY";
    private static final String RULE_REF = "Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 9";
    private static final String TITLE = "General Legibility and Conspicuous Display";

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public ComplianceCheck evaluate(StructuredLabelData labelData) {
        if (labelData == null) {
            return incompleteCheck("No scan data available");
        }
        int detectedFields = labelData.countDetectedFields();
        return evaluateQuality(detectedFields, labelData.rawOcrText());
    }

    @Override
    public ComplianceCheck evaluateNormalized(NormalizedLabel label, ApplicabilityProfile profile) {
        if (label == null) {
            return incompleteCheck("No scan data available");
        }
        int detected = (label.productName() != null ? 1 : 0)
                + (label.netQuantity() != null ? 1 : 0)
                + (label.mrp() != null ? 1 : 0)
                + (label.manufacturerName() != null ? 1 : 0);
        return evaluateQuality(detected, label.rawOcrText());
    }

    private ComplianceCheck evaluateQuality(int detectedCoreFields, String rawOcr) {
        if (detectedCoreFields >= 3 && rawOcr != null && rawOcr.length() > 50) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.PASS,
                    "Legible text detected (" + detectedCoreFields + " core fields recognized)",
                    "Statutory declarations on scanned panel are legible and prominently identifiable in accordance with Rule 9. No superimposition or illegible distortion identified.",
                    "Ensure background contrast remains high across all print production batches.",
                    RuleSeverity.NONE
            );
        } else if (detectedCoreFields >= 1) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.WARNING,
                    "Partial legibility detected",
                    "Some label text was recognized, but optical clarity or lighting on the packaging panel appears limited. Rule 9 mandates conspicuous, unambiguous print quality.",
                    "Ensure high contrast between typography and background packaging material; avoid printing over patterned imagery.",
                    RuleSeverity.LOW
            );
        } else {
            return incompleteCheck("Image blur or extreme glare prevented optical character recognition");
        }
    }

    private ComplianceCheck incompleteCheck(String reason) {
        return new ComplianceCheck(
                RULE_ID,
                RULE_REF,
                TITLE,
                RuleStatus.REQUIRES_MANUAL_VERIFICATION,
                "Insufficient image quality",
                "Text legibility could not be verified automatically due to optical blur, low resolution, or adverse lighting conditions. Note: Image capture quality issues do not establish legal non-compliance of the physical package.",
                "Capture a well-lit, orthogonal photograph of the label panel or examine the physical package directly.",
                RuleSeverity.INFO
        );
    }
}
