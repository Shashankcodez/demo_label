package com.labelcheck.compliance.rules;

import com.labelcheck.compliance.ComplianceCheck;
import com.labelcheck.compliance.ComplianceRule;
import com.labelcheck.compliance.RuleSeverity;
import com.labelcheck.compliance.RuleStatus;
import com.labelcheck.dto.StructuredLabelData;
import org.springframework.stereotype.Component;

/**
 * Validates consumer grievance redressal contact information under
 * Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(f).
 */
@Component
public class ConsumerCareRule implements ComplianceRule {

    private static final String RULE_ID = "RULE_CONSUMER_CARE";
    private static final String RULE_REF = "Legal Metrology (Packaged Commodities) Rules, 2011 - Rule 6(1)(f)";
    private static final String TITLE = "Consumer Care / Grievance Redressal Mechanism";

    @Override
    public ComplianceCheck evaluate(StructuredLabelData labelData) {
        String phone = labelData.customerCarePhone();
        String email = labelData.customerCareEmail();
        String address = labelData.customerCareAddress();

        boolean hasPhone = phone != null && !phone.isBlank();
        boolean hasEmail = email != null && !email.isBlank();

        if (hasPhone && hasEmail) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.PASS,
                    "Phone: " + phone + " | Email: " + email,
                    "Comprehensive consumer grievance redressal contacts (both telephone number and email address) detected in accordance with Rule 6(1)(f).",
                    "Ensure consumer helpline phone numbers and email support channels remain continuously operational.",
                    RuleSeverity.NONE
            );
        } else if (hasPhone) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.WARNING,
                    "Phone: " + phone + " (Email not identified)",
                    "Consumer helpline telephone number detected (" + phone + "), but email address was not identified in this scan. Note that Rule 6(1)(f) prescribes the name, address, telephone number, and e-mail address of the person or office that can be contacted.",
                    "Verify whether a consumer support email address is printed on another panel of the physical package.",
                    RuleSeverity.LOW
            );
        } else if (hasEmail) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.WARNING,
                    "Email: " + email + " (Telephone not identified)",
                    "Consumer support email detected (" + email + "), but telephone helpline was not identified in this scan. Rule 6(1)(f) prescribes both telephone number and email address.",
                    "Verify whether a telephone helpline number is printed on the physical packaging.",
                    RuleSeverity.LOW
            );
        } else if (address != null && !address.isBlank()) {
            return new ComplianceCheck(
                    RULE_ID,
                    RULE_REF,
                    TITLE,
                    RuleStatus.WARNING,
                    "Address: " + address,
                    "Consumer care postal address was detected, but direct telephone and email contacts were not identified in this scan as required by Rule 6(1)(f).",
                    "Ensure direct telephone number and email address are prominently declared alongside the grievance address.",
                    RuleSeverity.LOW
            );
        }

        return new ComplianceCheck(
                RULE_ID,
                RULE_REF,
                TITLE,
                RuleStatus.WARNING,
                "Not detected in image",
                "Consumer care telephone number or email address was not identified in the scanned photograph. Mandatory under Legal Metrology Rule 6(1)(f).",
                "Ensure consumer helpline phone number, email address, or grievance cell details are declared conspicuously on the package.",
                RuleSeverity.LOW
        );
    }
}
