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
 * Validates statutory applicability and exemptions under
 * Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 26.
 * Identifies whether a commodity qualifies for statutory exemptions (e.g. net quantity <= 10g/ml,
 * industrial/institutional consumer packaging, bulk packages > 25kg/25L).
 */
@Component
public class ExemptionRule implements ComplianceRule {

    private static final String RULE_ID = "RULE_EXEMPTION";
    private static final String RULE_REF = "Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 26";
    private static final String TITLE = "Statutory Scope & Exemption Verification";

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public ComplianceCheck evaluate(StructuredLabelData labelData) {
        if (labelData == null) {
            return standardRetailCheck();
        }
        String netQty = labelData.netQuantity();
        String rawOcr = labelData.rawOcrText();
        return evaluateScope(netQty, rawOcr);
    }

    @Override
    public ComplianceCheck evaluateNormalized(NormalizedLabel label, ApplicabilityProfile profile) {
        if (profile != null && (profile.industrialConsumer() || profile.institutionalConsumer())) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.PASS,
                    "Institutional / Industrial Consumer Package",
                    "Package qualifies for statutory exemption under Rule 26(f) (packages intended for industrial or institutional consumers directly). Standard retail declarations are exempt, provided package bears 'Not for Retail Sale' or institutional marking.",
                    "Verify the presence of 'Not for retail sale' markings on distribution packaging.",
                    RuleSeverity.NONE
            );
        }

        if (label != null && label.netQuantityNumeric() != null && label.netQuantityNumeric() <= 10.0) {
            String unit = label.netQuantityUnit();
            if (unit != null && (unit.equalsIgnoreCase("g") || unit.equalsIgnoreCase("ml") || unit.equalsIgnoreCase("gm"))) {
                return new ComplianceCheck(
                        RULE_ID,
                        RULE_REF,
                        TITLE,
                        RuleStatus.PASS,
                        "Small Package (<= 10g/ml): " + label.netQuantity(),
                        "Package qualifies for small-package exemption under Rule 26(a). Under Rule 26(a) proviso, only retail sale price (MRP) and date of expiry/use-by are mandatory; full detailed declarations are exempt.",
                        "Ensure MRP and expiry/best-before dates remain legible despite small package footprint.",
                        RuleSeverity.NONE
                );
            }
        }

        return evaluateScope(label != null ? label.netQuantity() : null, label != null ? label.rawOcrText() : null);
    }

    private ComplianceCheck evaluateScope(String netQty, String rawOcr) {
        if (rawOcr != null && rawOcr.matches("(?i).*(not\\s+for\\s+retail\\s+sale|institutional\\s+consumer|industrial\\s+use).*")) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.PASS,
                    "Institutional Marking Detected",
                    "Package contains institutional/industrial markings qualifying under Rule 26(f) or Chapter II Scope exclusions.",
                    "Ensure institutional supply chain chain-of-custody documentation is maintained.",
                    RuleSeverity.NONE
            );
        }

        return standardRetailCheck();
    }

    private ComplianceCheck standardRetailCheck() {
        return new ComplianceCheck(
                RULE_ID,
                RULE_REF,
                TITLE,
                RuleStatus.PASS,
                "Standard Retail Pre-packaged Commodity",
                "Commodity is subject to standard retail pre-packaged commodity rules under Chapter II of Legal Metrology Rules, 2011. No Rule 26 statutory exemptions apply.",
                "Maintain complete compliance across all mandatory retail declarations.",
                RuleSeverity.NONE
        );
    }
}
