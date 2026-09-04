package com.labelcheck.compliance.model;

/**
 * Applicability attributes for a scanned commodity package.
 * Determined prior to rule validation to prevent executing irrelevant statutory rules.
 */
public record ApplicabilityProfile(
        boolean retailPackage,
        boolean industrialConsumer,
        boolean institutionalConsumer,
        Boolean imported,
        boolean foodProduct,
        boolean multiPack,
        boolean combinationPack,
        MeasurementType measurementType,
        PackageGeometry geometry,
        Double packageGrossMassGrams,
        Double netQuantityNumeric,
        String netQuantityUnit,
        Double applicabilityConfidence,
        String classificationRationale
) {
    public enum MeasurementType {
        MASS,
        VOLUME,
        LENGTH,
        AREA,
        NUMBER,
        UNKNOWN
    }

    public enum PackageGeometry {
        RECTANGULAR,
        CYLINDRICAL,
        NEARLY_CYLINDRICAL,
        POUCH_FLEXIBLE,
        OTHER
    }

    public static ApplicabilityProfile defaultRetailFood() {
        return new ApplicabilityProfile(
                true, false, false, false, true, false, false,
                MeasurementType.MASS, PackageGeometry.RECTANGULAR,
                null, null, null, 0.90, "Standard retail packaged food commodity"
        );
    }
}
