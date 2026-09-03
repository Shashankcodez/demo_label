package com.labelcheck.compliance.rules;

import com.labelcheck.compliance.ComplianceCheck;
import com.labelcheck.compliance.ComplianceRule;
import com.labelcheck.compliance.RuleSeverity;
import com.labelcheck.compliance.RuleStatus;
import com.labelcheck.dto.StructuredLabelData;
import org.springframework.stereotype.Component;

/**
 * Validates country of origin declaration under
 * Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(10) and
 * FSSAI (Labelling and Display) Regulations, 2020 - Regulation 5(5).
 */
@Component
public class OriginRule implements ComplianceRule {

    private static final String RULE_ID = "RULE_ORIGIN";
    private static final String RULE_REF = "Legal Metrology Rules, 2011 - Rule 6(10) & FSSAI Reg. 5(5)";
    private static final String TITLE = "Country of Origin Declaration";

    @Override
    public ComplianceCheck evaluate(StructuredLabelData labelData) {
        String origin = labelData.countryOfOrigin();
        String importer = labelData.importerName();
        String manufacturer = labelData.manufacturerName();

        if (origin != null && !origin.isBlank()) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.PASS,
                    origin,
                    "Country of origin declaration clearly identified on the commodity label in accordance with Rule 6(10) and FSSAI Regulation 5(5).",
                    "Ensure country of origin text is prominent and conspicuous.",
                    RuleSeverity.NONE
            );
        }

        // Mandatory for imported goods where importer details are detected
        if (importer != null && !importer.isBlank()) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.WARNING,
                    "Not detected on imported commodity",
                    "An importer declaration was identified, but explicit Country of Origin was not detected in this photograph. Country of Origin declaration is strictly mandatory for all imported commodities under Legal Metrology Rule 6(10) and FSSAI Regulation 5(5).",
                    "Add or verify the presence of an explicit 'Country of Origin: [Country]' or 'Made in [Country]' statement on the principal label.",
                    RuleSeverity.HIGH
            );
        }

        // For domestic products: manufacturer address legally establishes domestic origin
        if (manufacturer != null && !manufacturer.isBlank()) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.PASS,
                    "Domestic (Substantiated by Manufacturer)",
                    "For domestic commodities, country of origin is legally established by the registered domestic manufacturer address under Rule 6(1)(a). Explicit 'Made in India' wording is recommended for consumer clarity but is not a standalone packaging infraction under Rule 6(10).",
                    "Consider printing explicit 'Made in India' or 'Country of Origin: India' for consumer transparency.",
                    RuleSeverity.NONE
            );
        }

        return new ComplianceCheck(
                RULE_ID,
                RULE_REF,
                TITLE,
                RuleStatus.WARNING,
                "Not detected in image",
                "Neither Country of Origin nor domestic manufacturer details were identified in the scanned photograph. If the commodity is imported, explicit declaration of country of origin is mandatory under Rule 6(10) and FSSAI Regulation 5(5).",
                "Ensure country of origin or complete domestic manufacturer premises are declared on the package.",
                RuleSeverity.LOW
        );
    }
}
