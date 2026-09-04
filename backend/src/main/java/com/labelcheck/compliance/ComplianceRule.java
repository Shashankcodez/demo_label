package com.labelcheck.compliance;

import com.labelcheck.compliance.model.ApplicabilityProfile;
import com.labelcheck.compliance.model.NormalizedLabel;
import com.labelcheck.compliance.model.RuleMetadata;
import com.labelcheck.compliance.rules.LegalMetrologyRuleCatalog;
import com.labelcheck.dto.StructuredLabelData;

/**
 * Interface contract for individual statutory packaging compliance rules.
 */
public interface ComplianceRule {

    /**
     * Evaluates the extracted structured label data against the specific statutory mandate.
     *
     * @param labelData the structured label information extracted from OCR
     * @return ComplianceCheck containing status, statutory rationale, and recommendations
     */
    ComplianceCheck evaluate(StructuredLabelData labelData);

    /**
     * Evaluates the normalized label data against the statutory mandate with applicability context.
     * Default implementation adapts to the legacy evaluate method.
     */
    default ComplianceCheck evaluateNormalized(NormalizedLabel label, ApplicabilityProfile profile) {
        if (label == null) {
            return evaluate(null);
        }
        return evaluate(toStructuredData(label));
    }

    /**
     * Returns the statutory rule metadata associated with this rule.
     */
    default RuleMetadata getMetadata() {
        return LegalMetrologyRuleCatalog.get(getRuleId());
    }

    /**
     * Unique identifier for this rule (e.g. "RULE_MRP").
     */
    default String getRuleId() {
        return getClass().getSimpleName();
    }

    private static StructuredLabelData toStructuredData(NormalizedLabel label) {
        if (label == null) return null;
        String mfg = label.manufacturerName();
        if (mfg != null && label.manufacturerAddress() != null && !label.manufacturerAddress().isBlank()) {
            mfg = mfg + ", " + label.manufacturerAddress();
        }
        String imp = label.importerName();
        if (imp != null && label.importerAddress() != null && !label.importerAddress().isBlank()) {
            imp = imp + ", " + label.importerAddress();
        }
        return new StructuredLabelData(
                label.productName(),
                label.brand(),
                label.netQuantity(),
                label.mrp(),
                label.mrpInclusiveOfTaxes(),
                label.unitSalePrice(),
                mfg,
                null,
                imp,
                null,
                label.countryOfOrigin(),
                label.manufactureOrPackingDate(),
                label.bestBeforeOrExpiry(),
                label.fssaiLicenseNumber(),
                label.customerCarePhone(),
                label.customerCareEmail(),
                label.customerCareAddress(),
                label.rawOcrText()
        );
    }
}

