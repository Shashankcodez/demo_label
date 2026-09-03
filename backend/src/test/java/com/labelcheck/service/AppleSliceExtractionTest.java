package com.labelcheck.service;

import com.labelcheck.dto.StructuredLabelData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class AppleSliceExtractionTest {

    private final String SAMPLE_RAW_OCR = """
            APPLE SLICE ;
            Ready to Cook "

            Coocking is made easier now as Cut fruits & vegetable is now
            available online @ best prices. It will save your time &leave
            less waste at the retail and kitchen levels.

            95 Kcal 10289
            0.3G
            25 ig Net Weight : 250Gm

            4.4 Gms. Batch : 20250509
            19 Gms.

            0.5 Gms. Packed On — : 09/05/2025

            Full C ;
            reais Best Before : 14/05/2025

            i Mineral 
            Other Nutrients} Other Vit SSA Lic No. : Applied For :

            Packed By :

            D MARKET MRP : ~150,

            G/7 Pathak Villa Building, Gopchar Pada, (Including all Taxes)
            Veer Savarkar Marg, Near S.R.T School,
            Virar East, Virar — 401305. Marketed By :
            District: Palghar. Maharashtra. India SMART ONLINE STORE
            Reach Us - Ph : +91 8888 720 520 A Branded eStore
            Email : admin@smartonlinestore.co.in
            """;

    @Test
    @DisplayName("Comprehensive Apple Slice Real-Label Regression Assertions")
    void testSampleExtractionRegression() {
        LabelExtractionService extractor = new LabelExtractionService();
        StructuredLabelData data = extractor.extract(SAMPLE_RAW_OCR);

        assertNotNull(data);

        // 1. MRP: MUST be 150, MUST NOT be 2150
        assertThat(data.mrp()).isEqualTo("150");
        assertThat(data.mrp()).isNotEqualTo("2150");
        assertThat(data.mrpInclusiveOfTaxes()).isTrue();

        // 2. Dates: Packed On MUST be 09/05/2025, MUST NOT be Protein 0.5 Gms
        assertThat(data.manufactureOrPackingDate()).isEqualTo("09/05/2025");
        assertThat(data.manufactureOrPackingDate()).doesNotContain("Protein");
        assertThat(data.manufactureOrPackingDate()).doesNotContain("0.5 Gms");
        assertThat(data.manufactureOrPackingDate()).isNotEqualTo("20250509");

        // 3. Best Before MUST be 14/05/2025
        assertThat(data.bestBeforeOrExpiry()).isEqualTo("14/05/2025");

        // 4. FSSAI: status MUST be APPLIED_FOR
        assertThat(data.fssaiStatus()).isEqualTo("APPLIED_FOR");

        // 5. Phone: MUST detect +91 8888 720 520
        assertThat(data.customerCarePhone()).isEqualTo("+91 8888 720 520");

        // 6. Email: MUST detect admin@smartonlinestore.co.in
        assertThat(data.customerCareEmail()).isEqualTo("admin@smartonlinestore.co.in");

        // 7. Product Name: MUST be Apple Slice
        assertThat(data.productName()).isEqualTo("Apple Slice");

        // 8. Manufacturer: MUST be D MARKET
        assertThat(data.manufacturerName()).isEqualTo("D MARKET");

        // 9. Batch Number: MUST be 20250509
        assertThat(data.batchNumber()).isEqualTo("20250509");

        // 10. Net Quantity: MUST contain 250
        assertThat(data.netQuantity()).contains("250");
    }

    @Test
    @DisplayName("Specific Unit Tests for Requirements from Section 14")
    void testSection14SpecificRequirements() {
        LabelExtractionService extractor = new LabelExtractionService();

        // MRP test: MRP: ₹150/- (Including all Taxes) => mrp = 150
        StructuredLabelData d1 = extractor.extract("MRP: ₹150/- (Including all Taxes)");
        assertThat(d1.mrp()).isEqualTo("150");
        assertThat(d1.mrpInclusiveOfTaxes()).isTrue();

        // FSSAI test: Lic No. : Applied For => APPLIED_FOR
        StructuredLabelData d2 = extractor.extract("Lic No. : Applied For");
        assertThat(d2.fssaiStatus()).isEqualTo("APPLIED_FOR");

        // Packed date test: Packed On: 09/05/2025 => 09/05/2025
        StructuredLabelData d3 = extractor.extract("Packed On: 09/05/2025");
        assertThat(d3.manufactureOrPackingDate()).isEqualTo("09/05/2025");

        // Best before test: Best Before: 14/05/2025 => 14/05/2025
        StructuredLabelData d4 = extractor.extract("Best Before: 14/05/2025");
        assertThat(d4.bestBeforeOrExpiry()).isEqualTo("14/05/2025");

        // Nutrition rejection test: Protein 0.5 Gms => NOT a date
        StructuredLabelData d5 = extractor.extract("Protein 0.5 Gms");
        assertThat(d5.manufactureOrPackingDate()).isNull();
        assertThat(d5.bestBeforeOrExpiry()).isNull();

        // Phone test: +91 8888 720 520 => phone detected
        StructuredLabelData d6 = extractor.extract("Reach Us: +91 8888 720 520");
        assertThat(d6.customerCarePhone()).isEqualTo("+91 8888 720 520");

        // Batch test: Batch: 20250509 => batchNumber = 20250509, packedDate != 20250509
        StructuredLabelData d7 = extractor.extract("Batch: 20250509");
        assertThat(d7.batchNumber()).isEqualTo("20250509");
        assertThat(d7.manufactureOrPackingDate()).isNull();
    }

    @Test
    @DisplayName("FSSAI License Rule evaluates APPLIED_FOR as WARNING with FoSCoS guidance")
    void testFssaiAppliedForRule() {
        com.labelcheck.compliance.rules.FssaiLicenseRule rule = new com.labelcheck.compliance.rules.FssaiLicenseRule();
        StructuredLabelData data = new StructuredLabelData(
                "Apple Slice", "Brand", "250 Gm", "150", true, null,
                "D MARKET", "Virar", null, null, "Domestic",
                "09/05/2025", "14/05/2025", null, "+91 8888 720 520",
                "admin@smartonlinestore.co.in", null, "", "20250509", "APPLIED_FOR"
        );
        com.labelcheck.compliance.ComplianceCheck check = rule.evaluate(data);
        assertThat(check.status()).isEqualTo(com.labelcheck.compliance.RuleStatus.WARNING);
        assertThat(check.detected()).contains("Applied For");
        assertThat(check.legalReason()).contains("Applied For");
        assertThat(check.recommendation()).contains("FoSCoS");
    }
}

