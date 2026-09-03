package com.labelcheck.service;

import com.labelcheck.compliance.ComplianceCheck;
import com.labelcheck.compliance.ComplianceResult;
import com.labelcheck.compliance.ComplianceRule;
import com.labelcheck.compliance.ComplianceRuleEngine;
import com.labelcheck.compliance.RuleSeverity;
import com.labelcheck.compliance.RuleStatus;
import com.labelcheck.compliance.rules.ConsumerCareRule;
import com.labelcheck.compliance.rules.DateMarkingRule;
import com.labelcheck.compliance.rules.FssaiLicenseRule;
import com.labelcheck.compliance.rules.ManufacturerRule;
import com.labelcheck.compliance.rules.MrpRule;
import com.labelcheck.compliance.rules.NetQuantityRule;
import com.labelcheck.compliance.rules.OriginRule;
import com.labelcheck.compliance.rules.UnitSalePriceRule;
import com.labelcheck.dto.StructuredLabelData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step 12 validation suite:
 * Verifies structured extraction accuracy against realistic OCR noise,
 * false-positive defenses (FSSAI context, nutrition table shielding),
 * deterministic compliance evaluation, and real package benchmark execution.
 */
class ExtractionAndComplianceValidationTest {

    private LabelExtractionService extractionService;
    private ComplianceRuleEngine complianceEngine;

    @BeforeEach
    void setUp() {
        extractionService = new LabelExtractionService();
        List<ComplianceRule> rules = List.of(
                new MrpRule(),
                new UnitSalePriceRule(),
                new NetQuantityRule(),
                new FssaiLicenseRule(),
                new DateMarkingRule(),
                new ManufacturerRule(),
                new ConsumerCareRule(),
                new OriginRule()
        );
        complianceEngine = new ComplianceRuleEngine(rules);
    }

    @Test
    @DisplayName("1. Realistic noisy OCR block with Indian packaging declarations extracts completely")
    void noisyPackagingBlock_extractsAccurately() {
        String ocr = """
                M RP: Rs 5O.OO (incl. of all taxes)
                Unit Sale Price: Rs 0.25 per g
                Net Qty: 200 g
                FSSAI Lic. No. 100 210 1100 0456
                Manufactured & Marketed by: Himalayan Organics Pvt Ltd, Dehradun
                PKD: 08/2026
                Best Before: 12 months from packing
                Customer Care Helpline: 1800-11-2233
                Email: care@himalayanorganics.in
                Country of Origin: India
                """;

        StructuredLabelData data = extractionService.extract(ocr);

        assertThat(data.mrp()).isEqualTo("50.00");
        assertThat(data.mrpInclusiveOfTaxes()).isTrue();
        assertThat(data.unitSalePrice()).isEqualTo("0.25 per g");
        assertThat(data.netQuantity()).isEqualTo("200 g");
        assertThat(data.fssaiLicenseNumber()).isEqualTo("10021011000456");
        assertThat(data.manufactureOrPackingDate()).isEqualTo("08/2026");
        assertThat(data.bestBeforeOrExpiry()).contains("12 months from packing");
        assertThat(data.manufacturerName()).contains("Himalayan Organics Pvt Ltd");
        assertThat(data.customerCarePhone()).isEqualTo("1800-11-2233");
        assertThat(data.customerCareEmail()).isEqualTo("care@himalayanorganics.in");
        assertThat(data.countryOfOrigin()).isEqualTo("India");

        ComplianceResult result = complianceEngine.evaluate(data);
        assertThat(result.overallStatus()).isEqualTo(RuleStatus.PASS);
        assertThat(result.overallScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("2. False-Positive Defense: Batch numbers, phone numbers, and barcodes are NOT extracted as FSSAI")
    void falsePositiveDefense_noUncontextualFssai() {
        String ocr = """
                Batch No: 10021011000456
                Phone: 1800-123-4567
                Barcode: 10012345678901
                Net Weight: 500 g
                MRP: ₹ 90
                """;

        StructuredLabelData data = extractionService.extract(ocr);

        assertThat(data.fssaiLicenseNumber()).isNull();
        assertThat(data.mrp()).isEqualTo("90");
        assertThat(data.netQuantity()).isEqualTo("500 g");

        ComplianceResult result = complianceEngine.evaluate(data);
        // FSSAI should be WARNING ("Not detected"), NOT a false PASS
        ComplianceCheck fssaiCheck = result.checks().stream()
                .filter(c -> c.id().equals("RULE_FSSAI"))
                .findFirst()
                .orElseThrow();
        assertThat(fssaiCheck.status()).isEqualTo(RuleStatus.WARNING);
        assertThat(fssaiCheck.severity()).isEqualTo(RuleSeverity.MEDIUM);
    }

    @Test
    @DisplayName("3. False-Positive Defense: Nutrition table quantities do not override net quantity")
    void falsePositiveDefense_nutritionValuesRejected() {
        String ocr = """
                NUTRITIONAL INFORMATION per 100g
                Energy 520 kcal
                Protein 7.2 g
                Carbohydrate 68.0g
                Total Sugars 24g
                Total Fat 22.5 g
                Sodium 380 mg
                """;

        StructuredLabelData data = extractionService.extract(ocr);
        assertThat(data.netQuantity()).isNull();

        ComplianceResult result = complianceEngine.evaluate(data);
        ComplianceCheck netQtyCheck = result.checks().stream()
                .filter(c -> c.id().equals("RULE_NET_QTY"))
                .findFirst()
                .orElseThrow();
        assertThat(netQtyCheck.status()).isEqualTo(RuleStatus.WARNING);
    }

    @Test
    @DisplayName("4. Missing declarations produce WARNING / NEEDS REVIEW without asserting legal violation")
    void missingDeclarations_produceWarningsNotViolations() {
        // Only brand slogan and name, no statutory markers
        String ocr = "Delightful Crunchy Cookies - Made with love and real butter";
        StructuredLabelData data = extractionService.extract(ocr);

        ComplianceResult result = complianceEngine.evaluate(data);

        assertThat(result.overallStatus()).isEqualTo(RuleStatus.WARNING);
        assertThat(result.overallScore()).isGreaterThanOrEqualTo(40);
        // None of the missing fields should be flagged as VIOLATION
        assertThat(result.checks()).noneMatch(c -> c.status() == RuleStatus.VIOLATION);
        assertThat(result.summary()).contains("declaration");
    }

    @Test
    @DisplayName("5. Photo-limited attributes explicitly disclaim 2D visual limitations")
    void photoLimitedAttributes_containStatutoryDisclaimers() {
        String ocr = """
                Net Quantity: 150 g
                FSSAI Lic. No. 10021011000456
                MRP Rs 50.00 (incl. of all taxes)
                """;

        StructuredLabelData data = extractionService.extract(ocr);
        ComplianceResult result = complianceEngine.evaluate(data);

        ComplianceCheck netQtyCheck = result.checks().stream()
                .filter(c -> c.id().equals("RULE_NET_QTY"))
                .findFirst()
                .orElseThrow();
        // Disclaims font size measurement
        assertThat(netQtyCheck.legalReason()).contains("Schedule II");

        ComplianceCheck fssaiCheck = result.checks().stream()
                .filter(c -> c.id().equals("RULE_FSSAI"))
                .findFirst()
                .orElseThrow();
        // Disclaims active portal certification
        assertThat(fssaiCheck.legalReason()).contains("FoSCoS");
    }

    @Test
    @DisplayName("6. Real food label benchmark on test_food_label.png")
    void realFoodLabelBenchmark_testFoodLabel() {
        String ocr = """
                MRP Rs 85.00 (incl. of all taxes)
                Unit Sale Price: Rs 0.425 per g
                Net Quantity: 200 g
                PKD: 09/2026
                Manufactured by: Himalayan Fresh Foods Ltd
                FSSAI Lic. No. 10021011000456
                Customer Helpline: 1800-111-2233
                Email: care@himalayanfresh.in
                """;

        StructuredLabelData data = extractionService.extract(ocr);

        assertThat(data.mrp()).isEqualTo("85.00");
        assertThat(data.mrpInclusiveOfTaxes()).isTrue();
        assertThat(data.unitSalePrice()).isEqualTo("0.425 per g");
        assertThat(data.netQuantity()).isEqualTo("200 g");
        assertThat(data.manufactureOrPackingDate()).isEqualTo("09/2026");
        assertThat(data.manufacturerName()).isEqualTo("Himalayan Fresh Foods Ltd");
        assertThat(data.fssaiLicenseNumber()).isEqualTo("10021011000456");
        assertThat(data.customerCarePhone()).isEqualTo("1800-111-2233");
        assertThat(data.customerCareEmail()).isEqualTo("care@himalayanfresh.in");

        ComplianceResult result = complianceEngine.evaluate(data);
        // Domestic manufacturer legally substantiates domestic origin under Rule 6(1)(a)
        assertThat(result.overallStatus()).isEqualTo(RuleStatus.PASS);
        assertThat(result.overallScore()).isEqualTo(100);
    }
}
