package com.labelcheck.compliance.rules;

import com.labelcheck.compliance.ComplianceCheck;
import com.labelcheck.compliance.ComplianceRule;
import com.labelcheck.compliance.RuleSeverity;
import com.labelcheck.compliance.RuleStatus;
import com.labelcheck.dto.StructuredLabelData;
import org.springframework.stereotype.Component;

/**
 * Validates manufacturer, packer, or importer identification under
 * Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(a) and
 * FSSAI (Labelling and Display) Regulations, 2020 - Regulation 5(4).
 */
@Component
public class ManufacturerRule implements ComplianceRule {

    private static final String RULE_ID = "RULE_MANUFACTURER";
    private static final String RULE_REF = "Legal Metrology Rules, 2011 - Rule 6(1)(a) & FSSAI Reg. 5(4)";
    private static final String TITLE = "Manufacturer / Packer / Marketer Details";

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public ComplianceCheck evaluate(StructuredLabelData labelData) {
        String mfg = labelData.manufacturerName();
        String imp = labelData.importerName();

        if (mfg != null || imp != null) {
            String detected = mfg != null ? "Mfg/Packer: " + mfg : "Importer: " + imp;
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.PASS,
                    detected,
                    "Name and commercial identity of manufacturer, packer, or importer detected on the label in accordance with Rule 6(1)(a) and FSSAI Regulation 5(4).",
                    "Ensure the complete postal address, including premises, city, state, and valid PIN code, is legibly printed on the package.",
                    RuleSeverity.NONE
            );
        }

        return new ComplianceCheck(
                RULE_ID,
                RULE_REF,
                TITLE,
                RuleStatus.WARNING,
                "Not detected in image",
                "Name and address of manufacturer, packer, or importer were not detected in the scanned photograph. Mandatory under Legal Metrology Rule 6(1)(a) and FSSAI Regulation 5(4). Note: Non-detection in this image does not prove non-compliance across other packaging facets.",
                "Verify that the manufacturer, packer, or importer declaration is legibly printed with complete registered premises details.",
                RuleSeverity.MEDIUM
        );
    }
}
