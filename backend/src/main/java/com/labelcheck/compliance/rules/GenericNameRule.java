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
 * Validates common or generic name of the commodity contained in the package under
 * Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(b) & Rule 6(1)(l).
 * Mandates that every package declare the generic name of the commodity alongside any brand/trade name.
 */
@Component
public class GenericNameRule implements ComplianceRule {

    private static final String RULE_ID = "RULE_GENERIC_NAME";
    private static final String RULE_REF = "Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(b) & Rule 6(1)(l)";
    private static final String TITLE = "Common or Generic Name Declaration";

    @Override
    public String getRuleId() {
        return RULE_ID;
    }

    @Override
    public ComplianceCheck evaluate(StructuredLabelData labelData) {
        if (labelData == null) {
            return notDetectedCheck();
        }
        String product = labelData.productName();
        String brand = labelData.brand();
        return evaluateNames(product, brand);
    }

    @Override
    public ComplianceCheck evaluateNormalized(NormalizedLabel label, ApplicabilityProfile profile) {
        if (label == null) {
            return notDetectedCheck();
        }
        String generic = label.genericName();
        String product = label.productName();
        String brand = label.brand();

        if (generic != null && !generic.isBlank()) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.PASS,
                    generic + (product != null ? " (" + product + ")" : ""),
                    "Common or generic commodity name clearly declared on package as required by Rule 6(1)(b).",
                    "Ensure the generic name is prominently positioned on the principal display panel.",
                    RuleSeverity.NONE
            );
        }

        return evaluateNames(product, brand);
    }

    private ComplianceCheck evaluateNames(String product, String brand) {
        if (product != null && !product.isBlank()) {
            boolean hasGenericIndicator = product.matches("(?i).*(oil|biscuit|cookie|wafer|water|juice|snack|atta|flour|rice|dal|pulse|tea|coffee|masala|spice|noodle|pasta|milk|butter|ghee|paneer|curd|shampoo|soap|detergent|cleaner|cream|lotion|paste|sugar|salt|cereal|chips|chocolate|sauce|jam|drink|beverage|confectionery|confectionary).*");

            if (hasGenericIndicator || (brand != null && !product.equalsIgnoreCase(brand.trim()))) {
                return new ComplianceCheck(
                        RULE_ID,
                        RULE_REF,
                        TITLE,
                        RuleStatus.PASS,
                        product,
                        "Generic or descriptive commodity identification detected in product title in accordance with Rule 6(1)(b).",
                        "Ensure the generic name is distinct from trade marks and prominently displayed.",
                        RuleSeverity.NONE
                );
            }

            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.WARNING,
                    product,
                    "Product title detected, but appears to be a trade name or brand without explicit generic commodity description. Rule 6(1)(b) mandates the common or generic name.",
                    "Verify on the physical package whether the common commodity name (e.g., 'Refined Sunflower Oil', 'Biscuits') is printed alongside the trade name.",
                    RuleSeverity.LOW
            );
        }

        return notDetectedCheck();
    }

    private ComplianceCheck notDetectedCheck() {
        return new ComplianceCheck(
                RULE_ID,
                RULE_REF,
                TITLE,
                RuleStatus.WARNING,
                "Not detected in image",
                "Common or generic name of commodity was not identified in the scanned photograph. Rule 6(1)(b) mandates clear commodity identification.",
                "Ensure the common or generic commodity name is prominently declared on the principal display panel.",
                RuleSeverity.MEDIUM
        );
    }
}
