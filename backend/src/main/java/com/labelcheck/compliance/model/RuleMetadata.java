package com.labelcheck.compliance.model;

/**
 * Versioned legal metadata describing a specific packaging statutory rule.
 */
public record RuleMetadata(
        String ruleId,
        RegulationFamily regulationFamily,
        String ruleNumber,
        String title,
        String description,
        String sourceAuthority,
        String sourceDocument,
        String legalReference,
        String effectiveFrom,
        String effectiveTo,
        String amendmentVersion,
        String applicabilityCondition
) {
    public static Builder builder(String ruleId) {
        return new Builder(ruleId);
    }

    public static class Builder {
        private final String ruleId;
        private RegulationFamily regulationFamily = RegulationFamily.LEGAL_METROLOGY;
        private String ruleNumber = "";
        private String title = "";
        private String description = "";
        private String sourceAuthority = "Department of Consumer Affairs, Government of India";
        private String sourceDocument = "Legal Metrology (Packaged Commodities) Rules, 2011";
        private String legalReference = "";
        private String effectiveFrom = "2011-04-01";
        private String effectiveTo = null;
        private String amendmentVersion = "2021-Amend";
        private String applicabilityCondition = "All pre-packaged commodities sold in retail";

        public Builder(String ruleId) {
            this.ruleId = ruleId;
        }

        public Builder family(RegulationFamily family) {
            this.regulationFamily = family;
            return this;
        }

        public Builder ruleNumber(String ruleNumber) {
            this.ruleNumber = ruleNumber;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder authority(String authority) {
            this.sourceAuthority = authority;
            return this;
        }

        public Builder document(String document) {
            this.sourceDocument = document;
            return this;
        }

        public Builder reference(String reference) {
            this.legalReference = reference;
            return this;
        }

        public Builder effectiveFrom(String from) {
            this.effectiveFrom = from;
            return this;
        }

        public Builder amendment(String amendment) {
            this.amendmentVersion = amendment;
            return this;
        }

        public Builder applicability(String applicability) {
            this.applicabilityCondition = applicability;
            return this;
        }

        public RuleMetadata build() {
            return new RuleMetadata(
                    ruleId, regulationFamily, ruleNumber, title, description,
                    sourceAuthority, sourceDocument, legalReference,
                    effectiveFrom, effectiveTo, amendmentVersion, applicabilityCondition
            );
        }
    }
}
