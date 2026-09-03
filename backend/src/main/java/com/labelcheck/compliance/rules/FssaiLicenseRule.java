package com.labelcheck.compliance.rules;

import com.labelcheck.compliance.ComplianceCheck;
import com.labelcheck.compliance.ComplianceRule;
import com.labelcheck.compliance.RuleSeverity;
import com.labelcheck.compliance.RuleStatus;
import com.labelcheck.dto.StructuredLabelData;
import org.springframework.stereotype.Component;

/**
 * Validates FSSAI 14-digit license/registration number format under
 * FSSAI Food Safety and Standards (Labelling and Display) Regulations, 2020 - Regulation 5(1).
 * Supports explicit recognition of "Applied For" status.
 */
@Component
public class FssaiLicenseRule implements ComplianceRule {

    private static final String RULE_ID = "RULE_FSSAI";
    private static final String RULE_REF = "FSSAI (Labelling and Display) Regulations, 2020 - Regulation 5(1)";
    private static final String TITLE = "FSSAI Food License / Registration Number";

    @Override
    public ComplianceCheck evaluate(StructuredLabelData labelData) {
        String fssaiStatus = labelData.fssaiStatus();
        String fssai = labelData.fssaiLicenseNumber();

        // 1. Explicit Applied For declaration
        if ("APPLIED_FOR".equalsIgnoreCase(fssaiStatus) ||
                (fssai != null && fssai.toLowerCase().contains("applied"))) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.WARNING,
                    "FSSAI License Status: Applied For",
                    "Label indicates 'Lic No. : Applied For'. Under FSSAI regulations, food business operators must possess a valid 14-digit license before commencing commercial food operations. Pre-packaged food displaying 'Applied For' requires physical review of official FoSCoS application documentation.",
                    "Verify official FoSCoS application reference or ensure active 14-digit registration before commercial distribution.",
                    RuleSeverity.MEDIUM
            );
        }

        // 2. Not detected in image
        if (fssai == null || fssai.isBlank()) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.WARNING,
                    "Not detected in image",
                    "A 14-digit FSSAI license or registration number was not detected in the scanned photograph. FSSAI Regulation 5(1) requires the FSSAI logo and license number to be displayed on all pre-packaged food labels. Note: Non-detection in a single image does not prove absence on other package panels.",
                    "Ensure the 14-digit FSSAI license number is clearly displayed on the package adjacent to the official FSSAI logo.",
                    RuleSeverity.MEDIUM
            );
        }

        // 3. Valid 14-digit format detected
        if (fssai.matches("^[0-9]{14}$")) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.PASS,
                    fssai,
                    "A 14-digit numeric sequence matching the statutory FSSAI license/registration number format was detected on the label as required by Regulation 5(1). Note: Format detection verifies numerical structure only; it does not confirm active license validity, authenticity, or licensee identity on the official FoSCoS portal.",
                    "Verify the operational status and licensee details of this 14-digit number on the official FSSAI FoSCoS portal (foscos.fssai.gov.in) before distribution.",
                    RuleSeverity.NONE
            );
        } else {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.WARNING,
                    fssai,
                    "An FSSAI license candidate was identified (" + fssai + ") but does not strictly match the statutory 14-digit numeric format (detected length: " + fssai.length() + " digits). Regulation 5(1) mandates an exact 14-digit license number.",
                    "Verify that the FSSAI license number is legibly printed with all 14 digits and free from extraneous punctuation or OCR noise.",
                    RuleSeverity.MEDIUM
            );
        }
    }
}
