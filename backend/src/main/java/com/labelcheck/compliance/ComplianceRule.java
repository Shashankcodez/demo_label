package com.labelcheck.compliance;

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
}
